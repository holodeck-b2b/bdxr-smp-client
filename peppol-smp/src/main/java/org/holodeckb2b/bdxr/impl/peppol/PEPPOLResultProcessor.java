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
package org.holodeckb2b.bdxr.impl.peppol;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.util.Collections;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.busdox.servicemetadata.publishing._1.EndpointType;
import org.busdox.servicemetadata.publishing._1.ProcessType;
import org.busdox.servicemetadata.publishing._1.RedirectType;
import org.busdox.servicemetadata.publishing._1.ServiceGroupType;
import org.busdox.servicemetadata.publishing._1.ServiceInformationType;
import org.busdox.servicemetadata.publishing._1.ServiceMetadataType;
import org.busdox.servicemetadata.publishing._1.SignedServiceMetadataType;
import org.holodeckb2b.bdxr.smp.api.ISMPResultProcessor;
import org.holodeckb2b.bdxr.smp.api.SMPQueryException;
import org.holodeckb2b.bdxr.smp.datamodel.Certificate;
import org.holodeckb2b.bdxr.smp.datamodel.EndpointInfo;
import org.holodeckb2b.bdxr.smp.datamodel.ISMPQueryResult;
import org.holodeckb2b.bdxr.smp.datamodel.Identifier;
import org.holodeckb2b.bdxr.smp.datamodel.ProcessIdentifier;
import org.holodeckb2b.bdxr.smp.datamodel.ProcessInfo;
import org.holodeckb2b.bdxr.smp.datamodel.ProcessList;
import org.holodeckb2b.bdxr.smp.datamodel.Redirection;
import org.holodeckb2b.bdxr.smp.datamodel.ServiceInformation;
import org.holodeckb2b.bdxr.smp.datamodel.ServiceMetadataResult;
import org.holodeckb2b.commons.security.CertificateUtils;
import org.w3c.dom.Document;

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
    static {
        try {
            // Initialize the JAXB Context used in processing of the PEPPOL responses
            jaxbContext = JAXBContext.newInstance(ServiceGroupType.class, SignedServiceMetadataType.class,
                                                  ServiceMetadataType.class);
        } catch (JAXBException jaxbFailure) {
            log.fatal("Could not load the JAXB Context required for processing SMP response! Details: {}",
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
    public ISMPQueryResult processResult(Document xmlDocument) throws SMPQueryException {
        JAXBElement jaxbDoc;
        try {
            log.debug("Convert the XML into Java objects");
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            jaxbDoc = (JAXBElement) unmarshaller.unmarshal(xmlDocument);
            log.debug("XML converted into Java objects");
        } catch (JAXBException parsingError) {
            log.error("Could not convert the XML document into Java objects! Details: {}", parsingError.getMessage());
            throw new SMPQueryException("XML could not be parsed as PEPPOL SMP result");
        }

        // Now check the root element of the response and process accordingly
        final Class rootType = jaxbDoc.getDeclaredType();
        if (ServiceGroupType.class.equals(rootType)) {
            log.debug("Document is a Service Group response");
            //@todo: Handle Service Group responses
            return null;
        } else {
            log.debug("Document is a (Signed) Service Metadata response, check for redirect");
            ServiceMetadataType svcMetadata = null;
            ServiceMetadataResult result = null;
            if (SignedServiceMetadataType.class.equals(rootType))
                svcMetadata = ((SignedServiceMetadataType) jaxbDoc.getValue()).getServiceMetadata();
            else
                svcMetadata = (ServiceMetadataType) jaxbDoc.getValue();
            if (svcMetadata.getRedirect() != null) {
                log.debug("Service Metadata contain a Redirect");
                result = processRedirection(svcMetadata.getRedirect());
            } else {
                log.debug("Service Metadata contain ServiceInformation");
                result = processServiceInformation(svcMetadata.getServiceInformation());
            }
            log.debug("Completely processed the response document");
            return result;
        }
    }
    
    /**
     * Processes the <i>service meta-data</i> included in the <code>ServiceInformation</code> element of the response.
     *
     * @param redirectXML   The JAXB representation of the <code>Redirect</code> element of the response
     * @return  A {@link Redirection} instance with the information on the redirection
     * @throws SMPQueryException When there is a problem in converting the XML into the object model
     */
    private Redirection processRedirection(RedirectType redirectXML) throws SMPQueryException {
    	try {
            // Although extensions are part of the PEPPOL specifications, they are not used in the PEPPOL network.
            // ==> ignored
    		return new Redirection(new URL(redirectXML.getHref()));
    	} catch (NullPointerException | MalformedURLException invalidURL) {
    		log.error("The Redirection response includes an invalid new target URL: {}", redirectXML.getHref());
    		throw new SMPQueryException("Invalid redirection response received!");
    	}
    }    

    /**
     * Processes the <i>service meta-data</i> included in the <code>ServiceInformation</code> element of the response.
     *
     * @param svcInfoXML   The JAXB representation of the <code>ServiceInformation</code> element of the response
     * @return  A {@link ServiceInformation} instance with the service meta-data
     * @throws SMPQueryException When there is a problem in converting the ServiceInformation XML into the object model
     */
    private ServiceInformation processServiceInformation(ServiceInformationType svcInfoXML) throws SMPQueryException {
        final ServiceInformation svcInfo = new ServiceInformation();

        svcInfo.setParticipantId(new Identifier(svcInfoXML.getParticipantIdentifier().getValue(),
                                                svcInfoXML.getParticipantIdentifier().getScheme()));
        svcInfo.setServiceId(new Identifier(svcInfoXML.getDocumentIdentifier().getValue(),
                                            svcInfoXML.getDocumentIdentifier().getScheme()));
        // Convert the list of Process elements in the ProcessList element
        for(ProcessType p : svcInfoXML.getProcessList().getProcess())
            svcInfo.addProcessInformation(processProcessInfo(p));
        // Although extensions are part of the PEPPOL specifications, they are not used in the PEPPOL network.
        // ==> ignored
        return svcInfo;
    }

    /**
     * Converts the information contained in the <code>Process</code> element into a <code>ProcessList</code> instance.
     *
     * @param procInfoXML   The JAXB representation of the <code>Process</code> element
     * @return              The <code>ProcessList</code> representation of the meta-data
     * @throws SMPQueryException When there is a problem in converting the Process XML into the object model
     */
    private ProcessList processProcessInfo(ProcessType procInfoXML) throws SMPQueryException {    	
    	final ProcessList procList = new ProcessList();

        // The PEPPOL specification allows just one (mandatory) process identifier
    	final ProcessInfo procInfo = new ProcessInfo();
        final String procID = procInfoXML.getProcessIdentifier().getValue();
        if (NO_PROCESS_ID.equals(procID)) 
        	procInfo.setProcessId(new ProcessIdentifier());
        else 
        	procInfo.setProcessId(new ProcessIdentifier(procID, procInfoXML.getProcessIdentifier().getScheme()));
        procList.addProcessInfo(procInfo);
        
        // Convert the Endpoint elements into object model
        for(EndpointType ep : procInfoXML.getServiceEndpointList().getEndpoint())
            procList.addEndpoint(processEndpoint(ep));
        // Extensions again ignored, even if part of the PEPPOL specifications
        return procList;
    }

    /**
     * Converts the information contained in an <code>Endpoint</code> element into an <code>EndpointInfo</code> instance.
     *
     * @param epInfoXML   The JAXB representation of the <code>Endpoint</code> element
     * @return            The <code>EndpointInfo</code> representation of the meta-data
     * @throws SMPQueryException When the XML certificate string could not be converted into object representation
     */
    private EndpointInfo processEndpoint(EndpointType epInfoXML) throws SMPQueryException {
        final EndpointInfo epInfo = new EndpointInfo();

        epInfo.setTransportProfile(epInfoXML.getTransportProfile());
        try {        	
			epInfo.setEndpointURL(new URL(epInfoXML.getEndpointReference().getAddress().getValue()));
		} catch (NullPointerException | MalformedURLException e) {
			log.error("Invalid URL specified for endpoint!");
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
            epInfo.setCertificates(Collections.singletonList(
                                        new Certificate(CertificateUtils.getCertificate(epInfoXML.getCertificate()))));
        } catch (CertificateException certReadError) {
            log.error("Could not read the Certificate from the SMP response! Details: {}", certReadError.getMessage());
            throw new SMPQueryException("Could not read the Certificate from the SMP response");
        }
        epInfo.setDescription(epInfoXML.getServiceDescription());
        epInfo.setContactInfo(epInfoXML.getTechnicalContactUrl());
        
        // As extensions are not used in the PEPPOL network, they are ignored here

        return epInfo;
    }
}
