package com.podio.sample.twitter;

import java.io.FileInputStream;
import java.util.Properties;

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

		TwitterReader streamReader = new StreamTwitterReader(properties);
		TwitterReader searchReader = new SearchTwitterReader(properties);

		TwitterWriter writer = new PodioTwitterWriter(properties);

		while (true) {
			try {
				System.out.println("Starting search");

				searchReader.process(writer);

				// System.out.println("Starting stream");

				// streamReader.process(writer);
				return;
			} catch (Exception e) {
				e.printStackTrace();
				Thread.sleep(1000 * 60 * 10);
			}
		}
	}
}
