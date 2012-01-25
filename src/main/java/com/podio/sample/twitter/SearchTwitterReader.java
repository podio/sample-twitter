package com.podio.sample.twitter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Tweet;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

public class SearchTwitterReader implements TwitterReader {

	/**
	 * The query to use for Twitter
	 */
	private static final String QUERY = "@podio OR #podio OR @podiosupport OR podio.com";

	/**
	 * The interface to Twitter
	 */
	private final Twitter twitter;

	/**
	 * Creates a new importer with configuration read from the given file.
	 * 
	 * @param configFile
	 *            The configuration file to use. For an example file see the
	 *            example.config.properties in the root folder.
	 * @throws IOException
	 *             If there was an error reading from the configuration file
	 */
	public SearchTwitterReader(Properties properties) throws IOException {
		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setDebugEnabled(true)
				.setOAuthConsumerKey(properties.getProperty("twitter.token"))
				.setOAuthConsumerSecret(
						properties.getProperty("twitter.secret"));

		this.twitter = new TwitterFactory(cb.build()).getInstance();
	}

	@Override
	public void process(TwitterWriter writer) throws Exception {
		QueryResult result = twitter.search(new Query(QUERY));

		List<Tweet> tweets = result.getTweets();
		Collections.reverse(tweets);
		for (Tweet tweet : tweets) {
			Status status = twitter.showStatus(tweet.getId());
			writer.write(status);
		}
	}
}
