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

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.UUID;

public abstract class KBEntity {

	private String id;
	private URI shortUri;
	private URI uri;
	private String uriBase;
	private String uriPrefix;

	public KBEntity(String id) throws URISyntaxException {
		uriBase = getURIBase();
		if (!uriBase.endsWith("/"))
			uriBase += "/";
		uriPrefix = getURIPrefix();
		setId(id);
	}

	public KBEntity() throws URISyntaxException {
		this(UUID.randomUUID().toString());
	}

	public abstract String getURIBase();

	public abstract String getURIPrefix();

	public String getId() {
		return id;
	}

	public void setId(String id) throws URISyntaxException {
		this.id = id;
		String encodedId;
		try {
			encodedId = URLEncoder.encode(id, "UTF-8");
			this.shortUri = new URI(uriPrefix + ":" + encodedId);
			this.uri = new URI(uriBase + encodedId);
		} catch (UnsupportedEncodingException e) {
			throw new IllegalArgumentException(e);
		}
	}

	public String getShortClassURI() {
		return uriPrefix + ":" + this.getClass().getSimpleName();
	}

	public String getClassURI() {
		return uriBase + this.getClass().getSimpleName();
	}

	public URI getUri() {
		return uri;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result
				+ ((shortUri == null) ? 0 : shortUri.hashCode());
		result = prime * result + ((uri == null) ? 0 : uri.hashCode());
		result = prime * result + ((uriBase == null) ? 0 : uriBase.hashCode());
		result = prime * result
				+ ((uriPrefix == null) ? 0 : uriPrefix.hashCode());
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
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
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
		if (uriBase == null) {
			if (other.uriBase != null)
				return false;
		} else if (!uriBase.equals(other.uriBase))
			return false;
		if (uriPrefix == null) {
			if (other.uriPrefix != null)
				return false;
		} else if (!uriPrefix.equals(other.uriPrefix))
			return false;
		return true;
	}

	public URI getShortURI() {
		return shortUri;
	}

}
