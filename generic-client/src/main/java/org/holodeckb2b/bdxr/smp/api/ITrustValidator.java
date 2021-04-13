package org.holodeckb2b.bdxr.smp.api;

import java.security.cert.X509Certificate;

/**
 * Defines the interface for the component responsible for deciding whether the X509 Certificate used by the SMP
 * server to sign the result is to be trusted or not. The rules whether the certificate of an SMP should be trusted or 
 * not depend on the environment in which the client is used.  
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public interface ITrustValidator {

	/**
	 * Checks whether the given X509 Certificate that was used by the SMP to sign the results is trusted.
	 * 
	 * @param certificate	The certificate that should be checked
	 * @return	<code>true</code> when the presented certificate is trusted, <code>false</code> if not
	 * @throws SMPQueryException
	 */
	public boolean isTrusted(final X509Certificate certificate) throws SMPQueryException;
}
