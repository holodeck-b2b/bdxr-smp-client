/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.holodeckb2b.bdxr.smp.client.impl.oasis_smp2;

import org.holodeckb2b.bdxr.smp.client.impl.oasis_smp2.ServiceMetadataProcessor;
import java.io.FileInputStream;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import org.holodeckb2b.bdxr.smp.client.api.SMPQueryException;
import org.holodeckb2b.bdxr.smp.datamodel.Certificate;
import org.holodeckb2b.bdxr.smp.datamodel.EndpointInfo;
import org.holodeckb2b.bdxr.smp.datamodel.ProcessGroup;
import org.holodeckb2b.bdxr.smp.datamodel.ProcessInfo;
import org.holodeckb2b.bdxr.smp.datamodel.QueryResult;
import org.holodeckb2b.bdxr.smp.datamodel.RedirectionV2;
import org.holodeckb2b.bdxr.smp.datamodel.ServiceMetadata;
import org.holodeckb2b.bdxr.smp.datamodel.impl.IdentifierImpl;
import org.holodeckb2b.bdxr.smp.datamodel.impl.ProcessIdentifierImpl;
import org.holodeckb2b.bdxr.smp.datamodel.impl.ProcessInfoImpl;
import org.holodeckb2b.bdxr.smp.datamodel.util.Comparator;
import org.holodeckb2b.commons.security.CertificateUtils;
import org.holodeckb2b.commons.testing.TestUtils;
import org.holodeckb2b.commons.util.Utils;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.w3c.dom.Document;

/**
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class ServiceMetadataProcessorTest {

	@Test
	void testMinimal() throws Exception {
		Document xml = readXMLDoc("minimal.xml");

		final ServiceMetadataProcessor processor = new ServiceMetadataProcessor();

		QueryResult qr = assertDoesNotThrow(() -> processor.processServiceMetadata(xml));

		assertNotNull(qr);
		assertTrue(qr instanceof ServiceMetadata);

		ServiceMetadata smd = (ServiceMetadata)qr;

		assertTrue(Comparator.equalIDs(
								new IdentifierImpl("holodeckb2b-test", "urn:oasis:tc:ebcore:partyid-type:unregistered"),
								smd.getParticipantId()));
		assertTrue(Comparator.equalIDs(
								new IdentifierImpl("urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017", "bdx-docid-qns"),
								smd.getServiceId()));

		assertEquals(1, smd.getProcessMetadata().size());
		ProcessGroup pg = smd.getProcessMetadata().iterator().next();

		assertTrue(Utils.isNullOrEmpty(pg.getProcessInfo()));
		assertEquals(1, pg.getEndpoints().size());
		EndpointInfo ep = pg.getEndpoints().iterator().next();
		assertEquals("bdxr-transport-ebms3-as4-v1p0", ep.getTransportProfile());
		assertEquals(new URL("https://ap.sample.holodeck-b2b.org/"), ep.getEndpointURL());
		assertNull(ep.getServiceActivationDate());
		assertNull(ep.getServiceExpirationDate());
		assertTrue(Utils.isNullOrEmpty(ep.getCertificates()));
		assertNull(ep.getDescription());
		assertNull(ep.getContactInfo());
	}

	@Test
	void testDetails() throws Exception {
		Document xml = readXMLDoc("detailed.xml");
		X509Certificate expCert = CertificateUtils.getCertificate(TestUtils.getTestResource("endpoint_a.cert"));

		final ServiceMetadataProcessor processor = new ServiceMetadataProcessor();

		QueryResult qr = assertDoesNotThrow(() -> processor.processServiceMetadata(xml));

		assertNotNull(qr);
		assertTrue(qr instanceof ServiceMetadata);

		ServiceMetadata smd = (ServiceMetadata)qr;

		assertTrue(Comparator.equalIDs(
								new IdentifierImpl("holodeckb2b-test", "urn:oasis:tc:ebcore:partyid-type:unregistered"),
								smd.getParticipantId()));
		assertTrue(Comparator.equalIDs(
								new IdentifierImpl("urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017", "bdx-docid-qns"),
								smd.getServiceId()));

		assertEquals(1, smd.getProcessMetadata().size());
		ProcessGroup pg = smd.getProcessMetadata().iterator().next();

		assertEquals(1, pg.getProcessInfo().size());
		ProcessInfo pi = pg.getProcessInfo().iterator().next();
		assertTrue(Comparator.equalProcIDs(new ProcessIdentifierImpl("urn:cen.eu:en16931:2017"), pi.getProcessId()));
		assertEquals(1, pi.getRoles().size());
		assertTrue(Comparator.equalIDs(new IdentifierImpl("Buyer"), pi.getRoles().iterator().next()));

		assertEquals(1, pg.getEndpoints().size());
		EndpointInfo ep = pg.getEndpoints().iterator().next();
		assertEquals("bdxr-transport-ebms3-as4-v1p0", ep.getTransportProfile());
		assertEquals(new URL("https://ap.sample.holodeck-b2b.org/"), ep.getEndpointURL());
		assertEquals(Instant.parse("2022-02-24T00:00:00+01:00"), ep.getServiceActivationDate().toInstant());
		assertEquals(Instant.parse("2024-01-01T00:00:00+01:00"), ep.getServiceExpirationDate().toInstant());
		assertEquals("Only defined for testing of SMP client" , ep.getDescription());
		assertEquals("sander at holodeck-b2b.org" , ep.getContactInfo());
		assertEquals(1, ep.getCertificates().size());
		Certificate cert = ep.getCertificates().iterator().next();
		assertEquals("sign-and-encrypt", cert.getUsage());
		assertEquals("AP cert for signing and encryption", cert.getDescription());
		assertEquals(LocalDate.parse("2021-01-01"), cert.getActivationDate().toLocalDate());
		assertEquals(LocalDate.parse("2024-01-01"), cert.getExpirationDate().toLocalDate());
		assertEquals(expCert, cert.getX509Cert());
	}

	@Test
	void testRepeatedElements() throws Exception {
		Document xml = readXMLDoc("repetition.xml");
		X509Certificate expCertA = CertificateUtils.getCertificate(TestUtils.getTestResource("endpoint_a.cert"));
		X509Certificate expCertB = CertificateUtils.getCertificate(TestUtils.getTestResource("endpoint_b.cert"));

		final ServiceMetadataProcessor processor = new ServiceMetadataProcessor();

		QueryResult qr = assertDoesNotThrow(() -> processor.processServiceMetadata(xml));

		assertNotNull(qr);
		assertTrue(qr instanceof ServiceMetadata);

		ServiceMetadata smd = (ServiceMetadata)qr;

		assertTrue(Comparator.equalIDs(
								new IdentifierImpl("holodeckb2b-test", "urn:oasis:tc:ebcore:partyid-type:unregistered"),
								smd.getParticipantId()));
		assertTrue(Comparator.equalIDs(
								new IdentifierImpl("urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017", "bdx-docid-qns"),
								smd.getServiceId()));

		assertEquals(2, smd.getProcessMetadata().size());

		for(ProcessGroup pg : smd.getProcessMetadata()) {
			if (pg.getProcessInfo().parallelStream()
								.anyMatch(pi -> "urn:cen.eu:en16931:2017".equals(pi.getProcessId().getValue()))) {

				assertEquals(2, pg.getProcessInfo().size());
				assertTrue(pg.getProcessInfo().parallelStream().anyMatch(pi ->
						Comparator.equalProcessInfos(new ProcessInfoImpl(new ProcessIdentifierImpl("urn:cen.eu:en16931:2023"), Set.of(new IdentifierImpl("Buyer")), null), pi)
				));

				assertEquals(2, pg.getEndpoints().size());
				for(EndpointInfo ep : pg.getEndpoints()) {
					if ("bdxr-transport-ebms3-as4-v1p0".equals(ep.getTransportProfile())) {
						assertEquals(new URL("https://ap.sample.holodeck-b2b.org/"), ep.getEndpointURL());
						assertEquals(Instant.parse("2022-02-24T00:00:00+01:00"), ep.getServiceActivationDate().toInstant());
						assertEquals(Instant.parse("2024-01-01T00:00:00+01:00"), ep.getServiceExpirationDate().toInstant());
						assertEquals("Only defined for testing of SMP client" , ep.getDescription());
						assertEquals("sander at holodeck-b2b.org" , ep.getContactInfo());
						assertEquals(2, ep.getCertificates().size());
						for (Certificate cert : ep.getCertificates()) {
							assertEquals("sign-and-encrypt", cert.getUsage());
							if (expCertA.equals(cert.getX509Cert()))
								assertEquals(Instant.parse("2022-07-01T00:00:00Z"), cert.getExpirationDate().toInstant());
							else {
								assertEquals(expCertB, cert.getX509Cert());
								assertEquals(Instant.parse("2022-04-01T00:00:00Z"), cert.getActivationDate().toInstant());
							}
						}
					} else {
						assertEquals("old-transport-protocol", ep.getTransportProfile());
						assertEquals(new URL("https://ap.sample.holodeck-b2b.org/old"), ep.getEndpointURL());
						assertNull(ep.getServiceActivationDate());
						assertEquals(Instant.parse("2022-08-01T00:00:00+01:00"), ep.getServiceExpirationDate().toInstant());
						assertEquals("Only defined for testing of SMP client" , ep.getDescription());
						assertEquals("sander at holodeck-b2b.org" , ep.getContactInfo());
						assertEquals(1, ep.getCertificates().size());
						Certificate cert = ep.getCertificates().iterator().next();
						assertNull(cert.getUsage());
						assertEquals(expCertA, cert.getX509Cert());
					}
				}
			} else {
				assertEquals(1, pg.getProcessInfo().size());
				assertTrue(Comparator.equalProcessInfos(new ProcessInfoImpl(new ProcessIdentifierImpl("urn:cen.eu:en16931:2014"), null),
														pg.getProcessInfo().iterator().next()));
				assertEquals(1, pg.getEndpoints().size());
				EndpointInfo ep = pg.getEndpoints().iterator().next();
				assertEquals("old-transport-protocol", ep.getTransportProfile());
				assertEquals(new URL("https://ap.sample.holodeck-b2b.org/old"), ep.getEndpointURL());
				assertNull(ep.getServiceActivationDate());
				assertEquals(Instant.parse("2022-05-01T00:00:00+01:00"), ep.getServiceExpirationDate().toInstant());
				assertEquals("Only defined for testing of SMP client" , ep.getDescription());
				assertEquals("sander at holodeck-b2b.org" , ep.getContactInfo());
				assertTrue(Utils.isNullOrEmpty(ep.getCertificates()));
			}
		}
	}

	@Test
	void testNoProcess() throws Exception {
		Document xml = readXMLDoc("noprocess.xml");

		final ServiceMetadataProcessor processor = new ServiceMetadataProcessor();

		QueryResult qr = assertDoesNotThrow(() -> processor.processServiceMetadata(xml));

		assertNotNull(qr);
		assertTrue(qr instanceof ServiceMetadata);

		ServiceMetadata smd = (ServiceMetadata)qr;

		assertTrue(Comparator.equalIDs(
								new IdentifierImpl("holodeckb2b-test", "urn:oasis:tc:ebcore:partyid-type:unregistered"),
								smd.getParticipantId()));
		assertTrue(Comparator.equalIDs(
								new IdentifierImpl("urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017", "bdx-docid-qns"),
								smd.getServiceId()));

		assertEquals(1, smd.getProcessMetadata().size());
		ProcessGroup pg = smd.getProcessMetadata().iterator().next();

		assertEquals(1, pg.getProcessInfo().size());
		assertTrue(pg.getProcessInfo().iterator().next().getProcessId().isNoProcess());
	}

	@Test
	void testRedirection() throws Exception {
		Document xml = readXMLDoc("redirections.xml");
		X509Certificate expCert = CertificateUtils.getCertificate(TestUtils.getTestResource("endpoint_a.cert"));

		final ServiceMetadataProcessor processor = new ServiceMetadataProcessor();

		QueryResult qr = assertDoesNotThrow(() -> processor.processServiceMetadata(xml));

		assertTrue(qr instanceof ServiceMetadata);
		ServiceMetadata smd = (ServiceMetadata)qr;

		assertTrue(Comparator.equalIDs(
								new IdentifierImpl("holodeckb2b-test", "urn:oasis:tc:ebcore:partyid-type:unregistered"),
								smd.getParticipantId()));
		assertTrue(Comparator.equalIDs(
								new IdentifierImpl("urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017", "bdx-docid-qns"),
								smd.getServiceId()));

		assertEquals(2, smd.getProcessMetadata().size());

		for(ProcessGroup pg : smd.getProcessMetadata()) {
			assertTrue(Utils.isNullOrEmpty(pg.getEndpoints()));
			assertNotNull(pg.getRedirection());
			assertTrue(pg.getRedirection() instanceof RedirectionV2);
			RedirectionV2 r = (RedirectionV2) pg.getRedirection();
			if (!Utils.isNullOrEmpty(pg.getProcessInfo())) {
				assertEquals(new URL("http://new.smp.for.specific.proc"), r.getNewSMPURL());
				assertEquals(expCert, r.getSMPCertificate());
			} else {
				assertEquals(new URL("http://new.default.smp"), r.getNewSMPURL());
				assertNull(r.getSMPCertificate());
			}
		}
	}

	@ParameterizedTest
	@ValueSource(strings = {"invalid.xml", "no_partid.xml", "no_serviceid.xml", "no_procid.xml", "no_roleid.xml",
							"no_transport_id.xml", "both_ep_redirect.xml"})
	void testInvalid(String xmlFile) throws Exception {
		Document xml = readXMLDoc(xmlFile);
		assertThrows(SMPQueryException.class, () -> new ServiceMetadataProcessor().processServiceMetadata(xml));
	}

	private Document readXMLDoc(String testFile) throws Exception {
		try (FileInputStream is = new FileInputStream(TestUtils.getTestResource(testFile).toFile())) {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            return dbf.newDocumentBuilder().parse(is);
		}
	}
}
