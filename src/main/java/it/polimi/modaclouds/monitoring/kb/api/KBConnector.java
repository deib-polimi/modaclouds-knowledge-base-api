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

import it.polimi.modaclouds.monitoring.kb.dto.KBEntity;

import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KBConnector {

	private static KBConnector _instance = null;

	public static KBConnector getInstance() throws MalformedURLException, FileNotFoundException {
		if (_instance == null) {
			_instance = new KBConnector();
		}
		return _instance;
	}

	private Logger logger = LoggerFactory
			.getLogger(KBConnector.class.getName());

	private URL kbURL;
	private String myID;
	

	public URL getKbURL() {
		return kbURL;
	}

	public void setKbURL(URL kbURL) {
		this.kbURL = kbURL;
	}

	private KBConnector() throws MalformedURLException, FileNotFoundException {
		loadConfig();
	}

	private void loadConfig() throws MalformedURLException, FileNotFoundException {
		Config config = Config.getInstance();
		String kbAddress = config.getKBServerAddress();
		int ddaPort = config.getKBServerPort();
		kbAddress = cleanAddress(kbAddress);
		kbURL = new URL("http://" + kbAddress + ":" + ddaPort);
		myID = config.getMyID();
	}

	private static String cleanAddress(String address) {
		if (address.indexOf("://") != -1)
			address = address.substring(address.indexOf("://") + 3);
		if (address.endsWith("/"))
			address = address.substring(0, address.length() - 1);
		return address;
	}

	public void add(KBEntity entity) {
		try {
			Map<String, String> properties = BeanUtils.describe(entity);
			for (String p : properties.keySet()) {

			}
		} catch (IllegalAccessException | InvocationTargetException
				| NoSuchMethodException e) {
			logger.error("Error while retrieving getters from entity", e);
		}
	}

	public void addAll(List<KBEntity> entities) {
		// TODO
	}

	public <T extends KBEntity> List<T> getAll(Class<T> entityClass) {
		// TODO
		return null;
	}

	public <T extends KBEntity> T get(String id, Class<T> entityClass) {
		// TODO
		return null;
	}

	public <T> void delete(String id, Class<T> entityClass) {
		// TODO
	}
	
	public String getMyId() {
		return myID;
	}

}
