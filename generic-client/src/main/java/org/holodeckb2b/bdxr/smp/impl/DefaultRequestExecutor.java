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
package org.holodeckb2b.bdxr.smp.impl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.holodeckb2b.bdxr.smp.api.IRequestExecutor;
import org.holodeckb2b.bdxr.smp.api.ISMPResponseConnection;
import org.holodeckb2b.bdxr.smp.api.SMPQueryException;

/**
 * Is the default implementation of {@link IRequestExecutor} and uses a standard {@link URLConnection} to execute the
 * SMP request. It therefore only supports HTTP for executing the queries. There is no connecting pooling or caching
 * of results.
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
	public ISMPResponseConnection executeRequest(URL requestURL) throws SMPQueryException, UnsupportedOperationException {
		if ("https".equalsIgnoreCase(requestURL.getProtocol())) {
			log.fatal("SMP requests using https are not supported!");
			throw new UnsupportedOperationException("https is not supported");		
		}
		final String smpServer = requestURL.getHost() + ":" + requestURL.getPort(); 		
		try {
			log.trace("Opening connecting to SMP server.\n\tSMP server  : {}\n\tTime out (s): {}", smpServer,
					  timeout / 1000);
	    	URLConnection urlConnection = requestURL.openConnection();
	        urlConnection.setConnectTimeout(timeout);
	        urlConnection.setReadTimeout(timeout);
	        log.trace("Starting query: {}", requestURL.getPath());
        	return new SMPResponseConnection(urlConnection.getInputStream());
		} catch (FileNotFoundException notFound) {
			log.warn("The requested meta-data could not be found! Request URL: {}", requestURL.toString());
			throw new SMPQueryException("Requested meta-data not found!");
		} catch (IOException connectionError) {
			log.error("An error occurred while connecting to the SMP server at {}. Error message: {}", smpServer, 
						connectionError.getMessage());
			throw new SMPQueryException("Error while connecting to the SMP server", connectionError);
		}	
	}

	/**
	 * Is the {@link ISMPResponseConnection} implementation for this default request executor implementation. Since
	 * there is no connection pooling the handling is very simple, just passing the input stream and closing.
	 */
	class SMPResponseConnection implements ISMPResponseConnection {
		private InputStream	responseIS;
		
		SMPResponseConnection(final InputStream is) {
			this.responseIS = is;
		}
		
		@Override
		public InputStream getInputStream() throws SMPQueryException {
			return responseIS;
		}

		@Override
		public void close() {
			try {
				this.responseIS.close();
			} catch (IOException e) {
				log.warn("An error occurred when closing the connection to the SMP server!");
			}			
		}	
	}
}
