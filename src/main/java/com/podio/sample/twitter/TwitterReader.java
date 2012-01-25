package com.podio.sample.twitter;

public interface TwitterReader {

	/**
	 * Reads from Twitter and posts to the Twitter app
	 */
	public void process(TwitterWriter writer) throws Exception;

}