package com.podio.sample.twitter;

import java.io.FileInputStream;
import java.util.Properties;

import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

/**
 * Imports tweets
 * 
 */
public class Importer {

	/**
	 * Runs the importer
	 * 
	 * @param args
	 *            The first argument is mandatory and must specify the
	 *            configuration file to use
	 */
	public static void main(String[] args) throws Exception {
		Properties properties = new Properties();
		properties.load(new FileInputStream(args[0]));

		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setDebugEnabled(true)
				.setOAuthConsumerKey(properties.getProperty("twitter.token"))
				.setOAuthConsumerSecret(
						properties.getProperty("twitter.secret"))
				.setOAuthAccessToken(
						properties.getProperty("twitter.access_token"))
				.setOAuthAccessTokenSecret(
						properties.getProperty("twitter.access_token_secret"))
				.setUseSSL(true);
		Configuration configuration = cb.build();

		TwitterReader searchReader = new SearchTwitterReader(configuration);
		TwitterReader streamReader = new StreamTwitterReader(configuration);

		TwitterWriter writer = new PodioTwitterWriter(properties);

		try {
			System.out.println("Starting search");

			searchReader.process(writer);

			System.out.println("Starting stream");

			streamReader.process(writer);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
