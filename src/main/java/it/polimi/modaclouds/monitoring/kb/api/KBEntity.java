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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

public abstract class KBEntity {

	public static final String uriBase = "http://www.modaclouds.eu/rdfs/1.0/entities#";
	public static final String uriPrefix = "modaent";

	private URI shortUri;
	private URI uri;

	public KBEntity() {
		try {
			String randomName = UUID.randomUUID().toString();
			this.shortUri = new URI(uriPrefix + ":" + randomName);
			this.uri = new URI(uriBase + randomName);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param uri must not be a short uri
	 */
	void setURI(URI uri) {
		try {
			this.shortUri = new URI(uriPrefix + ":" + uri.getFragment());
			this.uri = uri;
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}

	public URI getUri() {
		return uri;
	}

	public String getShortClassURI() {
		return uriPrefix + ":" + this.getClass().getSimpleName();
	}

	public String getClassURI() {
		return uriBase + this.getClass().getSimpleName();
	}

	public URI getShortURI() {
		return shortUri;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((shortUri == null) ? 0 : shortUri.hashCode());
		result = prime * result + ((uri == null) ? 0 : uri.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		KBEntity other = (KBEntity) obj;
		if (shortUri == null) {
			if (other.shortUri != null)
				return false;
		} else if (!shortUri.equals(other.shortUri))
			return false;
		if (uri == null) {
			if (other.uri != null)
				return false;
		} else if (!uri.equals(other.uri))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "KBEntity [shortUri=" + shortUri + ", uri=" + uri + "]";
	}

}
