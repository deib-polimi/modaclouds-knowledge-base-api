[Documentation table of contents](TOC.md)

# Usage

This api is used to perform CRUD operations against the Monitoring Knowledege Base in an object
oriented fashion.

## Installation

You can download the jar from https://github.com/deib-polimi/modaclouds-knowledge-base-api/releases or add the dependency to your Maven project.

Releases repository:
```xml
<repositories>
	...
	<repository>
        <id>deib-polimi-releases</id>
        <url>https://github.com/deib-polimi/deib-polimi-mvn-repo/raw/master/releases</url>
	</repository>
	...
</repositories>
```

Snapshots repository:
```xml
<repositories>
	...
	<repository>
        <id>deib-polimi-snapshots</id>
        <url>https://github.com/deib-polimi/deib-polimi-mvn-repo/raw/master/snapshots</url>
	</repository>
	...
</repositories>
```

Dependency:
```xml
<dependencies>
	<dependency>
		<groupId>it.polimi.modaclouds.monitoring</groupId>
		<artifactId>knowledge-base-api</artifactId>
		<version>VERSION</version>
	</dependency>
</dependencies>
```

## Configuration

The fuseki server should be started with the following configuration file,
and after creating the *ds* folder:

```
@prefix fuseki:  <http://jena.apache.org/fuseki#> .
@prefix rdf:     <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
@prefix rdfs:    <http://www.w3.org/2000/01/rdf-schema#> .
@prefix tdb:     <http://jena.hpl.hp.com/2008/tdb#> .
@prefix ja:      <http://jena.hpl.hp.com/2005/11/Assembler#> .
@prefix :        <#> .
[] rdf:type fuseki:Server ;
   fuseki:services (
     <#service1> 
   ) .

<#service1>  rdf:type fuseki:Service ;
    fuseki:name                       "modaclouds/kb" ;
    fuseki:serviceQuery               "query" ;
    fuseki:serviceQuery               "sparql" ;
    fuseki:serviceUpdate              "update" ;
    fuseki:serviceUpload              "upload" ;
    fuseki:serviceReadWriteGraphStore "data" ;
    fuseki:dataset           <#dataset> ;
    .

<#dataset> rdf:type      tdb:DatasetTDB ;
    tdb:location "ds" ;
    ja:context [ ja:cxtName "arq:queryTimeout" ;  ja:cxtValue "1000" ] ;
    tdb:unionDefaultGraph true ;
     .
```

Any application using this api should have in its build-path a "kb.properties" file with
information about the fuseki server port and address. Here is an example 
with a local fuseki instance:

```
kb_server.port=3030
kb_server.address=localhost
```