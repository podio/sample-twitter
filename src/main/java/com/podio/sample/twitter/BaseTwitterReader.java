package com.podio.sample.twitter;


public abstract class BaseTwitterReader implements TwitterReader {

	protected static final String[] TERMS = new String[] { "@podio", "#podio",
			"@podiosupport", "podio.com" };
}
