package it.polimi.modaclouds.monitoring.kb.examples;

import java.util.List;

import it.polimi.modaclouds.qos_models.monitoring_ontology.KBEntity;

public abstract class DeploymentModelFactory {
	
	static final String baseURI = "http://www.modaclouds.eu/rdfs/1.0/monitoring/";

	public abstract List<KBEntity> getModel();
}
