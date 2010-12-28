package com.podio.sample.twitter;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Tweet;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;

import com.podio.APIFactory;
import com.podio.ResourceFactory;
import com.podio.comment.Comment;
import com.podio.comment.CommentAPI;
import com.podio.comment.CommentCreate;
import com.podio.common.Reference;
import com.podio.common.ReferenceType;
import com.podio.file.FileAPI;
import com.podio.item.FieldValuesUpdate;
import com.podio.item.ItemCreate;
import com.podio.item.ItemsResponse;
import com.podio.oauth.OAuthClientCredentials;
import com.podio.oauth.OAuthUsernameCredentials;

/**
 * Imports tweets
 * 
 */
public class Importer {

	/**
	 * The query to use for Twitter
	 */
	private static final String QUERY = "@podio OR #podio";

	/**
	 * The mapping to the fields in the app
	 */
	private static final int APP_ID = 29350;
	private static final int TEXT = 172265;
	private static final int TWEET = 172266;
	private static final int FROM = 172267;
	private static final int AVATAR = 172268;
	private static final int LOCATION = 172269;
	private static final int SOURCE = 172270;
	private static final int LINK = 172271;
	private static final int REPLY_TO = 180297;

	/**
	 * The interface to Twitter
	 */
	private final Twitter twitter;

	/**
	 * The interface to Podio
	 */
	private final APIFactory apiFactory;

	/**
	 * Creates a new importer with configuration read from the given file.
	 * 
	 * @param configFile
	 *            The configuration file to use. For an example file see the
	 *            example.config.properties in the root folder.
	 * @throws IOException
	 *             If there was an error reading from the configuration file
	 */
	public Importer(String configFile) throws IOException {
		super();

		Properties properties = new Properties();
		properties.load(new FileInputStream(configFile));

		this.twitter = new TwitterFactory().getOAuthAuthorizedInstance(
				properties.getProperty("twitter.token"),
				properties.getProperty("twitter.secret"));
		ResourceFactory resourceFactory = new ResourceFactory("api.podio.com",
				"upload.podio.com", 443, true, false,
				new OAuthClientCredentials(properties
						.getProperty("podio.client.mail"), properties
						.getProperty("podio.client.secret")),
				new OAuthUsernameCredentials(properties
						.getProperty("podio.user.mail"), properties
						.getProperty("podio.user.password")));
		this.apiFactory = new APIFactory(resourceFactory);
	}

	/**
	 * Reads from Twitter and posts to the Twitter app
	 * 
	 * @throws TwitterException
	 *             If any error occurs during communication with Twitter
	 */
	public void process() throws TwitterException {
		QueryResult result = twitter.search(new Query(QUERY));

		List<Tweet> tweets = result.getTweets();
		Collections.reverse(tweets);
		for (Tweet tweet : tweets) {
			publish(tweet);
		}
	}

	/**
	 * Returns the full text for for a Tweet
	 * 
	 * @param tweet
	 *            The tweet to get the full text for
	 * @param status
	 *            The status of the tweet
	 * @return The full text of the tweet in HTML code
	 */
	private String getFullText(Tweet tweet, Status status) {
		String[] hashtags = status.getHashtags();
		User[] users = status.getUserMentions();

		String text = tweet.getText();
		for (String hashtag : hashtags) {
			try {
				text = text.replace(
						("#" + hashtag),
						"<a href=\"http://twitter.com/#search?q="
								+ URLEncoder.encode(hashtag, "UTF-8") + "\">#"
								+ hashtag + "</a>");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}

		for (User user : users) {
			text = text.replace(("@" + user.getScreenName()),
					"<a href=\"http://twitter.com/" + user.getScreenName()
							+ "\">@" + user.getScreenName() + "</a>");
		}

		return text;
	}

	/**
	 * Publishes the tweet to Podio
	 * 
	 * @param tweet
	 *            The tweet to publish
	 * @return <code>true</code> if the tweet was added, <code>false</code>
	 * @throws TwitterException
	 *             if there was an error communicating with Podio
	 */
	private boolean publish(Tweet tweet) throws TwitterException {
		Status status = twitter.showStatus(tweet.getId());

		if (getItemId(tweet.getId()) != null) {
			System.out.println("Already added");
			return false;
		}

		if (status.isRetweet()) {
			Integer retweetItemId = getItemId(status.getRetweetedStatus()
					.getId());
			if (retweetItemId == null) {
				System.out.println("Unable to find original tweet");
				return false;
			}

			CommentAPI commentAPI = apiFactory.getCommentAPI();
			List<Comment> comments = commentAPI.getComments(new Reference(
					ReferenceType.ITEM, retweetItemId));
			for (Comment comment : comments) {
				if (comment.getExternalId() != null
						&& comment.getExternalId().equals(
								Long.toString(tweet.getId()))) {
					System.out.println("Already added as comment");
					return false;
				}
			}

			Integer profileImageId = uploadProfile(status);
			List<Integer> fileIds = new ArrayList<Integer>();
			if (profileImageId != null) {
				fileIds.add(profileImageId);
			}

			String text = getFullText(tweet, status);
			text += "<br />";
			text += "<br />";
			text += getAuthorLink(tweet, status);
			text += "<br />";
			text += "<br />";
			text += getTweetLink(tweet);

			int commentId = commentAPI.addComment(new Reference(
					ReferenceType.ITEM, retweetItemId),
					new CommentCreate(text, Long.toString(tweet.getId()),
							Collections.<Integer> emptyList(), fileIds), false);

			uploadURLs(status, new Reference(ReferenceType.COMMENT, commentId));

			System.out.println("Added retweet " + tweet.getText());

			return true;
		} else {
			Integer imageId = uploadProfile(status);

			List<FieldValuesUpdate> fields = new ArrayList<FieldValuesUpdate>();
			fields.add(new FieldValuesUpdate(TEXT, "value", tweet.getText()));
			fields.add(new FieldValuesUpdate(TWEET, "value", getFullText(tweet,
					status)));
			fields.add(new FieldValuesUpdate(FROM, "value", getAuthorLink(
					tweet, status)));
			if (imageId != null) {
				fields.add(new FieldValuesUpdate(AVATAR, "value", imageId));
			}
			if (tweet.getLocation() != null) {
				fields.add(new FieldValuesUpdate(LOCATION, "value", tweet
						.getLocation()));
			}
			if (tweet.getSource() != null) {
				fields.add(new FieldValuesUpdate(SOURCE, "value", tweet
						.getSource()));
			}
			fields.add(new FieldValuesUpdate(LINK, "value", getTweetLink(tweet)));
			if (status.getInReplyToStatusId() > 0) {
				Integer replyToItemId = getItemId(status.getInReplyToStatusId());
				if (replyToItemId != null) {
					fields.add(new FieldValuesUpdate(REPLY_TO, "value",
							replyToItemId));
				}
			}

			int itemId = apiFactory.getItemAPI().addItem(
					APP_ID,
					new ItemCreate(Long.toString(tweet.getId()), fields,
							Collections.<Integer> emptyList(), Arrays
									.asList(status.getHashtags())), false);

			Reference reference = new Reference(ReferenceType.ITEM, itemId);
			uploadURLs(status, reference);

			System.out.println("Added tweet " + tweet.getText());

			return true;
		}
	}

	/**
	 * Returns the link to the tweet
	 * 
	 * @param tweet
	 *            The tweet to link to
	 * @return The link
	 */
	private String getTweetLink(Tweet tweet) {
		return "http://twitter.com/" + tweet.getFromUser() + "/status/"
				+ tweet.getId();
	}

	/**
	 * Returns the link to the author of the tweet
	 * 
	 * @param tweet
	 *            The tweet in question
	 * @param status
	 *            The status of the tweet
	 * @return The link
	 */
	private String getAuthorLink(Tweet tweet, Status status) {
		return "<a href=\"http://twitter.com/" + tweet.getFromUser() + "\">"
				+ status.getUser().getName() + "</a>";
	}

	/**
	 * Uploads the profile picture to Podio
	 * 
	 * @param status
	 *            The status to upload the photo from
	 * @return The file id of the uploaded file
	 */
	private Integer uploadProfile(Status status) {
		Integer imageId = null;
		FileAPI fileAPI = apiFactory.getFileAPI();
		try {
			imageId = fileAPI
					.uploadImage(status.getUser().getProfileImageURL());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return imageId;
	}

	/**
	 * 
	 * 
	 * @param status
	 * @param reference
	 */
	private void uploadURLs(Status status, Reference reference) {
		for (URL url : status.getURLs()) {
			uploadURL(url, reference);
		}
	}

	/**
	 * Uploads a single URL to the given reference if the URL is a link to an
	 * image. Currently only images from twitpic and yfrog is supported.
	 * 
	 * @param url
	 *            The URL to the image
	 * @param reference
	 *            The reference to attach the image to
	 */
	private void uploadURL(URL url, Reference reference) {
		if (url.getHost().equals("twitpic.com")) {
			try {
				URL imageURL = new URL("http://twitpic.com/show/large"
						+ url.getPath());
				String contentType = imageURL.openConnection().getContentType();
				String name = url.getPath().substring(1);
				if (contentType.contains("png")) {
					name += ".png";
				} else if (contentType.contains("jpg")
						|| contentType.contains("jpeg")) {
					name += ".jpg";
				} else if (contentType.contains("gif")) {
					name += ".gif";
				}

				apiFactory.getFileAPI().uploadImage(imageURL, name, reference);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else if (url.getHost().equals("yfrog.com")) {
			try {
				URL imageURL = new URL(url.toString() + ":medium");
				String contentType = imageURL.openConnection().getContentType();
				String name = url.getPath().substring(1);
				if (contentType.contains("png")) {
					name += ".png";
				} else if (contentType.contains("jpg")
						|| contentType.contains("jpeg")) {
					name += ".jpg";
				} else if (contentType.contains("gif")) {
					name += ".gif";
				}

				apiFactory.getFileAPI().uploadImage(imageURL, name, reference);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Returns the item id for a given tweet id.
	 * 
	 * @param id
	 *            The id of the tweet
	 * @return The item id if the tweet exists in Podio already,
	 *         <code>null</code> otherwise
	 */
	private Integer getItemId(long id) {
		ItemsResponse response = apiFactory.getItemAPI().getItemsByExternalId(
				APP_ID, Long.toString(id));

		if (response.getFiltered() == 0) {
			return null;
		}

		return response.getItems().get(0).getId();
	}

	/**
	 * Runs the importer
	 * 
	 * @param args
	 *            The first argument is mandatory and must specify the
	 *            configuration file to use
	 * @throws IOException
	 *             if there was an error reading the configuration file
	 * @throws TwitterException
	 *             if there was an error communicating with Twitter
	 */
	public static void main(String[] args) throws IOException, TwitterException {
		Importer importer = new Importer(args[0]);
		importer.process();
	}
}
