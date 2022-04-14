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
package org.holodeckb2b.bdxr.smp.client.impl.peppol;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.busdox.servicemetadata.publishing._1.EndpointType;
import org.busdox.servicemetadata.publishing._1.ExtensionType;
import org.busdox.servicemetadata.publishing._1.ProcessType;
import org.busdox.servicemetadata.publishing._1.RedirectType;
import org.busdox.servicemetadata.publishing._1.ServiceGroupType;
import org.busdox.servicemetadata.publishing._1.ServiceInformationType;
import org.busdox.servicemetadata.publishing._1.ServiceMetadataReferenceType;
import org.busdox.servicemetadata.publishing._1.ServiceMetadataType;
import org.busdox.servicemetadata.publishing._1.SignedServiceMetadataType;
import org.holodeckb2b.bdxr.smp.client.api.ISMPResultProcessor;
import org.holodeckb2b.bdxr.smp.client.api.SMPQueryException;
import org.holodeckb2b.bdxr.smp.datamodel.EndpointInfo;
import org.holodeckb2b.bdxr.smp.datamodel.Extension;
import org.holodeckb2b.bdxr.smp.datamodel.ProcessGroup;
import org.holodeckb2b.bdxr.smp.datamodel.QueryResult;
import org.holodeckb2b.bdxr.smp.datamodel.Redirection;
import org.holodeckb2b.bdxr.smp.datamodel.ServiceMetadata;
import org.holodeckb2b.bdxr.smp.datamodel.SignedQueryResult;
import org.holodeckb2b.bdxr.smp.datamodel.impl.CertificateImpl;
import org.holodeckb2b.bdxr.smp.datamodel.impl.EndpointInfoV1Impl;
import org.holodeckb2b.bdxr.smp.datamodel.impl.IdentifierImpl;
import org.holodeckb2b.bdxr.smp.datamodel.impl.ProcessGroupImpl;
import org.holodeckb2b.bdxr.smp.datamodel.impl.ProcessIdentifierImpl;
import org.holodeckb2b.bdxr.smp.datamodel.impl.ProcessInfoImpl;
import org.holodeckb2b.bdxr.smp.datamodel.impl.RedirectionV1Impl;
import org.holodeckb2b.bdxr.smp.datamodel.impl.ServiceGroupV1Impl;
import org.holodeckb2b.bdxr.smp.datamodel.impl.ServiceMetadataImpl;
import org.holodeckb2b.bdxr.smp.datamodel.impl.SignedServiceMetadataImpl;
import org.holodeckb2b.commons.security.CertificateUtils;
import org.holodeckb2b.commons.util.Utils;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Is the {@link ISMPResultProcessor} implementation that handles the XML format as defined in the PEPPOL SMP
 * specification.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class PEPPOLResultProcessor implements ISMPResultProcessor {
    private static final Logger	log = LogManager.getLogger(PEPPOLResultProcessor.class);

    /**
     * The JAXB context for the conversion of XML into Java objects
     */
    private static final JAXBContext jaxbContext;
	/**
	 * The XML schema to validate the SMP responses
	 */
	private static final Schema		smpSchema;

    static {
        try {
            // Initialize the JAXB Context used in processing of the PEPPOL responses
            jaxbContext = JAXBContext.newInstance(ServiceGroupType.class, SignedServiceMetadataType.class,
                                                  ServiceMetadataType.class);
			// Read the XSD for validation of responses
			SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			smpSchema = sf.newSchema(PEPPOLResultProcessor.class.getResource("/xsd/peppol-smp-1.0.xsd"));
        } catch (SAXException | JAXBException jaxbFailure) {
            log.fatal("Could not prepare the JAXB Context required for processing SMP response! Details: {}",
                      jaxbFailure.getMessage());
            throw new RuntimeException(jaxbFailure.getMessage(), jaxbFailure);
        }
    }

    /**
     * The special process identifier used to indicate that the document id is not assigned to a specific process.
     */
    private static final String NO_PROCESS_ID = "busdox:noprocess";

    /**
     * The namespace URI of SMP XML result documents as specified in the PEPPOL SMP specification
     */
    public static final String NAMESPACE_URI = "http://busdox.org/serviceMetadata/publishing/1.0/";

    @Override
    public boolean canProcess(final String namespaceURI) {
    	return NAMESPACE_URI.equals(namespaceURI);
    }

    @Override
    public QueryResult processResult(Document xmlDocument) throws SMPQueryException {
        JAXBElement jaxbDoc;
        try {
            log.debug("Convert the XML into Java objects");
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
			unmarshaller.setSchema(smpSchema);
            jaxbDoc = (JAXBElement) unmarshaller.unmarshal(xmlDocument);
            log.debug("XML converted into Java objects");
        } catch (JAXBException parsingError) {
            log.error("Could not convert the XML document into Java objects! Details: {}", parsingError.getMessage());
            throw new SMPQueryException("XML could not be parsed as a valid PEPPOL SMP result");
        }

		try {
			// Now check the root element of the response and process accordingly
			final Class rootType = jaxbDoc.getDeclaredType();
			if (ServiceGroupType.class.equals(rootType))
				return processServiceGroup((ServiceGroupType) jaxbDoc.getValue());
			else
				return processServiceMetadata(SignedServiceMetadataType.class.equals(rootType) ?
												((SignedServiceMetadataType) jaxbDoc.getValue()).getServiceMetadata() :
												(ServiceMetadataType) jaxbDoc.getValue());
		} catch (IllegalArgumentException iae) {
			log.error("Response contains an invalid value for some meta-data! Details: {}", Utils.getExceptionTrace(iae));
			throw new SMPQueryException("Response contains an invalid value");
		}
    }

	@Override
	public SignedQueryResult processResult(Document xmlDocument, X509Certificate signingCert) throws SMPQueryException {
		final QueryResult queryResult = processResult(xmlDocument);
		if (queryResult instanceof ServiceMetadata)
			return new SignedServiceMetadataImpl((ServiceMetadata) queryResult, signingCert);
		else
			throw new SMPQueryException("Signed ServiceGroup is not supported in OASIS V1 SMP specification!");
	}

	private ServiceMetadata processServiceMetadata(ServiceMetadataType smdXML) throws SMPQueryException {
		log.debug("Process ServiceMetadata result document");

		ServiceMetadataImpl smd = new ServiceMetadataImpl();
		if (smdXML.getRedirect() != null) {
			/* Because redirections can be done on process level in the OASIS V2 spec it is included in the ProcessGroup
			 * data element. In PEPPOL however the redirection is on the service level. This implies that we need to
			 * package it here in a ProcessGroup without process info
			 */
			ProcessGroupImpl pg = new ProcessGroupImpl();
			log.trace("Service Metadata contains a Redirect");
			pg.setRedirection(convertRedirection(smdXML.getRedirect()));
			smd.addProcessGroup(pg);
		} else {
			log.trace("Service Metadata contains ServiceInformation");
			ServiceInformationType siXML = smdXML.getServiceInformation();
			smd.setParticipantId(new IdentifierImpl(siXML.getParticipantIdentifier().getValue(),
													siXML.getParticipantIdentifier().getScheme()));
			smd.setServiceId(new IdentifierImpl(siXML.getDocumentIdentifier().getValue(),
												siXML.getDocumentIdentifier().getScheme()));
			/* Convert the list of ProcessList/Process elements. Because in the PEPPOL spec each process in which the
			 * service/document is used has its own list of endpoint it must be added as a ProcessGroup
			 */
			for(ProcessType p : siXML.getProcessList().getProcess())
				smd.addProcessGroup(convertProcessMetadata(p));

			smd.setExtensions(handleServiceInfoExtensions(siXML.getExtension()));
		}

		log.debug("Completely processed the response document");
		return smd;
	}

    private Redirection convertRedirection(RedirectType redirectXML) throws SMPQueryException {
    	try {
    		final RedirectionV1Impl redirection = new RedirectionV1Impl(new URL(redirectXML.getHref()));
            redirection.setExtensions(handleRedirectionExtensions(redirectXML.getExtension()));
    		return redirection;
    	} catch (NullPointerException | MalformedURLException invalidURL) {
    		log.error("The Redirection response includes an invalid new target URL: {}", redirectXML.getHref());
    		throw new SMPQueryException("Invalid redirection response received!");
    	}
    }

    /**
     * Converts the <code>Extension</code> child elements of the <code>Redirection</code> element into the object
     * representation.
     * <p><b>NOTE: </b> This default implementation <b>ignores</b> all included extensions. If a network uses
     * extension you should create a descendant class and override this method to correctly handle the network
     * specific extensions.
     *
	 * @param extensions	The extension included with the <code>Redirection</code> element
	 * @return				The object representation of the extensions
	 */
	protected List<Extension> handleRedirectionExtensions(ExtensionType extensions) {
		return null;
	}

    /**
     * Converts the <code>Extension</code> child elements of the <code>ServiceInformation</code> element into the
     * object representation.
     * <p><b>NOTE: </b> This default implementation <b>ignores</b> all included extensions. If a network uses
     * extension you should create a descendant class and override this method to correctly handle the network
     * specific extensions.
     *
	 * @param extensions	The extension included with the <code>ServiceInformation</code> element
	 * @return				The object representation of the extensions
	 */
	protected List<Extension> handleServiceInfoExtensions(ExtensionType extensions) {
		return null;
	}

    private ProcessGroup convertProcessMetadata(ProcessType procInfoXML) throws SMPQueryException {
    	final ProcessGroupImpl pg = new ProcessGroupImpl();
    	final ProcessInfoImpl procInfo = new ProcessInfoImpl();

        final String procID = procInfoXML.getProcessIdentifier().getValue();
        if (NO_PROCESS_ID.equals(procID))
        	procInfo.setProcessId(new ProcessIdentifierImpl());
        else
        	procInfo.setProcessId(new ProcessIdentifierImpl(procID, procInfoXML.getProcessIdentifier().getScheme()));
        pg.addProcessInfo(procInfo);

        // Convert the Endpoint elements into object model
        for(EndpointType ep : procInfoXML.getServiceEndpointList().getEndpoint())
            pg.addEndpoint(convertEndpoint(ep));

        pg.setExtensions(handleProcessInfoExtensions(procInfoXML.getExtension()));

        return pg;
    }

    /**
     * Converts the <code>Extension</code> child elements of the <code>Process</code> element into the object
     * representation.
     * <p><b>NOTE: </b> This default implementation <b>ignores</b> all included extensions. If a network uses
     * extension you should create a descendant class and override this method to correctly handle the network
     * specific extensions.
     *
	 * @param extensions	The extension included with the <code>Process</code> element
	 * @return				The object representation of the extensions
	 */
	protected List<Extension> handleProcessInfoExtensions(ExtensionType extensions) {
		return null;
	}

    private EndpointInfo convertEndpoint(EndpointType epInfoXML) throws SMPQueryException {
        final EndpointInfoV1Impl epInfo = new EndpointInfoV1Impl();

		String profile = epInfoXML.getTransportProfile();
		Utils.requireNotNullOrEmpty(profile);
        epInfo.setTransportProfile(profile);
        try {
			epInfo.setEndpointURL(new URL(epInfoXML.getEndpointReference().getAddress().getValue()));
		} catch (MalformedURLException e) {
			log.error("Invalid URL specified for endpoint! Value={}",
						epInfoXML.getEndpointReference().getAddress().getValue());
			throw new SMPQueryException("Invalid endpoint meta-data");
		}
        epInfo.setBusinessLevelSignatureRequired(epInfoXML.isRequireBusinessLevelSignature());
        epInfo.setMinimumAuthenticationLevel(epInfoXML.getMinimumAuthenticationLevel());
        final XMLGregorianCalendar svcActivationDate = epInfoXML.getServiceActivationDate();
        if (svcActivationDate != null)
            epInfo.setServiceActivationDate(svcActivationDate.toGregorianCalendar().toZonedDateTime());
        final XMLGregorianCalendar svcExpirationDate = epInfoXML.getServiceExpirationDate();
        if (svcExpirationDate != null)
            epInfo.setServiceExpirationDate(svcExpirationDate.toGregorianCalendar().toZonedDateTime());
        try {
			X509Certificate epCert = CertificateUtils.getCertificate(epInfoXML.getCertificate());
            if (epCert != null)
				epInfo.addCertificate(new CertificateImpl(epCert));
        } catch (CertificateException certReadError) {
            log.error("Could not read the Certificate from the SMP response! Details: {}", certReadError.getMessage());
            throw new SMPQueryException("Could not read the Certificate from the SMP response");
        }
        epInfo.setDescription(epInfoXML.getServiceDescription());
        epInfo.setContactInfo(epInfoXML.getTechnicalContactUrl());
		String techInfoURL = epInfoXML.getTechnicalInformationUrl();
		if (!Utils.isNullOrEmpty(techInfoURL))
			try {
				epInfo.setTechnicalInformationURL(new URL(techInfoURL));
			} catch (MalformedURLException ex) {
				log.error("Invalid URL specified for technical information! Value={}",
							epInfoXML.getTechnicalContactUrl());
				throw new SMPQueryException("Invalid endpoint meta-data");
			}
        epInfo.setExtensions(handleEndpointInfoExtensions(epInfoXML.getExtension()));

        return epInfo;
    }

    /**
     * Converts the <code>Extension</code> child elements of the <code>Endpoint</code> element into the object
     * representation.
     * <p><b>NOTE: </b> This default implementation <b>ignores</b> all included extensions. If a network uses
     * extension you should create a descendant class and override this method to correctly handle the network
     * specific extensions.
     *
	 * @param extensions	The extension included with the <code>Endpoint</code> element
	 * @return				The object representation of the extensions
	 */
	protected List<Extension> handleEndpointInfoExtensions(ExtensionType extensions) {
		return null;
	}

	private QueryResult processServiceGroup(ServiceGroupType svcGrpXML) throws SMPQueryException {
		ServiceGroupV1Impl sg = new ServiceGroupV1Impl();

		log.debug("Process ServiceGroup result document");
		sg.setParticipantId(new IdentifierImpl(svcGrpXML.getParticipantIdentifier().getValue(),
											   svcGrpXML.getParticipantIdentifier().getScheme()));

		for(ServiceMetadataReferenceType r :
									svcGrpXML.getServiceMetadataReferenceCollection().getServiceMetadataReference()) {
			try {
				sg.addServiceReference(new URL(r.getHref()));
			} catch (MalformedURLException ex) {
				log.error("ServiceGroup contains an invalid reference URL ({})!", r.getHref());
				throw new SMPQueryException("Invalid SMP response!");
			}
		}
		sg.setExtensions(handleServiceGroupExtensions(svcGrpXML.getExtension()));
		log.debug("Completely processed ServiceGroup result document");
		return sg;
	}

    /**
     * Converts the <code>Extension</code> child elements of the <code>ServiceGroup</code> element into the object
     * representation.
     * <p><b>NOTE: </b> This default implementation <b>ignores</b> all included extensions. If a network uses
     * extension you should create a descendant class and override this method to correctly handle the network
     * specific extensions.
     *
	 * @param extensions	The extension included with the <code>ServiceGroup</code> element
	 * @return				The object representation of the extensions
	 */
	private List<Extension> handleServiceGroupExtensions(ExtensionType extension) {
		return null;
	}
}
