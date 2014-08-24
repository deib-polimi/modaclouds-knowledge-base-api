package it.polimi.modaclouds.monitoring.kb.api.examples;

import java.util.Arrays;

import it.polimi.modaclouds.monitoring.kb.api.DeserializationException;
import it.polimi.modaclouds.monitoring.kb.api.FusekiKBAPI;
import it.polimi.modaclouds.monitoring.kb.api.SerializationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Example {
	private static final Logger logger = LoggerFactory.getLogger(Example.class);

	public static void main(String[] args) {
		FusekiKBAPI kb = new FusekiKBAPI("http://localhost:3030/modaclouds/kb");
		try {
			MyEntity entity1 = new MyEntity();
			entity1.setId("1");
			MyEntity entity2 = new MyEntity();
			entity2.setId("2");
			MyEntity entity3 = new MyEntity();
			entity3.setId("3");
			entity3.addElementToList("1");
			entity3.addElementToList("2");
			entity3.addElementToList("3");
			entity3.addElementToList("4");
			entity3.setString("Hello world!");
			kb.add(Arrays.asList(new Object[]{entity1,entity2,entity3}), "id");
			kb.deleteEntitiesById(Arrays.asList(new String[]{"1","2"}),"id");
		} catch (Exception e) {
			logger.error("Error", e);
		}
	}
}
