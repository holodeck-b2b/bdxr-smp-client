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
package org.holodeckb2b.bdxr.impl.oasis_smp2;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.holodeckb2b.bdxr.smp.api.ISMPResultProcessor;
import org.holodeckb2b.bdxr.smp.api.SMPQueryException;
import org.holodeckb2b.bdxr.smp.datamodel.Certificate;
import org.holodeckb2b.bdxr.smp.datamodel.EndpointInfo;
import org.holodeckb2b.bdxr.smp.datamodel.IExtension;
import org.holodeckb2b.bdxr.smp.datamodel.ISMPQueryResult;
import org.holodeckb2b.bdxr.smp.datamodel.Identifier;
import org.holodeckb2b.bdxr.smp.datamodel.ProcessIdentifier;
import org.holodeckb2b.bdxr.smp.datamodel.ProcessInfo;
import org.holodeckb2b.bdxr.smp.datamodel.ProcessList;
import org.holodeckb2b.bdxr.smp.datamodel.Redirection;
import org.holodeckb2b.bdxr.smp.datamodel.ServiceInformation;
import org.holodeckb2b.commons.security.CertificateUtils;
import org.holodeckb2b.commons.util.Utils;
import org.oasis_open.docs.bdxr.ns.smp._2.aggregatecomponents.CertificateType;
import org.oasis_open.docs.bdxr.ns.smp._2.aggregatecomponents.EndpointType;
import org.oasis_open.docs.bdxr.ns.smp._2.aggregatecomponents.ProcessMetadataType;
import org.oasis_open.docs.bdxr.ns.smp._2.aggregatecomponents.ProcessType;
import org.oasis_open.docs.bdxr.ns.smp._2.aggregatecomponents.RedirectType;
import org.oasis_open.docs.bdxr.ns.smp._2.basiccomponents.RoleIDType;
import org.oasis_open.docs.bdxr.ns.smp._2.basiccomponents.TypeCodeType;
import org.oasis_open.docs.bdxr.ns.smp._2.extensioncomponents.SMPExtensionsType;
import org.oasis_open.docs.bdxr.ns.smp._2.servicegroup.ServiceGroupType;
import org.oasis_open.docs.bdxr.ns.smp._2.servicemetadata.ServiceMetadataType;
import org.w3c.dom.Document;

/**
 * Is the {@link ISMPResultProcessor} implementation that handles the XML format as defined in the OASIS SMP 
 * version 2.0 specification.    
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  2.0.0
 */
public class OASISv2ResultProcessor implements ISMPResultProcessor {
    private static final Logger	log = LogManager.getLogger(OASISv2ResultProcessor.class);

    /**
     * The JAXB context for the conversion of XML into Java objects
     */
    private static final JAXBContext jaxbContext;
    static {
        try {
            // Initialize the JAXB Context used in processing of the PEPPOL responses
            jaxbContext = JAXBContext.newInstance(ServiceGroupType.class, ServiceMetadataType.class);
        } catch (JAXBException jaxbFailure) {
            log.fatal("Could not load the JAXB Context required for processing SMP response! Details: {}",
                      jaxbFailure.getMessage());
            throw new RuntimeException(jaxbFailure.getMessage(), jaxbFailure);
        }
    }

    /**
     * The special process identifier used to indicate that the document id is not assigned to a specific process. 
     */
    private static final String NO_PROCESS_ID = "bdx:noprocess";

    /**
     * The namespace URI of the SMP XML <i>ServiceMetadata</i> document as specified in the OASIS SMP V2 specification
     */
    public static final String SVC_METADATA_NS_URI = "http://docs.oasis-open.org/bdxr/ns/SMP/2/ServiceMetadata";
    /**
     * The namespace URI of the SMP XML <i>ServiceGroup</i> document as specified in the OASIS SMP V2 specification
     */
    public static final String SVC_GROUP_NS_URI = "http://docs.oasis-open.org/bdxr/ns/SMP/2/ServiceGroup";
        
    @Override
    public boolean canProcess(final String namespaceURI) {
    	return SVC_METADATA_NS_URI.equals(namespaceURI) || SVC_GROUP_NS_URI.equals(namespaceURI);
    }

    @Override	
    public ISMPQueryResult processResult(Document xmlDocument) throws SMPQueryException {
        JAXBElement jaxbDoc;
        try {
            log.debug("Convert the XML into Java objects");
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            jaxbDoc = (JAXBElement) unmarshaller.unmarshal(xmlDocument);
            log.debug("XML converted into Java objects");
        } catch (JAXBException parsingError) {
            log.error("Could not convert the XML document into Java objects! Details: {}", parsingError.getMessage());
            throw new SMPQueryException("XML could not be parsed as OASIS SMP2 result");
        }

        // Now check the root element of the response and process accordingly
        final Class rootType = jaxbDoc.getDeclaredType();
        if (ServiceGroupType.class.equals(rootType)) {
            log.debug("Document is a Service Group response");
            //@todo: Handle Service Group responses
            return null;
        } else {
            log.debug("Document is a Service Metadata response");
            ServiceMetadataType svcMetadata = (ServiceMetadataType) jaxbDoc.getValue();
            final ServiceInformation svcInfo = new ServiceInformation();
            svcInfo.setParticipantId(new Identifier(svcMetadata.getParticipantID().getValue(),
                                                    svcMetadata.getParticipantID().getSchemeID()));
            svcInfo.setServiceId(new Identifier(svcMetadata.getID().getValue(), svcMetadata.getID().getSchemeID()));
            // Convert the list of ProcessMetadata element
            for(ProcessMetadataType p : svcMetadata.getProcessMetadata())
                svcInfo.addProcessInformation(processProcessMetadata(p));

            svcInfo.setExtensions(handleServiceMetadataExtensions(svcMetadata.getSMPExtensions()));
            
            log.debug("Completely processed the response document");
            return svcInfo;
        }
    }
    
	/**
	 * Converts the information contained in the <code>ProcessMetadata</code> element into a <code>ProcessList</code> 
	 * instance.
	 *
	 * @param procMetadataXML The JAXB representation of the <code>ProcessMetadata</code> element
	 * @return The <code>ProcessList</code> representation of the meta-data
	 * @throws SMPQueryException When there is a problem in converting the Process XML into the object model, for 
	 * 							 example when it includes both <code>Endpoint</code> and <code>Redirect</code> elements
	 */
	private ProcessList processProcessMetadata(ProcessMetadataType procMetadataXML) throws SMPQueryException {
		final ProcessList procList = new ProcessList();
		
		for (ProcessType pi : procMetadataXML.getProcess()) {
			final ProcessInfo procInfo = new ProcessInfo();
			final String procID = pi.getID().getValue();
			if (NO_PROCESS_ID.equals(procID)) 
				procInfo.setProcessId(new ProcessIdentifier());
			else
				procInfo.setProcessId(new ProcessIdentifier(procID, pi.getID().getSchemeID()));
			for(RoleIDType r : pi.getRoleID()) 
				procInfo.addRole(new Identifier(r.getValue(), r.getSchemeID()));	
			procInfo.setExtensions(handleProcessInfoExtensions(pi.getSMPExtensions()));
			procList.addProcessInfo(procInfo);
		}
		
		final List<EndpointType> endpoints = procMetadataXML.getEndpoint();
		final RedirectType redirection = procMetadataXML.getRedirect();
		if (!Utils.isNullOrEmpty(endpoints) && redirection != null) 
			// There cannot be endpoints and a redirection at the same time!
			throw new SMPQueryException("Invalid meta-data received (both endpoint and redirect specified)");
		
		// Convert the Endpoint elements into object model
		for (EndpointType ep : endpoints)
			procList.addEndpoint(processEndpoint(ep));

		procList.setRedirection(processRedirection(redirection));		
		procList.setExtensions(handleProcessMetadataExtensions(procMetadataXML.getSMPExtensions()));

		return procList;
	}
    
	/**
	 * Converts the information contained in an <code>Endpoint</code> element into an <code>EndpointInfo</code> 
	 * instance.
	 *
	 * @param  epInfoXML The JAXB representation of the <code>Endpoint</code> element
	 * @return The <code>EndpointInfo</code> representation of the meta-data
	 * @throws SMPQueryException When the XML certificate string could not be
	 *                           converted into object representation
	 */
	private EndpointInfo processEndpoint(EndpointType epInfoXML) throws SMPQueryException {
		final EndpointInfo epInfo = new EndpointInfo();

		final String tpID = epInfoXML.getTransportProfileID() != null ? epInfoXML.getTransportProfileID().getValue() 
																		: null;
		if (Utils.isNullOrEmpty(tpID)) {
			log.error("Missing transport profile identifier");
			throw new SMPQueryException("Invalid endpoint meta-data");			
		}
		epInfo.setTransportProfile(tpID);
		
		final String epURL = epInfoXML.getAddressURI() != null ? epInfoXML.getAddressURI().getValue() : null;
		try {
			epInfo.setEndpointURL(new URL(epURL));
		} catch (MalformedURLException invalidURL) {
			log.error("Invalid endpoint URL specified: {}", epURL);
			throw new SMPQueryException("Invalid endpoint meta-data");			
		}
		
		final XMLGregorianCalendar activation = epInfoXML.getActivationDate() != null ? 
																	 epInfoXML.getActivationDate().getValue() : null;
		if (activation != null)
			epInfo.setServiceActivationDate(activation.toGregorianCalendar().toZonedDateTime());
		final XMLGregorianCalendar expiration = epInfoXML.getExpirationDate() != null ? 
																	 epInfoXML.getExpirationDate().getValue() : null;
		if (expiration != null)
			epInfo.setServiceExpirationDate(expiration.toGregorianCalendar().toZonedDateTime());

		epInfo.setDescription(epInfoXML.getDescription() != null ? epInfoXML.getDescription().getValue() : null);
		epInfo.setContactInfo(epInfoXML.getContact() != null ? epInfoXML.getContact().getValue() : null);

		try {
			for(CertificateType c : epInfoXML.getCertificate())
				epInfo.addCertificate(processCertificateInfo(c));
		} catch (CertificateException certReadError) {
			log.error("Could not read the Certificates from the SMP response! Details: {}", certReadError.getMessage());
			throw new SMPQueryException("Invalid endpoint meta-data");
		}

		epInfo.setExtensions(handleEndpointInfoExtensions(epInfoXML.getSMPExtensions()));

		return epInfo;
	}

	/**
     * Processes the <i>service meta-data</i> included in the <code>ServiceInformation</code> element of the response.
     *
     * @param redirectXML   The JAXB representation of the <code>Redirect</code> element of the response
     * @return  A {@link Redirection} instance with the information on the redirection
     * @throws SMPQueryException When there is a problem in converting the XML into the object model
     */
    private Redirection processRedirection(RedirectType redirectXML) throws SMPQueryException {
		URL redirectURL = null;
		try {
			redirectURL = redirectXML.getPublisherURI() != null ? new URL(redirectXML.getPublisherURI().getValue()) 
																: null;
		} catch (MalformedURLException invalidURL) {
			log.error("Invalid value for redirect URL: {}", redirectXML.getPublisherURI().getValue()); 
		}
		if (redirectURL == null)
			throw new SMPQueryException("Invalid redirection response received!");
		
		final Redirection redirectionInfo = new Redirection(redirectURL);
		if (!Utils.isNullOrEmpty(redirectXML.getCertificate())) 
			try {
				redirectionInfo.setSMPCertitificate(processCertificateInfo(redirectXML.getCertificate().get(0))
																			.getX509Cert());
			} catch (CertificateException invalidCert) {
				log.error("Invalid certificate included in Redirection to {}", redirectURL.toString());
				throw new SMPQueryException("Invalid redirection response received!");
			}
		
        redirectionInfo.setExtensions(handleRedirectionExtensions(redirectXML.getSMPExtensions()));
		return redirectionInfo;
    }   
    
    /**
     * Processes the <i>certificate meta-data</i> that can be included in both the <code>Endpoint</code> and <code>
     * Redirect</code> elements.
     * 
     * @param certInfo	The <code>Certificate</code> XML element
     * @return	The {@link Certificate} representation
     * @throws CertificateException	when the base64 encoded bytes are not a X509 Certificate
     */
    private Certificate processCertificateInfo(final CertificateType certInfo) throws CertificateException {
    	final TypeCodeType usage = certInfo.getTypeCode();		
    	final Certificate cert = new Certificate(CertificateUtils.getCertificate(
    																	certInfo.getContentBinaryObject().getValue()),
												 usage != null ? new String[] { usage.getValue() } : new String[] {});
		cert.setDescription(certInfo.getDescription() != null ? certInfo.getDescription().getValue() : null);
		final XMLGregorianCalendar activation = certInfo.getActivationDate() != null ? 
																		certInfo.getActivationDate().getValue() : null;    																			
		cert.setActivationDate(activation != null ? activation.toGregorianCalendar().toZonedDateTime() : null);
		final XMLGregorianCalendar expiration = certInfo.getExpirationDate() != null ? 
				certInfo.getExpirationDate().getValue() : null;    																			
		cert.setEpirationDate(expiration != null ? expiration.toGregorianCalendar().toZonedDateTime() : null);
    
		return cert;
    }
    
    /**
     * Converts the <code>SMPExtensions</code> child element of the <code>Redirection</code> element into the object 
     * representation.
     * <p><b>NOTE: </b> This default implementation <b>ignores</b> all included extensions. If a network uses 
     * extension you should create a descendant class and override this method to correctly handle the network 
     * specific extensions.
     * 
	 * @param extensions	The extension included with the <code>Redirection</code> element
	 * @return				The object representation of the extensions
	 */
	protected List<IExtension> handleRedirectionExtensions(SMPExtensionsType extensions) {
		return null;
	}

    /**
     * Converts the <code>SMPExtensions</code> child element of the <code>ServiceMetadata</code> element into the 
     * object representation.
     * <p><b>NOTE: </b> This default implementation <b>ignores</b> all included extensions. If a network uses 
     * extension you should create a descendant class and override this method to correctly handle the network 
     * specific extensions.
     * 
	 * @param extensions	The extension included with the <code>ServiceMetadata</code> element
	 * @return				The object representation of the extensions
	 */
	protected List<IExtension> handleServiceMetadataExtensions(SMPExtensionsType extensions) {
		return null;
	}

    /**
     * Converts the <code>SMPExtensions</code> child element of the <code>Process</code> element into the object 
     * representation.
     * <p><b>NOTE: </b> This default implementation <b>ignores</b> all included extensions. If a network uses 
     * extension you should create a descendant class and override this method to correctly handle the network 
     * specific extensions.
     * 
	 * @param extensions	The extension included with the <code>Process</code> element
	 * @return				The object representation of the extensions
	 */
	protected List<IExtension> handleProcessInfoExtensions(SMPExtensionsType extensions) {
		return null;
	}
	
	/**
	 * Converts the <code>SMPExtensions</code> child element of the <code>ProcessMetadata</code> element into the object 
	 * representation.
	 * <p><b>NOTE: </b> This default implementation <b>ignores</b> all included extensions. If a network uses 
	 * extension you should create a descendant class and override this method to correctly handle the network 
	 * specific extensions.
	 * 
	 * @param extensions	The extension included with the <code>ProcessMetadata</code> element
	 * @return				The object representation of the extensions
	 */
	protected List<IExtension> handleProcessMetadataExtensions(SMPExtensionsType extensions) {
		return null;
	}
	
    /**
     * Converts the <code>SMPExtensions</code> child element of the <code>Endpoint</code> element into the object 
     * representation.
     * <p><b>NOTE: </b> This default implementation <b>ignores</b> all included extensions. If a network uses 
     * extension you should create a descendant class and override this method to correctly handle the network 
     * specific extensions.
     * 
	 * @param extensions	The extension included with the <code>Endpoint</code> element
	 * @return				The object representation of the extensions
	 */
	protected List<IExtension> handleEndpointInfoExtensions(SMPExtensionsType extensions) {
		return null;
	}    
}
