/**
 * Copyright 2014 deib-polimi
 * Contact: deib-polimi <marco.miglierina@polimi.it>
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package it.polimi.modaclouds.monitoring.kb.api;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Util {

	private static final Logger logger = LoggerFactory.getLogger(Util.class);

	private static boolean isResponseCode(String url, int expectedCode)
			throws MalformedURLException, IOException {
		HttpURLConnection connection = (HttpURLConnection) new URL(url)
				.openConnection();
		connection.setRequestMethod("GET");
		connection.connect();
		return connection.getResponseCode() == expectedCode;
	}

	public static void waitForResponseCode(String url, int expectedCode,
			int retryTimes, int retryPeriodInMilliseconds) throws IOException {
		while (true) {
			try {
				if (isResponseCode(url, expectedCode))
					return;
			} catch (Exception e) {
			}
			retryTimes--;
			if (retryTimes <= 0) {
				throw new IOException("Could not connect to the service.");
			}
			try {
				logger.info("Connection failed, retrying in {} seconds...",
						retryPeriodInMilliseconds / 1000);
				Thread.sleep(retryPeriodInMilliseconds);
			} catch (InterruptedException e) {
				throw new IOException();
			}
		}
	}

	public static int findFreePort() {
		ServerSocket socket = null;
		try {
			socket = new ServerSocket(0);
			socket.setReuseAddress(true);
			int port = socket.getLocalPort();
			try {
				socket.close();
			} catch (IOException e) {
				// Ignore IOException on close()
			}
			return port;
		} catch (IOException e) {
		} finally {
			if (socket != null) {
				try {
					socket.close();
				} catch (IOException e) {
				}
			}
		}
		throw new IllegalStateException(
				"Could not find a free TCP/IP port to start the data analyzer on");

	}

}
