package com.podio.sample.twitter;

public class MarkdownTweetPrinter implements TweetPrinter {

	@Override
	public String getLink(String text, String url) {
		return "[" + text + "](" + url + ")";
	}
}
