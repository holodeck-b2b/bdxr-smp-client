package org.holodeckb2b.bdxr.smp.api;

import java.io.InputStream;

/**
 * Defines the interface for accessing the SMP response received. This interface has only two methods, one to get 
 * access to the data of the response and one to close the connection. The separate close method allows that a {@link 
 * IRequestExecutor} can manage its resources.  
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public interface ISMPResponseConnection {

	/**
	 * Gets the input stream to access the SMP response.
	 *  
	 * @return	The input stream to the SMP response data.	
	 * @throws SMPQueryException	When the input stream to the result is not available. This might be caused by
	 * 								problems with the SMP server but also indicate that the requested information is
	 * 								not available.
	 */
	public InputStream getInputStream() throws SMPQueryException;
	
	/**
	 * Closes the connection and allows the connection manager to free up resources. This method is called by the
	 * {@link ISMPClient} after it has completely processed the SMP result.  
	 */
	public void close();	
}
