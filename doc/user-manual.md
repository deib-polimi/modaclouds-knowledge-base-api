[Documentation table of contents](TOC.md)

# Usage

This api is used to perform CRUD operations against Apache Fuseki RDF triple store in an object
oriented fashion.
See the `it.polimi.modaclouds.monitoring.kb.api.FusekiKBAPIIT.java` class for usage examples.

Any object that can be persisted is called entity.
Allowed data types inside entities are:
* `String`
* `Map<?,?>` `?` can be any type in this list
* `List<?>` `?` can be any type in this list
* `Set<?>` `?` can be any type in this list
* An entity

Entities are neither written nor deleted recursively. Reads are instead recursive.

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