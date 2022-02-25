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
package org.holodeckb2b.brdx.smp.testhelpers;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.security.cert.X509Certificate;
import javax.xml.parsers.DocumentBuilderFactory;
import org.holodeckb2b.bdxr.smp.client.api.ISMPResultProcessor;
import org.holodeckb2b.bdxr.smp.client.api.SMPQueryException;
import org.holodeckb2b.bdxr.smp.datamodel.QueryResult;
import org.holodeckb2b.bdxr.smp.datamodel.ServiceGroupV2;
import org.holodeckb2b.bdxr.smp.datamodel.ServiceMetadata;
import org.holodeckb2b.bdxr.smp.datamodel.SignedQueryResult;
import org.holodeckb2b.bdxr.smp.datamodel.impl.SignedServiceMetadataImpl;
import org.w3c.dom.Document;

/**
 * A mock result processor that returns a pre-configured result when it is given an XML document with the specified
 * namespace. It can also check that the XML document handed over to it is equal to a pre-set one.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class MockResultProcessor implements ISMPResultProcessor {
	private final String		supportedNS;
	private final Document		expectedDoc;
	private final QueryResult	result;

	private boolean called = false;

	public MockResultProcessor(final String forNS) {
		this.supportedNS = forNS;
		this.expectedDoc = null;
		this.result = null;
	}

	public MockResultProcessor(final String forNS, final QueryResult r) {
		this.supportedNS = forNS;
		this.expectedDoc = null;
		this.result = r;
	}

	public MockResultProcessor(final String forNS, final Path expected, final QueryResult reqResult) throws Exception {
		try (FileInputStream is = new FileInputStream(expected.toFile())) {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            expectedDoc = dbf.newDocumentBuilder().parse(is);
		}
		supportedNS = forNS;
		result = reqResult;
	}

	public MockResultProcessor(final String forNS, final Document expected, final QueryResult reqResult) {
		supportedNS = forNS;
		expectedDoc = expected;
		result = reqResult;
	}

	@Override
	public QueryResult processResult(Document xmlDocument) throws SMPQueryException {
		called = true;
		if (expectedDoc != null && !expectedDoc.isEqualNode(xmlDocument))
			throw new SMPQueryException("Provided XML document is different from expected document");
		return result;
	}

	@Override
	public boolean canProcess(String namespaceURI) {
		return supportedNS.equals(namespaceURI);
	}

	public boolean wasCalled() {
		return called;
	}

	@Override
	public SignedQueryResult processResult(Document xmlDocument, X509Certificate signingCert) throws SMPQueryException {
		processResult(xmlDocument);
		if (result instanceof ServiceMetadata)
			return new SignedServiceMetadataImpl((ServiceMetadata) result, signingCert);
		else if (result instanceof ServiceGroupV2)
			return null;
		else
			throw new UnsupportedOperationException();
	}
}
