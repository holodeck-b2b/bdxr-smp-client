/*
 * Copyright (C) 2018 The Holodeck B2B Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.holodeckb2b.bdxr.smp.client.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.holodeckb2b.bdxr.smp.client.api.IRequestExecutor;
import org.holodeckb2b.bdxr.smp.client.api.ISMPResponse;
import org.holodeckb2b.bdxr.smp.client.api.SMPQueryException;
import org.holodeckb2b.commons.util.Utils;

/**
 * Is the default implementation of {@link IRequestExecutor} and uses a standard {@link HttpURLConnection} to execute
 * the SMP request.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class DefaultRequestExecutor implements IRequestExecutor {
	private static final Logger	log = LogManager.getLogger(DefaultRequestExecutor.class);

	/**
	 * Default time out for executing the request is 10 seconds
	 */
	private static final int DEFAULT_TIMEOUT = 10000;
	/**
	 * The configured timeout for executing the request
	 */
	private int timeout;

	/**
	 * Creates a new instance with the default time out setting
	 */
	public DefaultRequestExecutor() {
		this(DEFAULT_TIMEOUT);
	}

	/**
	 * Creates a new instance with the given time out
	 *
	 * @param timeout	The time out in seconds for executing the SMP request
	 */
	public DefaultRequestExecutor(final int timeout) {
		this.timeout = timeout;
	}

	@Override
	public ISMPResponse executeRequest(final URL requestURL, final String lastModified)
															throws SMPQueryException, UnsupportedOperationException {
		final String smpServer = requestURL.getHost() + ":" + requestURL.getPort();
		try {
			log.trace("Connecting to SMP server {}, using time out of {} seconds", smpServer, timeout / 1000);
	    	HttpURLConnection conn = (HttpURLConnection) requestURL.openConnection();
	        conn.setConnectTimeout(timeout);
	        conn.setReadTimeout(timeout);
			if (!Utils.isNullOrEmpty(lastModified)) {
				log.trace("Setting If-Modified-Since header to {}", lastModified);
				conn.setRequestProperty("If-Modified-Since", lastModified);
			}
	        log.trace("Starting query: {}", requestURL.getPath());
			final int status = conn.getResponseCode();
			log.trace("Executed request to {}", requestURL.toString());
			if (status / 200 == 1)
				return new SMPResponseConnection(status,
											 conn.getHeaderFields().entrySet().parallelStream()
															.filter(h -> "last-modified".equalsIgnoreCase(h.getKey()))
															.findFirst().map(h -> h.getValue().get(0)).orElse(null),
											 conn.getInputStream());
			else
				return new SMPResponseConnection(status, null, null);
		} catch (ClassCastException unsupportedProtocol) {
			log.error("Unsupported transport protocol ({})", requestURL.getProtocol());
			throw new UnsupportedOperationException();
		} catch (IOException connectionError) {
			log.error("An error occurred while connecting to the SMP server at {}. Error message: {}", smpServer,
						connectionError.getMessage());
			throw new SMPQueryException("Error while connecting to the SMP server", connectionError);
		}
	}

	/**
	 * Is the {@link ISMPResponse} implementation for this default request executor implementation. Since
	 * there is no connection pooling the handling is very simple, just a proxy of the connection's input stream.
	 */
	public static class SMPResponseConnection implements ISMPResponse {
		private int				status;
		private String			lastModified;
		private InputStream		contentStream;

		public SMPResponseConnection(final int status, final String lm, final InputStream is) {
			this.status = status;
			this.lastModified = lm;
			this.contentStream = is;
		}

		@Override
		public int getStatusCode() throws SMPQueryException {
			return status;
		}

		@Override
		public String getLastModified() throws SMPQueryException {
			return lastModified;
		}

		@Override
		public InputStream getInputStream() throws SMPQueryException {
			return contentStream;
		}

		@Override
		public void close() {
			if (this.contentStream != null)
				try {
					this.contentStream.close();
				} catch (IOException e) {
					log.warn("An error occurred when closing the connection to the SMP server!");
				}
		}
	}
}
