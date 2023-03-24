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
package org.holodeckb2b.bdxr.smp.client.impl;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.cert.X509Certificate;
import org.holodeckb2b.bdxr.smp.client.api.SMPQueryException;
import org.holodeckb2b.bdxr.smp.datamodel.QueryResult;
import org.holodeckb2b.bdxr.smp.datamodel.ServiceMetadata;
import org.holodeckb2b.bdxr.smp.datamodel.SignedQueryResult;
import org.holodeckb2b.bdxr.smp.datamodel.impl.IdentifierImpl;
import org.holodeckb2b.bdxr.smp.datamodel.impl.ServiceMetadataImpl;
import org.holodeckb2b.brdx.smp.testhelpers.MockResultProcessor;
import org.holodeckb2b.commons.testing.TestUtils;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class SMPResultReaderTest {

	private static final String TEST_XML_NS = "http://docs.oasis-open.org/bdxr/ns/SMP/2/ServiceMetadata";

	@Test
	void testUnsignedXML() throws Exception {
		Path respDoc = TestUtils.getTestResource("unsigned_result.xml");
		ServiceMetadataImpl smd = new ServiceMetadataImpl(new IdentifierImpl("P_ID_1"), null, null, null);

		SMPClientConfig cfg = new SMPClientConfig();
		cfg.addProcessor(new MockResultProcessor(TEST_XML_NS, respDoc, smd));

		try (FileInputStream fis = new FileInputStream(respDoc.toFile())) {
			QueryResult smpData = assertDoesNotThrow(() -> new SMPResultReader(cfg).handleResponse(fis));

			assertFalse(smpData instanceof SignedQueryResult);
			assertTrue(smpData instanceof ServiceMetadata);
			assertEquals(smd, (ServiceMetadata) smpData);
		}
	}

	@Test
	void testSignedXML() throws Exception {
		Path respDoc = TestUtils.getTestResource("signed_result.xml");
		ServiceMetadataImpl smd = new ServiceMetadataImpl(new IdentifierImpl("P_ID_1"), null, null, null);

		SMPClientConfig cfg = new SMPClientConfig();
		cfg.addProcessor(new MockResultProcessor(TEST_XML_NS, respDoc, smd));

		try (FileInputStream fis = new FileInputStream(respDoc.toFile())) {
			QueryResult smpData = assertDoesNotThrow(() -> new SMPResultReader(cfg).handleResponse(fis));

			assertTrue(smpData instanceof SignedQueryResult);
			assertTrue(smpData instanceof ServiceMetadata);
			assertEquals(smd, (ServiceMetadata) smpData);
		}
	}

	@Test
	void testInvalidSignedXML() throws Exception {
		Path respDoc = TestUtils.getTestResource("inv_signed_result.xml");
		ServiceMetadataImpl smd = new ServiceMetadataImpl(new IdentifierImpl("P_ID_1"), null, null, null);

		SMPClientConfig cfg = new SMPClientConfig();
		cfg.addProcessor(new MockResultProcessor(TEST_XML_NS, respDoc, smd));

		try (FileInputStream fis = new FileInputStream(respDoc.toFile())) {
			SMPQueryException ex = assertThrows(SMPQueryException.class,
																() -> new SMPResultReader(cfg).handleResponse(fis));
			assertTrue(ex.getMessage().contains("verified"));
		}
	}

	@Test
	void testUntrustedSignedXML() throws Exception {
		Path respDoc = TestUtils.getTestResource("signed_result.xml");
		ServiceMetadataImpl smd = new ServiceMetadataImpl(new IdentifierImpl("P_ID_1"), null, null, null);

		SMPClientConfig cfg = new SMPClientConfig();
		cfg.addProcessor(new MockResultProcessor(TEST_XML_NS, respDoc, smd));
		cfg.setTrustValidator((X509Certificate certificate) -> false);

		try (FileInputStream fis = new FileInputStream(respDoc.toFile())) {
			SMPQueryException ex = assertThrows(SMPQueryException.class,
																() -> new SMPResultReader(cfg).handleResponse(fis));
			assertTrue(ex.getMessage().contains("not trusted"));
		}
	}

	@Test
	void testSelectProcessor() throws IOException {
		SMPClientConfig cfg = new SMPClientConfig();
		MockResultProcessor nonExecProc = new MockResultProcessor("some_other");
		MockResultProcessor execProc = new MockResultProcessor(TEST_XML_NS);
		cfg.addProcessor(nonExecProc);
		cfg.addProcessor(execProc);

		try (FileInputStream fis = new FileInputStream(TestUtils.getTestResource("unsigned_result.xml").toFile())) {
			assertDoesNotThrow(() -> new SMPResultReader(cfg).handleResponse(fis));

			assertTrue(execProc.wasCalled());
			assertFalse(nonExecProc.wasCalled());
		}
	}

	@Test
	void testNoProcessor() throws IOException {
		SMPClientConfig cfg = new SMPClientConfig();
		MockResultProcessor nonExecProc = new MockResultProcessor("some_other");
		cfg.addProcessor(nonExecProc);

		try (FileInputStream fis = new FileInputStream(TestUtils.getTestResource("unsigned_result.xml").toFile())) {
			SMPQueryException ex = assertThrows(SMPQueryException.class,
																() -> new SMPResultReader(cfg).handleResponse(fis));
			assertTrue(ex.getMessage().contains("Unknown XML document"));
		}
	}
}
