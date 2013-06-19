package com.podio.sample.twitter;

import java.io.IOException;

import twitter4j.FilterQuery;
import twitter4j.Status;
import twitter4j.StatusListener;
import twitter4j.TwitterException;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.UserStreamAdapter;
import twitter4j.auth.OAuthAuthorization;
import twitter4j.conf.Configuration;

public class StreamTwitterReader extends BaseTwitterReader {

	private TwitterStream twitter;

	public StreamTwitterReader(Configuration configuration) throws IOException,
			TwitterException {
		this.twitter = new TwitterStreamFactory(configuration)
				.getInstance(new OAuthAuthorization(configuration));
	}

	@Override
	public void process(final TwitterWriter writer) throws TwitterException {
		StatusListener listener = new UserStreamAdapter() {
			@Override
			public void onStatus(Status status) {
				try {
					writer.write(status);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			@Override
			public void onException(Exception ex) {
				ex.printStackTrace();
			}
		};
		twitter.addListener(listener);

		FilterQuery query = new FilterQuery();
		query.track(TERMS);
		twitter.filter(query);
	}
}
