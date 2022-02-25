package org.holodeckb2b.bdxr.smp.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.FileInputStream;
import java.io.IOException;

import org.holodeckb2b.bdxr.smp.api.SMPQueryException;
import org.holodeckb2b.bdxr.smp.datamodel.ISMPQueryResult;
import org.holodeckb2b.bdxr.smp.datamodel.ServiceMetadataResult;
import org.holodeckb2b.commons.testing.TestUtils;
import org.holodeckb2b.brdx.smp.testhelpers.MockResultProcessor;
import org.junit.jupiter.api.Test;

class SMPResultReaderTest {

	private static final String TEST_XML_NS = "http://docs.oasis-open.org/bdxr/ns/SMP/2/ServiceMetadata";
	
	@Test
	void testUnsignedXML() {
		SMPClientConfig cfg = new SMPClientConfig();
		cfg.addProcessor(new MockResultProcessor(TEST_XML_NS));
		
		try (FileInputStream fis = new FileInputStream(TestUtils.getTestResource("unsigned_result.xml").toFile())) {
			ISMPQueryResult smpData = new SMPResultReader(cfg).handleResponse(fis);
			
			assertNotNull(smpData);
		} catch (IOException e) {
			e.printStackTrace();
			fail();
		} catch (SMPQueryException e) {
			fail();
		}
	}
	
	@Test
	void testValidSignedXML() {
		SMPClientConfig cfg = new SMPClientConfig();
		cfg.addProcessor(new MockResultProcessor(TEST_XML_NS));
		
		try (FileInputStream fis = new FileInputStream(TestUtils.getTestResource("signed_result.xml").toFile())) {
			ISMPQueryResult smpData = new SMPResultReader(cfg).handleResponse(fis);
			
			assertNotNull(smpData);
			assertTrue(smpData instanceof ServiceMetadataResult);
			assertNotNull(((ServiceMetadataResult) smpData).getSignerCertificate());
		} catch (IOException e) {
			e.printStackTrace();
			fail();
		} catch (SMPQueryException e) {
			fail();
		}
	}
	
	@Test
	void testInvalidSignedXML() {
		SMPClientConfig cfg = new SMPClientConfig();
		cfg.addProcessor(new MockResultProcessor(TEST_XML_NS));
		
		try (FileInputStream fis = new FileInputStream(TestUtils.getTestResource("inv_signed_result.xml").toFile())) {
			ISMPQueryResult smpData = new SMPResultReader(cfg).handleResponse(fis);
			fail();
		} catch (IOException e) {
			e.printStackTrace();
			fail();
		} catch (SMPQueryException e) {			
		}
	}
	
	@Test
	void testSelectProcessor() {
		SMPClientConfig cfg = new SMPClientConfig();
		MockResultProcessor nonExecProc = new MockResultProcessor("some_other");
		MockResultProcessor execProc = new MockResultProcessor(TEST_XML_NS);
		cfg.addProcessor(nonExecProc);
		cfg.addProcessor(execProc);
		
		try (FileInputStream fis = new FileInputStream(TestUtils.getTestResource("unsigned_result.xml").toFile())) {
			ISMPQueryResult smpData = new SMPResultReader(cfg).handleResponse(fis);
			
			assertNotNull(smpData);
			assertTrue(execProc.wasCalled());
			assertFalse(nonExecProc.wasCalled());
		} catch (IOException e) {
			e.printStackTrace();
			fail();
		} catch (SMPQueryException e) {
			fail();
		}
	}	
	
	@Test
	void testNoProcessor() {
		SMPClientConfig cfg = new SMPClientConfig();
		MockResultProcessor nonExecProc = new MockResultProcessor("some_other");
		cfg.addProcessor(nonExecProc);
		
		try (FileInputStream fis = new FileInputStream(TestUtils.getTestResource("unsigned_result.xml").toFile())) {
			ISMPQueryResult smpData = new SMPResultReader(cfg).handleResponse(fis);
			fail();
		} catch (IOException e) {
			e.printStackTrace();
			fail();
		} catch (SMPQueryException e) {			
		}
	}	
}
