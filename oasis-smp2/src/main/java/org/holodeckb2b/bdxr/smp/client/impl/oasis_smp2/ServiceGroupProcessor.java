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

import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.holodeckb2b.bdxr.smp.client.api.SMPQueryException;
import org.holodeckb2b.bdxr.smp.datamodel.Extension;
import org.holodeckb2b.bdxr.smp.datamodel.QueryResult;
import org.holodeckb2b.bdxr.smp.datamodel.ServiceReference;
import org.holodeckb2b.bdxr.smp.datamodel.impl.IdentifierImpl;
import org.holodeckb2b.bdxr.smp.datamodel.impl.ProcessIdentifierImpl;
import org.holodeckb2b.bdxr.smp.datamodel.impl.ProcessInfoImpl;
import org.holodeckb2b.bdxr.smp.datamodel.impl.ServiceGroupV2Impl;
import org.holodeckb2b.bdxr.smp.datamodel.impl.ServiceReferenceImpl;
import org.holodeckb2b.commons.util.Utils;
import org.oasis_open.docs.bdxr.ns.smp._2.aggregatecomponents.ProcessType;
import org.oasis_open.docs.bdxr.ns.smp._2.aggregatecomponents.ServiceReferenceType;
import org.oasis_open.docs.bdxr.ns.smp._2.basiccomponents.RoleIDType;
import org.oasis_open.docs.bdxr.ns.smp._2.extensioncomponents.SMPExtensionsType;
import org.oasis_open.docs.bdxr.ns.smp._2.servicegroup.ServiceGroupType;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Handles the processing of the <i>ServiceGroup</i> XML as defined in the OASIS SMP version 2.0 specification.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  NEXT_VERSION
 */
class ServiceGroupProcessor {
    private static final Logger	log = LogManager.getLogger(ServiceGroupProcessor.class);

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
            jaxbContext = JAXBContext.newInstance(ServiceGroupType.class);
			// Read the XSD for validation of responses
			SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			smpSchema = sf.newSchema(ServiceGroupProcessor.class.getResource("/xsd/ServiceGroup-2.0.xsd"));
        } catch (SAXException | JAXBException jaxbFailure) {
            log.fatal("Could not prepare the JAXB Context required for processing SMP response! Details: {}",
                      jaxbFailure.getMessage());
            throw new RuntimeException(jaxbFailure.getMessage(), jaxbFailure);
        }
    }

    QueryResult processServiceGroup(Document xmlDocument) throws SMPQueryException {
        ServiceGroupType sgXML;
        try {
            log.trace("Parsing the XML of ServiceMetadata document");
			Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
			unmarshaller.setSchema(smpSchema);
			sgXML = unmarshaller.unmarshal(xmlDocument, ServiceGroupType.class).getValue();
        } catch (JAXBException parsingError) {
            log.error("Could not convert the XML document into Java objects! Details: {}", parsingError.getMessage());
            throw new SMPQueryException("XML could not be parsed as OASIS SMP2 result");
        }
		try {
			log.trace("Convert XML into object data model representation");
			final ServiceGroupV2Impl sg = new ServiceGroupV2Impl();
			sg.setParticipantId(new IdentifierImpl(sgXML.getParticipantID().getValue(),
													sgXML.getParticipantID().getSchemeID()));
			// Convert the list of ServiceReference elements
			for(ServiceReferenceType r : sgXML.getServiceReference())
				sg.addServiceReference(convertServiceRef(r));

			sg.setExtensions(handleServiceGroupExtensions(sgXML.getSMPExtensions()));

			log.debug("Completely processed the response document");
			return sg;
		} catch (IllegalArgumentException iae) {
			log.error("Response contains an invalid value for some meta-data! Details: {}", Utils.getExceptionTrace(iae));
			throw new SMPQueryException("Response contains an invalid value");
		}
    }

	private ServiceReference convertServiceRef(ServiceReferenceType refXML) {
		ServiceReferenceImpl ref = new ServiceReferenceImpl();

		ref.setServiceId(new IdentifierImpl(refXML.getID().getValue(), refXML.getID().getSchemeID()));

		for (ProcessType pi : refXML.getProcess()) {
			final ProcessInfoImpl procInfo = new ProcessInfoImpl();
			final String procID = pi.getID().getValue();
			if (OASISv2ResultProcessor.NO_PROCESS_ID.equals(procID))
				procInfo.setProcessId(new ProcessIdentifierImpl());
			else
				procInfo.setProcessId(new ProcessIdentifierImpl(procID, pi.getID().getSchemeID()));

			for(RoleIDType r : pi.getRoleID())
				procInfo.addRole(new IdentifierImpl(r.getValue(), r.getSchemeID()));
			procInfo.setExtensions(handleProcessInfoExtensions(pi.getSMPExtensions()));
			ref.addProcessInfo(procInfo);
		}

		ref.setExtensions(handleServiceReferenceExtensions(refXML.getSMPExtensions()));

		return ref;
	}

    /**
     * Converts the <code>SMPExtensions</code> child element of the <code>ServiceGroup</code> element into the object
     * representation.
     * <p><b>NOTE: </b> This default implementation <b>ignores</b> all included extensions. If a network uses
     * extension you should create a descendant class and override this method to correctly handle the network
     * specific extensions.
     *
	 * @param extensions	The extension included with the <code>ServiceGroup</code> element
	 * @return				The object representation of the extensions
	 */
	protected List<Extension> handleServiceGroupExtensions(SMPExtensionsType extensions) {
		return null;
	}

	/**
     * Converts the <code>SMPExtensions</code> child element of the <code>ServiceReference</code> element into the object
     * representation.
     * <p><b>NOTE: </b> This default implementation <b>ignores</b> all included extensions. If a network uses
     * extension you should create a descendant class and override this method to correctly handle the network
     * specific extensions.
     *
	 * @param extensions	The extension included with the <code>ServiceReference</code> element
	 * @return				The object representation of the extensions
	 */
	protected List<Extension> handleServiceReferenceExtensions(SMPExtensionsType extensions) {
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
}
