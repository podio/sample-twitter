package com.podio.sample.twitter;

public class HTMLTweetPrinter implements TweetPrinter {

	@Override
	public String getLink(String text, String url) {
		return "<a href=\"" + url + "\" target=\"_blank\">" + text + "</a>";
	}
}
