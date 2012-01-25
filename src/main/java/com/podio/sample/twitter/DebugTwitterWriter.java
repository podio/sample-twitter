package com.podio.sample.twitter;

import twitter4j.Status;

public class DebugTwitterWriter implements TwitterWriter {

	@Override
	public boolean write(Status status) throws Exception {
		System.out.println(status.getText());
		return false;
	}

}
