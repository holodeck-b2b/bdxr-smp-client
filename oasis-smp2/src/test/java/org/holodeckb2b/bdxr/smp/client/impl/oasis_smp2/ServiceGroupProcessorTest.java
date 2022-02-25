/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.holodeckb2b.bdxr.smp.client.impl.oasis_smp2;

import org.holodeckb2b.bdxr.smp.client.impl.oasis_smp2.ServiceGroupProcessor;
import org.holodeckb2b.bdxr.smp.client.impl.oasis_smp2.ServiceMetadataProcessor;
import java.io.FileInputStream;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import org.holodeckb2b.bdxr.smp.client.api.SMPQueryException;
import org.holodeckb2b.bdxr.smp.datamodel.QueryResult;
import org.holodeckb2b.bdxr.smp.datamodel.ServiceGroupV2;
import org.holodeckb2b.bdxr.smp.datamodel.ServiceReference;
import org.holodeckb2b.bdxr.smp.datamodel.impl.IdentifierImpl;
import org.holodeckb2b.bdxr.smp.datamodel.impl.ProcessIdentifierImpl;
import org.holodeckb2b.bdxr.smp.datamodel.impl.ProcessInfoImpl;
import org.holodeckb2b.bdxr.smp.datamodel.util.Comparator;
import org.holodeckb2b.commons.testing.TestUtils;
import org.holodeckb2b.commons.util.Utils;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.w3c.dom.Document;

/**
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class ServiceGroupProcessorTest {

	@Test
	void testNormal() throws Exception {
		Document xml = readXMLDoc("detailed.xml");

		final ServiceGroupProcessor processor = new ServiceGroupProcessor();

		QueryResult qr = assertDoesNotThrow(() -> processor.processServiceGroup(xml));

		assertNotNull(qr);
		assertTrue(qr instanceof ServiceGroupV2);

		ServiceGroupV2 sg = (ServiceGroupV2) qr;

		assertTrue(Comparator.equalIDs(
								new IdentifierImpl("holodeckb2b-test", "urn:oasis:tc:ebcore:partyid-type:unregistered"),
								sg.getParticipantId()));

		assertEquals(2, sg.getServiceReferences().size());
		for (ServiceReference r : sg.getServiceReferences()) {
			if (Comparator.equalIDs(
								new IdentifierImpl("urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017", "bdx-docid-qns"),
								r.getServiceId())) {
				assertEquals(2, r.getProcessInfo().size());
				assertTrue(r.getProcessInfo().parallelStream().allMatch(pi ->
					Comparator.equalProcessInfos(new ProcessInfoImpl(new ProcessIdentifierImpl("urn:cen.eu:en16931:2017"), null), pi)
					|| Comparator.equalProcessInfos(new ProcessInfoImpl(new ProcessIdentifierImpl("urn:cen.eu:en16931:2018"), null), pi)
				));
			} else {
				assertTrue(Comparator.equalIDs(
								new IdentifierImpl("urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2023", "bdx-docid-qns"),
								r.getServiceId()));
				assertEquals(1, r.getProcessInfo().size());
				assertTrue(Comparator.equalProcessInfos(
						new ProcessInfoImpl(new ProcessIdentifierImpl("urn:cen.eu:en16931:2023"), Set.of(new IdentifierImpl("Buyer")), null),
						r.getProcessInfo().iterator().next()));
			}
		}
	}

	@Test
	void testAllProcess() throws Exception {
		Document xml = readXMLDoc("allprocesses.xml");

		final ServiceGroupProcessor processor = new ServiceGroupProcessor();

		QueryResult qr = assertDoesNotThrow(() -> processor.processServiceGroup(xml));
		assertNotNull(qr);
		assertTrue(qr instanceof ServiceGroupV2);

		ServiceGroupV2 sg = (ServiceGroupV2) qr;

		assertTrue(Comparator.equalIDs(
								new IdentifierImpl("holodeckb2b-test", "urn:oasis:tc:ebcore:partyid-type:unregistered"),
								sg.getParticipantId()));

		assertEquals(1, sg.getServiceReferences().size());
		ServiceReference r = sg.getServiceReferences().iterator().next();
		assertTrue(Comparator.equalIDs(
					new IdentifierImpl("urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2023", "bdx-docid-qns"),
					r.getServiceId()));
		assertTrue(Utils.isNullOrEmpty(r.getProcessInfo()));
	}

	@Test
	void testNoProcess() throws Exception {
		Document xml = readXMLDoc("noprocess.xml");

		final ServiceGroupProcessor processor = new ServiceGroupProcessor();

		QueryResult qr = assertDoesNotThrow(() -> processor.processServiceGroup(xml));
		assertNotNull(qr);
		assertTrue(qr instanceof ServiceGroupV2);

		ServiceGroupV2 sg = (ServiceGroupV2) qr;

		assertTrue(Comparator.equalIDs(
								new IdentifierImpl("holodeckb2b-test", "urn:oasis:tc:ebcore:partyid-type:unregistered"),
								sg.getParticipantId()));

		assertEquals(1, sg.getServiceReferences().size());
		ServiceReference r = sg.getServiceReferences().iterator().next();
		assertTrue(Comparator.equalIDs(
					new IdentifierImpl("urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2023", "bdx-docid-qns"),
					r.getServiceId()));
		assertEquals(1, r.getProcessInfo().size());
		assertTrue(r.getProcessInfo().iterator().next().getProcessId().isNoProcess());
	}

	@Test
	void testEmpty() throws Exception {
		Document xml = readXMLDoc("emptygroup.xml");

		final ServiceGroupProcessor processor = new ServiceGroupProcessor();

		QueryResult qr = assertDoesNotThrow(() -> processor.processServiceGroup(xml));
		assertNotNull(qr);
		assertTrue(qr instanceof ServiceGroupV2);

		ServiceGroupV2 sg = (ServiceGroupV2) qr;

		assertTrue(Comparator.equalIDs(
								new IdentifierImpl("holodeckb2b-test", "urn:oasis:tc:ebcore:partyid-type:unregistered"),
								sg.getParticipantId()));

		assertTrue(Utils.isNullOrEmpty(sg.getServiceReferences()));
	}

	@ParameterizedTest
	@ValueSource(strings = {"invalid.xml", "no_partid.xml", "no_serviceid.xml", "no_procid.xml", "no_roleid.xml"})
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
