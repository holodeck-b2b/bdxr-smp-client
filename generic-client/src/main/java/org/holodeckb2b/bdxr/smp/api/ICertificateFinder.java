package org.holodeckb2b.bdxr.smp.api;

import java.security.cert.X509Certificate;

import javax.xml.crypto.AlgorithmMethod;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;

/**
 * Defines the interface of the component that finds the certificate that was used by the SMP server to sign the 
 * results. Although all versions of the SMP specification state that the certificate used for signing must be 
 * included within the <code>ds:Signature</code> element this interface allows to use other <b>non standard</n> ways of 
 * referencing the certificate. 
 *  
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public interface ICertificateFinder {
	
	/**
	 * Finds the certificate used by the SMP server for signing the result based on the information provided in the
	 * XML signature.
	 * 
	 * @param keyInfo	The information contained in the ds:KeyInfo element  
	 * @param method	The signature algorithm used for creating the signature, may be needed to determine if a key/
	 * 					cert is applicable
	 * @param context	The crypto processing context 
	 * @return	The X509 Certificate used for the signature,<br>
	 * 			or <code>null</code> if the certificate cannot be found
	 */
	X509Certificate findCertificate(final KeyInfo keyInfo, final AlgorithmMethod method, 
									final XMLCryptoContext context);
}
