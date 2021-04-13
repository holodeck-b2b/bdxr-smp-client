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

import java.security.cert.X509Certificate;
import java.util.Iterator;

import javax.xml.crypto.AlgorithmMethod;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.X509Data;

import org.holodeckb2b.bdxr.smp.api.ICertificateFinder;

/**
 * Is the default implementation of the {@link ICertificateFinder} interface that retrieves the X509 Certificate that
 * was used to sign the SMP from the <code>ds:Signature/ds:KeyInfo/ds:X509Data/ds:X509Certificate</code> element.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class DefaultCertFinder implements ICertificateFinder {

	/**
     * Gets the certificate for verification of the signature from the <code>ds:KeyInfo/ds:X509Data/ds:X509Certificate 
     * </code> element. 
     * 
     * {@inheritDoc}
     */
    @Override
	public X509Certificate findCertificate(KeyInfo keyInfo, AlgorithmMethod method, XMLCryptoContext context) {
    	X509Certificate foundCert = null; 
    	Iterator ki = keyInfo.getContent().iterator();
        while (foundCert == null && ki.hasNext()) {
            XMLStructure info = (XMLStructure) ki.next();
            if (info instanceof X509Data) {
            	X509Data x509Data = (X509Data) info;
            	Iterator xi = x509Data.getContent().iterator();
            	while (foundCert == null && xi.hasNext()) {
            		Object o = xi.next();
            		if (o instanceof X509Certificate) {
            			// Make sure the algorithm is compatible with the method.
            			if (checkKeyAlgorithm(method.getAlgorithm(), 
            								  ((X509Certificate) o).getPublicKey().getAlgorithm()))
            				foundCert = (X509Certificate) o;
            		}
            	}
            }
        }        
        return foundCert;
    }

    /**
     * Check that the key can be used with the algorithm used for signing.
     * 
     * @param signatureAlgorithm	The URI identifying the signature algorithm
     * @param keyAlgorithm			The type of key
     * @return	boolean indicating whether the key can be used with the signature algorithm
     */
    private boolean checkKeyAlgorithm(final String signatureAlgorithm, final String keyAlgorithm) {
        return (keyAlgorithm.equalsIgnoreCase("DSA") && signatureAlgorithm.contains("xmldsig#dsa"))
        	|| (keyAlgorithm.equalsIgnoreCase("RSA") && (signatureAlgorithm.contains("xmldsig#rsa") 
        												|| signatureAlgorithm.contains("xmldsig-more#rsa"))
        	   );
    }
}
