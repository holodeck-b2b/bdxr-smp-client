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


import java.security.cert.X509Certificate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.holodeckb2b.bdxr.smp.client.api.ISMPResultProcessor;
import org.holodeckb2b.bdxr.smp.client.api.SMPQueryException;
import org.holodeckb2b.bdxr.smp.datamodel.QueryResult;
import org.holodeckb2b.bdxr.smp.datamodel.ServiceGroupV2;
import org.holodeckb2b.bdxr.smp.datamodel.ServiceMetadata;
import org.holodeckb2b.bdxr.smp.datamodel.SignedQueryResult;
import org.holodeckb2b.bdxr.smp.datamodel.impl.SignedServiceGroupImpl;
import org.holodeckb2b.bdxr.smp.datamodel.impl.SignedServiceMetadataImpl;
import org.w3c.dom.Document;

/**
 * Is the {@link ISMPResultProcessor} implementation that handles the XML format as defined in the OASIS SMP version 2.0
 * specification.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  2.0.0
 */
public class OASISv2ResultProcessor implements ISMPResultProcessor {
    private static final Logger	log = LogManager.getLogger(OASISv2ResultProcessor.class);

    /**
     * The special process identifier used to indicate that the document id is not assigned to a specific process.
     */
    static final String NO_PROCESS_ID = "bdx:noprocess";

    /**
     * The namespace URI of the SMP XML <i>ServiceMetadata</i> document as specified in the OASIS SMP V2 specification
     */
    public static final String SVC_METADATA_NS_URI = "http://docs.oasis-open.org/bdxr/ns/SMP/2/ServiceMetadata";
    /**
     * The namespace URI of the SMP XML <i>ServiceGroup</i> document as specified in the OASIS SMP V2 specification
     */
    public static final String SVC_GROUP_NS_URI = "http://docs.oasis-open.org/bdxr/ns/SMP/2/ServiceGroup";

	private ServiceMetadataProcessor	smdProcessor;
	private ServiceGroupProcessor		sgProcessor;

	public OASISv2ResultProcessor() {
		smdProcessor = new ServiceMetadataProcessor();
		sgProcessor = new ServiceGroupProcessor();
	}

    @Override
    public boolean canProcess(final String namespaceURI) {
    	return SVC_METADATA_NS_URI.equals(namespaceURI) || SVC_GROUP_NS_URI.equals(namespaceURI);
    }

    @Override
    public QueryResult processResult(Document xmlDocument) throws SMPQueryException {
		final String docNS = xmlDocument.getDocumentElement().getNamespaceURI();
		if (SVC_METADATA_NS_URI.equals(docNS))
			return smdProcessor.processServiceMetadata(xmlDocument);
		else // SVC_GROUP_NS_URI.equals(docNS)
			return sgProcessor.processServiceGroup(xmlDocument);
    }

	@Override
	public SignedQueryResult processResult(Document xmlDocument, X509Certificate signingCert) throws SMPQueryException {
		final String docNS = xmlDocument.getDocumentElement().getNamespaceURI();
		if (SVC_METADATA_NS_URI.equals(docNS))
			return new SignedServiceMetadataImpl((ServiceMetadata) smdProcessor.processServiceMetadata(xmlDocument));
		else // SVC_GROUP_NS_URI.equals(docNS)
			return new SignedServiceGroupImpl((ServiceGroupV2) sgProcessor.processServiceGroup(xmlDocument));
	}
}
