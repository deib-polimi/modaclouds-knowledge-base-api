package it.polimi.modaclouds.monitoring.kb.api;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.impl.PropertyImpl;
import com.hp.hpl.jena.rdf.model.impl.ResourceImpl;


public class KBConfig {

	public static final String namespace = "http://www.modaclouds.eu/rdfs/1.0/entities#";
	public static final String uriPrefix = "modaent";
	
	public static final Property javaClassRDFProperty = new PropertyImpl(namespace, "class");
	public static final Property keyRDFProperty = new PropertyImpl(namespace, "key");
	public static final Property valueRDFProperty = new PropertyImpl(namespace, "value");
	
	public static final Resource MapRDFResource = new ResourceImpl(namespace + "Map");
}
