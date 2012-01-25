package com.podio.sample.twitter;

import twitter4j.Status;

public interface TwitterWriter {

	/**
	 * Publishes the tweet to Podio
	 * 
	 * @param tweet
	 *            The tweet to publish
	 * @return <code>true</code> if the tweet was added, <code>false</code>
	 */
	boolean write(Status status) throws Exception;

}