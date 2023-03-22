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
package org.holodeckb2b.bdxr.smp.client.impl.oasis_smp2;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.holodeckb2b.bdxr.smp.client.api.SMPQueryException;
import org.holodeckb2b.bdxr.smp.datamodel.Certificate;
import org.holodeckb2b.bdxr.smp.datamodel.EndpointInfo;
import org.holodeckb2b.bdxr.smp.datamodel.Extension;
import org.holodeckb2b.bdxr.smp.datamodel.ProcessGroup;
import org.holodeckb2b.bdxr.smp.datamodel.QueryResult;
import org.holodeckb2b.bdxr.smp.datamodel.Redirection;
import org.holodeckb2b.bdxr.smp.datamodel.impl.CertificateImpl;
import org.holodeckb2b.bdxr.smp.datamodel.impl.EndpointInfoImpl;
import org.holodeckb2b.bdxr.smp.datamodel.impl.IdentifierImpl;
import org.holodeckb2b.bdxr.smp.datamodel.impl.ProcessGroupImpl;
import org.holodeckb2b.bdxr.smp.datamodel.impl.ProcessIdentifierImpl;
import org.holodeckb2b.bdxr.smp.datamodel.impl.ProcessInfoImpl;
import org.holodeckb2b.bdxr.smp.datamodel.impl.RedirectionV2Impl;
import org.holodeckb2b.bdxr.smp.datamodel.impl.ServiceMetadataImpl;
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
import org.oasis_open.docs.bdxr.ns.smp._2.servicemetadata.ServiceMetadataType;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Handles the processing of the <i>ServiceMetadata</i> XML as defined in the OASIS SMP version 2.0 specification.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  3.0.0
 */
class ServiceMetadataProcessor {
    private static final Logger	log = LogManager.getLogger(ServiceMetadataProcessor.class);

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
            // Initialize the JAXB Context used in processing of the responses
            jaxbContext = JAXBContext.newInstance(ServiceMetadataType.class);
			// Read the XSD for validation of responses
			SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			smpSchema = sf.newSchema(ServiceMetadataProcessor.class.getResource("/xsd/ServiceMetadata-2.0.xsd"));
        } catch (SAXException | JAXBException jaxbFailure) {
            log.fatal("Could not prepare the JAXB Context required for processing SMP response! Details: {}",
                      jaxbFailure.getMessage());
            throw new RuntimeException(jaxbFailure.getMessage(), jaxbFailure);
        }
    }

    QueryResult processServiceMetadata(Document xmlDocument) throws SMPQueryException {
        ServiceMetadataType smdXML;
        try {
            log.trace("Parsing the XML of ServiceMetadata document");
			Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
			unmarshaller.setSchema(smpSchema);
			smdXML = unmarshaller.unmarshal(xmlDocument, ServiceMetadataType.class).getValue();
        } catch (JAXBException parsingError) {
            log.error("Could not convert the XML document into Java objects! Details: {}", parsingError.getMessage());
            throw new SMPQueryException("XML could not be parsed as OASIS SMP2 result");
        }
		try {
			log.trace("Convert XML into object data model representation");
			final ServiceMetadataImpl smd = new ServiceMetadataImpl();
			smd.setParticipantId(new IdentifierImpl(smdXML.getParticipantID().getValue(),
													smdXML.getParticipantID().getSchemeID()));
			smd.setServiceId(new IdentifierImpl(smdXML.getID().getValue(), smdXML.getID().getSchemeID()));
			// Convert the list of ProcessMetadata element
			for(ProcessMetadataType p : smdXML.getProcessMetadata())
				smd.addProcessGroup(convertProcessMetadata(p));

			smd.setExtensions(handleServiceMetadataExtensions(smdXML.getSMPExtensions()));

			log.debug("Completely processed the response document");
			return smd;
		} catch (IllegalArgumentException iae) {
			log.error("Response contains an invalid value for some meta-data! Details: {}", Utils.getExceptionTrace(iae));
			throw new SMPQueryException("Response contains an invalid value");
		}
    }

	private ProcessGroup convertProcessMetadata(ProcessMetadataType procMetadataXML) throws SMPQueryException {
		final ProcessGroupImpl pg = new ProcessGroupImpl();

		for (ProcessType pi : procMetadataXML.getProcess()) {
			final ProcessInfoImpl procInfo = new ProcessInfoImpl();
			final String procID = pi.getID().getValue();
			if (OASISv2ResultProcessor.NO_PROCESS_ID.equals(procID))
				procInfo.setProcessId(new ProcessIdentifierImpl());
			else
				procInfo.setProcessId(new ProcessIdentifierImpl(procID, pi.getID().getSchemeID()));

			for(RoleIDType r : pi.getRoleID())
				procInfo.addRole(new IdentifierImpl(r.getValue(), r.getSchemeID()));
			procInfo.setExtensions(handleProcessInfoExtensions(pi.getSMPExtensions()));
			pg.addProcessInfo(procInfo);
		}

		final List<EndpointType> endpoints = procMetadataXML.getEndpoint();
		final RedirectType redirection = procMetadataXML.getRedirect();
		if (!Utils.isNullOrEmpty(endpoints) && redirection != null) {
			log.error("There cannot be endpoints and a redirection at the same time!");
			throw new SMPQueryException("Invalid meta-data received (both endpoint and redirect specified)");
		}

		// Convert the Endpoint elements into object model
		for (EndpointType ep : endpoints)
			pg.addEndpoint(convertEndpoint(ep));

		pg.setRedirection(convertRedirection(redirection));
		pg.setExtensions(handleProcessMetadataExtensions(procMetadataXML.getSMPExtensions()));

		return pg;
	}

	private EndpointInfo convertEndpoint(EndpointType epInfoXML) throws SMPQueryException {
		final EndpointInfoImpl epInfo = new EndpointInfoImpl();

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
				epInfo.addCertificate(convertCertificateInfo(c));
		} catch (CertificateException certReadError) {
			log.error("Could not read the Certificates from the SMP response! Details: {}", certReadError.getMessage());
			throw new SMPQueryException("Invalid endpoint meta-data");
		}

		epInfo.setExtensions(handleEndpointInfoExtensions(epInfoXML.getSMPExtensions()));
		return epInfo;
	}

	private Redirection convertRedirection(RedirectType redirectXML) throws SMPQueryException {
		if (redirectXML == null)
			return null;

		URL redirectURL = null;
		try {
			redirectURL = redirectXML.getPublisherURI() != null ? new URL(redirectXML.getPublisherURI().getValue())
																: null;
		} catch (MalformedURLException invalidURL) {
			log.error("Invalid value for redirect URL: {}", redirectXML.getPublisherURI().getValue());
		}
		if (redirectURL == null)
			throw new SMPQueryException("Invalid redirection response received!");

		final RedirectionV2Impl redirectionInfo = new RedirectionV2Impl(redirectURL);
		if (!Utils.isNullOrEmpty(redirectXML.getCertificate()))
			try {
				redirectionInfo.setSMPCertitificate(convertCertificateInfo(redirectXML.getCertificate().get(0))
																			.getX509Cert());
			} catch (CertificateException invalidCert) {
				log.error("Invalid certificate included in Redirection to {}", redirectURL.toString());
				throw new SMPQueryException("Invalid redirection response received!");
			}

        redirectionInfo.setExtensions(handleRedirectionExtensions(redirectXML.getSMPExtensions()));
		return redirectionInfo;
    }

    private Certificate convertCertificateInfo(final CertificateType certInfo) throws CertificateException {
    	final TypeCodeType usage = certInfo.getTypeCode();
    	final CertificateImpl cert = new CertificateImpl(CertificateUtils.getCertificate(
    																	certInfo.getContentBinaryObject().getValue()),
														 usage != null ? usage.getValue() : null);
		cert.setDescription(certInfo.getDescription() != null ? certInfo.getDescription().getValue() : null);
		final XMLGregorianCalendar activation = certInfo.getActivationDate() != null ?
																		certInfo.getActivationDate().getValue() : null;
		cert.setActivationDate(activation != null ? activation.toGregorianCalendar().toZonedDateTime() : null);
		final XMLGregorianCalendar expiration = certInfo.getExpirationDate() != null ?
				certInfo.getExpirationDate().getValue() : null;
		cert.setExpirationDate(expiration != null ? expiration.toGregorianCalendar().toZonedDateTime() : null);

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
	protected List<Extension> handleRedirectionExtensions(SMPExtensionsType extensions) {
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
	protected List<Extension> handleServiceMetadataExtensions(SMPExtensionsType extensions) {
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
	protected List<Extension> handleProcessInfoExtensions(SMPExtensionsType extensions) {
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
	protected List<Extension> handleProcessMetadataExtensions(SMPExtensionsType extensions) {
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
	protected List<Extension> handleEndpointInfoExtensions(SMPExtensionsType extensions) {
		return null;
	}
}
