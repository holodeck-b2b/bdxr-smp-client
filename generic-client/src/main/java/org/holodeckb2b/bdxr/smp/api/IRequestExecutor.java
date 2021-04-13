package org.holodeckb2b.bdxr.smp.api;

import java.net.URI;
import java.net.URL;

/**
 * Defines the interface for the component responsible for executing the HTTP(S) request to the SMP server. 
 * <p>Implementations can optimise the handling of connections, for example by using connection pooling, or implement
 * a specific authentication and/or trust mechanism. Implementations should ensure that they are thread safe, i.e.
 * the {@link #executeRequest(URI)} can be called multiple time, in parallel. Any initialisation that is needed should 
 * be done before the executor is provided to the {@link SMPClientBuilder}. 
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public interface IRequestExecutor {

	/**
	 * Executes a HTTP GET method using the provided request URL to retrieve the meta-data about a participant.    
	 * 
	 * @param requestURL	The complete SMP query URL
	 * @return A {@link ISMPResponseConnection} to get access to the SMP response 
	 * @throws SMPQueryException	When the request executor can not complete the request and is unable to provide
	 * 								access to the response. Reasons could be that the server does not accept 
	 * 								connections or does not respond 
	 * @throws UnsupportedOperationException When the request executor implementation does not support the requested
	 * 										 protocol, e.g. if only http is supported but the requestURL is requesting
	 * 										 https
	 */
	ISMPResponseConnection executeRequest(final URL requestURL) 
			 												 throws SMPQueryException,  UnsupportedOperationException;
}
