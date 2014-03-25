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

import it.polimi.modaclouds.monitoring.kb.dto.SDA;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class KBConnector {

	private static KBConnector _instance = null;

	public static KBConnector getInstance() throws MalformedURLException {
		if (_instance == null) {
			_instance = new KBConnector();
		}
		return _instance;
	}


	private Logger logger = LoggerFactory.getLogger(KBConnector.class
			.getName());

	private URL kbURL;

	public URL getKbURL() {
		return kbURL;
	}

	public void setKbURL(URL kbURL) {
		this.kbURL = kbURL;
	}

	private KBConnector() throws MalformedURLException {
		loadConfig();
	}

	private void loadConfig() throws MalformedURLException {
		Config config = Config.getInstance();
		String kbAddress = config.getKBServerAddress();
		int ddaPort = config.getKBServerPort();
		kbAddress = cleanAddress(kbAddress);
		kbURL = new URL("http://" + kbAddress + ":" + ddaPort);
	}

	
	private static String cleanAddress(String address) {
		if (address.indexOf("://") != -1) address = address.substring(address.indexOf("://")+3);
		if (address.endsWith("/")) address = address.substring(0, address.length()-1);
		return address;
	}

	public void installSDA(SDA fSda) {
		
	}

	public List<SDA> getInstalledSDAs() {
		// TODO Auto-generated method stub
		return null;
	}

	public void setStarted(SDA sda) {
		// TODO Auto-generated method stub
		
	}
}
