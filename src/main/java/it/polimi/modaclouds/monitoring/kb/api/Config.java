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

import java.io.FileNotFoundException;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Config {
	private static Config _instance = null;
	private static final Logger logger = LoggerFactory.getLogger(Config.class); 
	
	private Configuration config;
	
	private Config() throws FileNotFoundException{
		try {
			config = new PropertiesConfiguration("kb.properties");
		} catch (ConfigurationException e) {
			logger.error("Error while reading the configuration file", e);
		}
		if (config ==null) throw new FileNotFoundException("kb.properties file not found");
	}
			
	public static Config getInstance() throws FileNotFoundException{
		if(_instance==null)
			_instance=new Config();
		return _instance;
	}
	
	public int getKBServerPort(){
		return config.getInt("kb_server.port");
	}
	
	public String getKBServerAddress(){
		return config.getString("kb_server.address");
	}

	
}
