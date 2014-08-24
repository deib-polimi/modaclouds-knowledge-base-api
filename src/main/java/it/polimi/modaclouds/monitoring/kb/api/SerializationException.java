package it.polimi.modaclouds.monitoring.kb.api;

public class SerializationException extends Exception {

	public SerializationException(Exception e) {
		super(e);
	}

	public SerializationException(String message) {
		super(message);
	}

}
