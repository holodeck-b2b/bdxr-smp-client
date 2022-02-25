package org.holodeckb2b.bdxr.smp.client.api;

import java.io.InputStream;

/**
 * Defines the interface for accessing the SMP response received. It defines four methods to:<ol>
 * <li>get the HTTP status code</li>
 * <li>get the value of the HTTP <i>Last-Modified</i> header</li>
 * <li>get the input stream to read the HTTP entity body, i.e. the actual response document</li>
 * <li>close the connection. The separate close method allows that a {@link IRequestExecutor} can manage its resources.</li>
 * </ol>
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public interface ISMPResponse {
	/**
	 * This status code indicates a successful query of the SMP server
	 */
	static final int OK = 200;
	/**
	 * This status code indicates that the requested meta-data were not found by the SMP server
	 */
	static final int NOT_FOUND = 404;
	/**
	 * This status code indicates that the requested meta-data were not modified and the cached response can be re-used
	 */
	static final int NOT_MODIFIED = 304;

	/**
	 * Gets the HTTP status code sent by the SMP server.
	 *
	 * @return	HTTP status code of the response
	 * @throws SMPQueryException	When the status code is not available, probably caused by a connection problem to
	 *								the SMP server
	 */
	int getStatusCode() throws SMPQueryException;

	/**
	 * Gets the value of the <i>Last-Modified</i> HTTP header included in the response
	 *
	 * @return	the value of the <i>Last-Modified</i> header if set by the server, <code>null</code> otherwise
	 * @throws SMPQueryException When the HTTP headers of the response cannot be read, probably caused by a connection
	 *							 problem to the SMP server
	 */
	String getLastModified() throws SMPQueryException;

	/**
	 * Gets the input stream to access the SMP response.
	 *
	 * @return	The input stream to the SMP response data.
	 * @throws SMPQueryException	When the input stream to the result is not available. This might be caused by
	 * 								problems with the SMP server but also indicate that the requested information is
	 * 								not available.
	 */
	InputStream getInputStream() throws SMPQueryException;

	/**
	 * Closes the connection and allows the connection manager to free up resources. This method is called by the
	 * {@link ISMPClient} after it has completely processed the SMP result.
	 */
	void close();
}
