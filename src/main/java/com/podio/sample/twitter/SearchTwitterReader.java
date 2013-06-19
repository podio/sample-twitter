package com.podio.sample.twitter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.OAuth2Authorization;
import twitter4j.conf.Configuration;

public class SearchTwitterReader extends BaseTwitterReader {

	private Twitter twitter;

	public SearchTwitterReader(Configuration configuration) throws IOException,
			TwitterException {
		OAuth2Authorization authorization = new OAuth2Authorization(
				configuration);
		authorization.getOAuth2Token();

		this.twitter = new TwitterFactory(configuration)
				.getInstance(authorization);
	}

	@Override
	public void process(TwitterWriter writer) throws Exception {
		String query = StringUtils.join(TERMS, " OR ");

		QueryResult result = twitter.search(new Query(query));

		List<Status> statuses = result.getTweets();
		Collections.reverse(statuses);
		for (Status status : statuses) {
			writer.write(status);
		}
	}
}
