package com.podio.sample.twitter;

import java.io.IOException;
import java.util.Properties;

import twitter4j.FilterQuery;
import twitter4j.Status;
import twitter4j.StatusListener;
import twitter4j.StatusStream;
import twitter4j.TwitterException;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.UserStreamAdapter;
import twitter4j.auth.BasicAuthorization;
import twitter4j.conf.ConfigurationBuilder;

public class StreamTwitterReader implements TwitterReader {

	/**
	 * The interface to Twitter
	 */
	private final TwitterStream twitter;

	/**
	 * Creates a new importer with configuration read from the given file.
	 * 
	 * @param configFile
	 *            The configuration file to use. For an example file see the
	 *            example.config.properties in the root folder.
	 * @throws IOException
	 *             If there was an error reading from the configuration file
	 */
	public StreamTwitterReader(Properties properties) throws IOException {
		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setDebugEnabled(true)
				.setOAuthConsumerKey(properties.getProperty("twitter.token"))
				.setOAuthConsumerSecret(
						properties.getProperty("twitter.secret"))
				.setUseSSL(true);

		this.twitter = new TwitterStreamFactory(cb.build())
				.getInstance(new BasicAuthorization(properties
						.getProperty("twitter.username"), properties
						.getProperty("twitter.password")));
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
		};

		FilterQuery query = new FilterQuery();
		query.track(new String[] { "@podio", "#podio", "@podiosupport",
				"podio.com" });

		StatusStream stream = twitter.getFilterStream(query);
		while (true) {
			stream.next(listener);
		}
	}
}
