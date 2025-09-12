/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.holodeckb2b.bdxr.smp.client.impl.oasis_smp1;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.FileInputStream;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Collection;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilderFactory;

import org.holodeckb2b.bdxr.common.datamodel.ProcessIdentifier;
import org.holodeckb2b.bdxr.common.datamodel.impl.IdentifierImpl;
import org.holodeckb2b.bdxr.common.datamodel.impl.ProcessIdentifierImpl;
import org.holodeckb2b.bdxr.smp.client.api.SMPQueryException;
import org.holodeckb2b.bdxr.smp.datamodel.EndpointInfo;
import org.holodeckb2b.bdxr.smp.datamodel.EndpointInfoV1;
import org.holodeckb2b.bdxr.smp.datamodel.ProcessGroup;
import org.holodeckb2b.bdxr.smp.datamodel.QueryResult;
import org.holodeckb2b.bdxr.smp.datamodel.Redirection;
import org.holodeckb2b.bdxr.smp.datamodel.ServiceGroupV1;
import org.holodeckb2b.bdxr.smp.datamodel.ServiceMetadata;
import org.holodeckb2b.bdxr.smp.datamodel.SignedQueryResult;
import org.holodeckb2b.commons.security.CertificateUtils;
import org.holodeckb2b.commons.testing.TestUtils;
import org.holodeckb2b.commons.util.Utils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.w3c.dom.Document;

/**
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class OASISv1ResultProcessorTests {

	@Test
	void testSignedSMD() throws Exception {
		Document xml = readXMLDoc("signedsmd.xml");
		X509Certificate cert = CertificateUtils.getCertificate(TestUtils.getTestResource("endpoint.cert"));

		final OASISv1ResultProcessor processor = new OASISv1ResultProcessor();

		assertTrue(processor.canProcess(xml.getDocumentElement().getNamespaceURI()));

		SignedQueryResult qr = assertDoesNotThrow(() -> processor.processResult(xml, cert));

		assertNotNull(qr);
		assertEquals(cert, qr.getSigningCertificate());
		assertTrue(qr instanceof ServiceMetadata);

		ServiceMetadata smd = (ServiceMetadata)qr;

		assertEquals(new IdentifierImpl("holodeckb2b-test", "urn:oasis:tc:ebcore:partyid-type:unregistered"),
					smd.getParticipantId());
		assertEquals(new IdentifierImpl("urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017", "bdx-docid-qns"),
					smd.getServiceId());

		assertEquals(2, smd.getProcessMetadata().size());
		for (ProcessGroup pg : smd.getProcessMetadata()) {
			assertFalse(Utils.isNullOrEmpty(pg.getProcessInfo()));
			ProcessIdentifier procId = (ProcessIdentifier) pg.getProcessInfo().iterator().next().getProcessId();
			if (procId.equals(new ProcessIdentifierImpl("urn:cen.eu:en16931:2017"))) {
				assertEquals(1, pg.getEndpoints().size());
				EndpointInfoV1 ep = (EndpointInfoV1) pg.getEndpoints().iterator().next();
				assertEquals("bdxr-transport-ebms3-as4-v1p0", ep.getTransportProfileId().getValue());
				assertEquals(new URL("https://ap.sample.holodeck-b2b.org/"), ep.getEndpointURL());
				assertFalse(ep.getBusinessLevelSignatureRequired());
				assertEquals("none", ep.getMinimumAuthenticationLevel());
				assertEquals(Instant.parse("2021-02-24T10:38:23.888Z"), ep.getServiceActivationDate().toInstant());
				assertEquals(Instant.parse("2024-01-01T00:00:00.000Z"), ep.getServiceExpirationDate().toInstant());
				assertFalse(Utils.isNullOrEmpty(ep.getCertificates()));
				assertEquals(cert, ep.getCertificates().iterator().next().getX509Cert());
				assertEquals("Only defined for testing of SMP client", ep.getDescription());
				assertEquals("sander at holodeck-b2b.org", ep.getContactInfo());
			} else if (procId.equals(new ProcessIdentifierImpl("urn:cen.eu:en16931:2023"))) {
				assertEquals(2, pg.getEndpoints().size());
				for(EndpointInfo ep : pg.getEndpoints()) {
					assertTrue(ep instanceof EndpointInfoV1);
					assertNull(((EndpointInfoV1) ep).getBusinessLevelSignatureRequired());
					assertNull(((EndpointInfoV1) ep).getMinimumAuthenticationLevel());
					assertFalse(Utils.isNullOrEmpty(ep.getCertificates()));
					assertEquals(cert, ep.getCertificates().iterator().next().getX509Cert());
					assertEquals("sander at holodeck-b2b.org", ep.getContactInfo());
					if ("bdxr-transport-ebms3-as4-v14p0".equals(ep.getTransportProfileId().getValue())) {
						assertEquals(new URL("https://ap.sample.holodeck-b2b.org/"), ep.getEndpointURL());
						assertNull(ep.getServiceActivationDate());
						assertEquals(Instant.parse("2022-07-01T00:00:00.000Z"), ep.getServiceExpirationDate().toInstant());
						assertEquals("Old profile", ep.getDescription());
					} else if ("bdxr-transport-ebms3-as4-v15p0".equals(ep.getTransportProfileId().getValue())) {
						assertEquals(new URL("https://ap2.sample.holodeck-b2b.org/"), ep.getEndpointURL());
						assertEquals(Instant.parse("2022-03-01T00:00:00.000Z"), ep.getServiceActivationDate().toInstant());
						assertNull(ep.getServiceExpirationDate());
						assertEquals("New profile", ep.getDescription());
					} else
						fail("Unexpected endpoint info");
				}
			} else
				fail("Unexpected process found");
		}
	}

	@Test
	void testUnsignedSMD() throws Exception {
		Document xml = readXMLDoc("unsignedsmd.xml");
		X509Certificate cert = CertificateUtils.getCertificate(TestUtils.getTestResource("endpoint.cert"));

		final OASISv1ResultProcessor processor = new OASISv1ResultProcessor();

		assertTrue(processor.canProcess(xml.getDocumentElement().getNamespaceURI()));

		SignedQueryResult qr = assertDoesNotThrow(() -> processor.processResult(xml, cert));

		assertNotNull(qr);
		assertEquals(cert, qr.getSigningCertificate());
		assertTrue(qr instanceof ServiceMetadata);

		ServiceMetadata smd = (ServiceMetadata)qr;

		assertEquals(new IdentifierImpl("holodeckb2b-test", "urn:oasis:tc:ebcore:partyid-type:unregistered"),
					smd.getParticipantId());
		assertEquals(new IdentifierImpl("urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017", "bdx-docid-qns"),
					smd.getServiceId());

		assertEquals(1, smd.getProcessMetadata().size());
		Iterator<? extends ProcessGroup> pgs = smd.getProcessMetadata().iterator();
		ProcessGroup pg = pgs.next();

		assertFalse(Utils.isNullOrEmpty(pg.getProcessInfo()));
		ProcessIdentifier procId = (ProcessIdentifier) pg.getProcessInfo().iterator().next().getProcessId();
		assertEquals(new ProcessIdentifierImpl("urn:cen.eu:en16931:2017"), procId);
		assertEquals(1, pg.getEndpoints().size());
		EndpointInfoV1 ep = (EndpointInfoV1) pg.getEndpoints().iterator().next();
		assertEquals("bdxr-transport-ebms3-as4-v1p0", ep.getTransportProfileId().getValue());
		assertEquals(new URL("https://ap.sample.holodeck-b2b.org/"), ep.getEndpointURL());
		assertNull(ep.getBusinessLevelSignatureRequired());
		assertNull(ep.getMinimumAuthenticationLevel());
		assertNull(ep.getServiceActivationDate());
		assertNull(ep.getServiceExpirationDate());
		assertTrue(Utils.isNullOrEmpty(ep.getCertificates()));
		assertEquals("Only defined for testing of SMP client", ep.getDescription());
		assertEquals("sander at holodeck-b2b.org", ep.getContactInfo());
		assertEquals(new URL("http://doc.test.holodeck-b2b.org/smp-test"), ep.getTechnicalInformationURL());
	}

	@Test
	void testNoProcess() throws Exception {
		Document xml = readXMLDoc("noprocess.xml");

		final OASISv1ResultProcessor processor = new OASISv1ResultProcessor();

		assertTrue(processor.canProcess(xml.getDocumentElement().getNamespaceURI()));

		QueryResult qr = assertDoesNotThrow(() -> processor.processResult(xml));

		assertNotNull(qr);
		assertTrue(qr instanceof ServiceMetadata);
		ServiceMetadata smd = (ServiceMetadata)qr;

		assertEquals(new IdentifierImpl("holodeckb2b-test", "urn:oasis:tc:ebcore:partyid-type:unregistered"),
					smd.getParticipantId());
		assertEquals(new IdentifierImpl("urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017", "bdx-docid-qns"),
					smd.getServiceId());

		assertEquals(1, smd.getProcessMetadata().size());
		Iterator<? extends ProcessGroup> pgs = smd.getProcessMetadata().iterator();
		ProcessGroup pg = pgs.next();

		assertFalse(Utils.isNullOrEmpty(pg.getProcessInfo()));
		assertTrue(pg.getProcessInfo().iterator().next().getProcessId().isNoProcess());
	}

	@Test
	void testRedirection() throws Exception {
		Document xml = readXMLDoc("redirection.xml");

		final OASISv1ResultProcessor processor = new OASISv1ResultProcessor();
		assertTrue(processor.canProcess(xml.getDocumentElement().getNamespaceURI()));

		QueryResult qr = assertDoesNotThrow(() -> processor.processResult(xml));

		assertTrue(qr instanceof ServiceMetadata);
		ServiceMetadata smd = (ServiceMetadata)qr;

		assertNull(smd.getParticipantId());

		assertEquals(1, smd.getProcessMetadata().size());
		ProcessGroup pg = smd.getProcessMetadata().iterator().next();
		assertTrue(Utils.isNullOrEmpty(pg.getProcessInfo()));
		assertTrue(Utils.isNullOrEmpty(pg.getEndpoints()));
		Redirection redirection = pg.getRedirection();
		assertNotNull(redirection);
		assertEquals(new URL("http://link.to.new.smp"), redirection.getNewSMPURL());
	}

	@Test
	void testServiceGroup() throws Exception {
		Document xml = readXMLDoc("servicegroup.xml");

		final OASISv1ResultProcessor processor = new OASISv1ResultProcessor();
		assertTrue(processor.canProcess(xml.getDocumentElement().getNamespaceURI()));

		QueryResult qr = assertDoesNotThrow(() -> processor.processResult(xml));

		assertTrue(qr instanceof ServiceGroupV1);
		ServiceGroupV1 sg = (ServiceGroupV1) qr;

		assertEquals(new IdentifierImpl("holodeckb2b-test", "urn:oasis:tc:ebcore:partyid-type:unregistered"),
					sg.getParticipantId());

		Collection<? extends URL> refs = sg.getServiceReferences();
		assertEquals(3, refs.size());
		for(URL r : refs)
			assertTrue(r.equals(new URL("http://link.to.service.1")) || r.equals(new URL("http://link.to.service.2"))
						|| r.equals(new URL("http://link.to.service.3")));
	}

	@Test
	void testEmptyServiceGroup() throws Exception {
		Document xml = readXMLDoc("empty_servicegroup.xml");

		final OASISv1ResultProcessor processor = new OASISv1ResultProcessor();
		assertTrue(processor.canProcess(xml.getDocumentElement().getNamespaceURI()));

		QueryResult qr = assertDoesNotThrow(() -> processor.processResult(xml));

		assertTrue(qr instanceof ServiceGroupV1);
		ServiceGroupV1 sg = (ServiceGroupV1) qr;

		assertTrue(Utils.isNullOrEmpty(sg.getServiceReferences()));
	}


	@ParameterizedTest
	@ValueSource(strings = {"invalid.xml", "no_partid.xml", "no_docid.xml", "no_ep_tprofile.xml"})
	void testInvalid(String xmlFile) throws Exception {
		Document xml = readXMLDoc(xmlFile);
		assertThrows(SMPQueryException.class, () -> new OASISv1ResultProcessor().processResult(xml));
	}

	private Document readXMLDoc(String testFile) throws Exception {
		try (FileInputStream is = new FileInputStream(TestUtils.getTestResource(testFile).toFile())) {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            return dbf.newDocumentBuilder().parse(is);
		}
	}
}
