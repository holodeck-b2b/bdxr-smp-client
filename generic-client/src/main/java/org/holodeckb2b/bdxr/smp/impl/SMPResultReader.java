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

import java.io.IOException;
import java.io.InputStream;
import java.security.Key;
import java.security.cert.X509Certificate;
import java.util.Optional;

import javax.xml.crypto.AlgorithmMethod;
import javax.xml.crypto.KeySelector;
import javax.xml.crypto.KeySelectorException;
import javax.xml.crypto.KeySelectorResult;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.holodeckb2b.bdxr.smp.api.ICertificateFinder;
import org.holodeckb2b.bdxr.smp.api.ISMPResultProcessor;
import org.holodeckb2b.bdxr.smp.api.ITrustValidator;
import org.holodeckb2b.bdxr.smp.api.SMPQueryException;
import org.holodeckb2b.bdxr.smp.datamodel.ISMPQueryResult;
import org.holodeckb2b.bdxr.smp.datamodel.ServiceMetadataResult;
import org.holodeckb2b.commons.util.Utils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Is the component responsible for parsing the SMP response into a XML document, verifying the signature if it is
 * signed, use the configured {@link ITrustValidator} to validate the signing certificate and to use the configured
 * {@link ISMPResultProcessor} to convert the XML document into a object representation of the SMP result.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class SMPResultReader {
    private static final Logger	log = LogManager.getLogger(SMPResultReader.class);

    /**
     * Namespace URI of XML-dsig
     */
    private static final String XMLDSIG_NS = "http://www.w3.org/2000/09/xmldsig#";
    /**
     * Local name of the XML element containing the signature
     */
    private static final String XMLDSIG_SIGNATURE = "Signature";
    /**
     * The configuration used by this SMP Client instance 
     */
    private SMPClientConfig		clientConfig;
    
    /**
     * Creates a new instance with the given configuration.
     * 
     * @param config	The configuration used by this SMP Client 
     */
    SMPResultReader(final SMPClientConfig config) {
    	this.clientConfig = config;
	}
    
    /**
     * Processes the SMP response and converts it into the object representation.
     *
     * @param is    The input stream that contains the SMP response
     * @return      A {@link ISMPQueryResult} instance that represent the response received from the SMP server
     * @throws SMPQueryException    When the response of SMP server could not be processed.
     * @throws IOException          When the input stream could not be completely processed.
     */
    public ISMPQueryResult handleResponse(final InputStream is) throws SMPQueryException, IOException {
        Document xmlResult;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            log.debug("Parsing the SMP response");
            xmlResult = db.parse(is);
            log.debug("Successfully parsed the SMP response into XML document");
        } catch (ParserConfigurationException | SAXException parsingError) {
            log.error("Could not parse the XML returned by the SMP server! Details: {}" + parsingError.getMessage());
            throw new SMPQueryException("Invalid response from SMP server!", parsingError);
        }

        final Element rootElement = xmlResult.getDocumentElement();
        final Element signature = getFirstSignature(rootElement);
        X509Certificate signingCert = null;
        if (signature != null) 
            signingCert = verifySignature(xmlResult, signature);

        // Get the name space of the root element to determine the correct result processor
        final String resultNamespace = rootElement.getNamespaceURI();
        log.debug("Finding processor for namespace URI of SMP response: {}", resultNamespace);
        ISMPResultProcessor processor = findResultProcessor(resultNamespace);
        if (processor == null) {
            log.error("Could not find a result processor for SMP response with namespace {}", resultNamespace);
            throw new SMPQueryException("Unknown XML document received from SMP server!");
        }
        log.debug("Using {} processor to convert XML into object representation", processor.getClass().getName());
        ISMPQueryResult objResult = processor.processResult(xmlResult);
        log.debug("Response meta-data successfully converted into object model representation");
        // Process signature of meta-data if available
        if (signingCert != null) {
            log.debug("Response was signed, add signature meta-data to the result meta-data");            
            // If the response was signed it must have been a ServiceMetadata
            ((ServiceMetadataResult) objResult).setSignerCertificate(signingCert);            
        }
        log.debug("Successfully converted the XML format to object model");
        return objResult;
    }

    /**
     * Finds the {@link ISMPResultProcessor} that should transform the received XML into object representation.
     *  
     * @param namespace		URI of the namespace of the response
     * @return	the result processor that will handle the response if one is registered, <code>null</code> otherwise
     */
    private ISMPResultProcessor findResultProcessor(String namespace) {
    	Optional<ISMPResultProcessor> processorSrch = clientConfig.getProcessors().parallelStream()
    																.filter(p -> p.canProcess(namespace)).findFirst();
		return processorSrch.isPresent() ? processorSrch.get() : null;
	}

	/**
     * Gets the first <code>ds:Signature</code> child element of the given element.
     * 
     * @param el	element that should contain the signature
     * @return		the first <code>ds:Signature</code> element if it exists, <code>null</code> otherwise
     */
    private Element getFirstSignature(final Element el) {
    	final NodeList children = el.getChildNodes();
    	Element sig = null;
    	
    	for(int i = 0; i < children.getLength() && sig == null; i++) {
    		final Node child = children.item(i);
    		if (child instanceof Element 
    				&& XMLDSIG_NS.equals(child.getNamespaceURI()) && XMLDSIG_SIGNATURE.equals(child.getLocalName()))
    			sig = (Element) child;
    	}

    	return sig;
    }
    
    /**
     * Validates the signature that was placed on the SMP result.
     * 
     * @param xmlResultDoc			The complete XML result document 
     * @param signatureElement		The XML element containing the signature to be validated
     * @return	The X509 Certificate used for signing the SMP result.
     * @throws SMPQueryException 	When the signature could not be validated
     */
    private X509Certificate verifySignature(final Document xmlResultDoc, final Element signatureElement) 
    																					throws SMPQueryException {
        try {
            CertificateKeySelector keySelector = new CertificateKeySelector();
            log.debug("Preparing context for signature verification");
            DOMValidateContext valContext = new DOMValidateContext(keySelector, signatureElement);
            XMLSignatureFactory xmlSignatureFactory = XMLSignatureFactory.getInstance("DOM");
            XMLSignature signature = xmlSignatureFactory.unmarshalXMLSignature(valContext);
            log.debug("Verifying the signature...");
            if (!signature.validate(valContext)) {
            	log.error("The signature on the SMP result could not be verified!");
                throw new SMPQueryException("The signature on the SMP result could not be verified!");
            }
            final X509Certificate signingCert = keySelector.getCertificate();
            log.debug("Signature succesfully verified");
            final ITrustValidator trustValidator = clientConfig.getTrustValidator();
            if (trustValidator != null) {
            	log.debug("Validating trust in certficate using validator {}", trustValidator.getClass().getName());
            	if (!trustValidator.isTrusted(signingCert)) {
            		log.warn("SMP Certificate is not trusted! Cert info:\n\tSubject         : {}\n\tIssuer/serialNo : {}/{}",
            				 signingCert.getSubjectDN().getName(), signingCert.getIssuerX500Principal().getName(), 
            				 signingCert.getSerialNumber().toString());
            		throw new SMPQueryException("SMP Certificate used for signing is not trusted");
            	}
            	log.debug("SMP Certificate is trusted. Cert info:\n\tSubject         : {}\n\tIssuer/serialNo : {}/{}",
						 signingCert.getSubjectDN().getName(), signingCert.getIssuerX500Principal().getName(), 
						 signingCert.getSerialNumber().toString());
            } else
            	log.debug("Trust validation disabled");
            return signingCert;
        } catch (XMLSignatureException | MarshalException verificationFailed) {
            log.error("An error occurred during signature verification!\n\tDetails: {}", 
            			Utils.getExceptionTrace(verificationFailed));
            throw new SMPQueryException("Unable to verify signature.", verificationFailed);
        }        
    }
    
    /**
     * Is an implementation of the abstract {@link KeySelector} to retrieve the X509 Certificate that contains the 
     * public key to use for the verification of the signature. This key selector use the configured {@link 
     * ICertificateFinder} to get the actual certificate. 
     * 
     * @author Sander Fieten (sander at holodeck-b2b.org)
     */
    class CertificateKeySelector extends KeySelector {
    	/**
    	 * The Certificate from which the key was retrieved
    	 */
        private X509Certificate certificate;

        /**
         * Attempts to get the public key for verification of the signature using the configured {@link 
         * ICertificateFinder}.
         * 
         * {@inheritDoc}
         */
        @Override
		public KeySelectorResult select(final KeyInfo keyInfo, final KeySelector.Purpose purpose, 
        								final AlgorithmMethod method, final XMLCryptoContext context) 
    																					throws KeySelectorException {
        	this.certificate = clientConfig.getCertificateFinder().findCertificate(keyInfo, method, context);
        	if (this.certificate == null)
        		throw new KeySelectorException("No (usable) Certificate found!");
        	else 
                return new KeySelectorResult() {    
        											@Override
        											public Key getKey() {
    								                            return certificate.getPublicKey();    								                        
    								                };
                    							};
        }

        /**
         * Gets the X509 Certificate that was retrieved from the signature and that used to verify it.
         * 
         * @return	The Certificate used for signature verification.
         */
        public X509Certificate getCertificate() {
            return certificate;
        }
    }
}
