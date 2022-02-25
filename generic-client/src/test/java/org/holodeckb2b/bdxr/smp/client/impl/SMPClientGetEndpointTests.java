package org.holodeckb2b.bdxr.smp.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.holodeckb2b.bdxr.smp.api.SMPClientBuilder;
import org.holodeckb2b.bdxr.smp.api.SMPQueryException;
import org.holodeckb2b.bdxr.smp.datamodel.EndpointInfo;
import org.holodeckb2b.bdxr.smp.datamodel.Identifier;
import org.holodeckb2b.bdxr.smp.datamodel.ProcessIdentifier;
import org.holodeckb2b.bdxr.smp.datamodel.ProcessInfo;
import org.holodeckb2b.bdxr.smp.datamodel.ProcessList;
import org.holodeckb2b.bdxr.smp.datamodel.ServiceInformation;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.brdx.smp.testhelpers.MockResultProcessor;
import org.holodeckb2b.brdx.smp.testhelpers.TestDocExecutor;
import org.junit.jupiter.api.Test;

class SMPClientTest {

	@Test
	void testGetEndpointsForProcess() throws MalformedURLException {
		
		Identifier participantId = new Identifier("PARTID_1", "test:scheme"); 
		Identifier serviceId = new Identifier("SVCID_1"); 
		ServiceInformation svcInfo = new ServiceInformation();
		svcInfo.setServiceId(serviceId);
		svcInfo.setParticipantId(participantId);
		
		ProcessList processList = new ProcessList();

		ProcessIdentifier selectedProcId = new ProcessIdentifier("PROCID_1");
		ProcessInfo procInfo = new ProcessInfo();		
		procInfo.setProcessId(selectedProcId);
		processList.addProcessInfo(procInfo);
		EndpointInfo epInf = new EndpointInfo("test-1", new URL("http://this.is.a.result"));
		processList.addEndpoint(epInf);
		EndpointInfo ep2Inf = new EndpointInfo("test-2", new URL("http://this.is.another.result"));
		processList.addEndpoint(ep2Inf);
		
		svcInfo.addProcessInformation(processList);

		processList = new ProcessList();
		ProcessIdentifier procId = new ProcessIdentifier("PROCID_2");
		procInfo = new ProcessInfo();
		procInfo.setProcessId(procId);	
		processList.addProcessInfo(procInfo);
		svcInfo.addProcessInformation(processList);
		
		try {
			List<EndpointInfo> endpoints = new SMPClientBuilder()
													.setSMPLocator(new StaticLocator("http://localhost"))
													.setRequestExecutor(new TestDocExecutor())
													.addProcessor(new MockResultProcessor(TestDocExecutor.TEST_NS_URI, svcInfo))
													.build()
													.getEndpoints(participantId, serviceId, selectedProcId);
						
			assertFalse(Utils.isNullOrEmpty(endpoints));
			assertEquals(2, endpoints.size());
			assertTrue(endpoints.stream().allMatch(ep -> ( ep.getEndpointURL().equals(epInf.getEndpointURL())
															&& ep.getTransportProfile().equals(epInf.getTransportProfile())
													 	) || (ep.getEndpointURL().equals(ep2Inf.getEndpointURL())
													 			&& ep.getTransportProfile().equals(ep2Inf.getTransportProfile()))));			
		} catch (SMPQueryException e) {
			fail();
		}
	}

	@Test
	void testGetEndpointsForProcessAndRole() throws MalformedURLException {
		
		Identifier participantId = new Identifier("PARTID_1", "test:scheme"); 
		Identifier serviceId = new Identifier("SVCID_1"); 
		ServiceInformation svcInfo = new ServiceInformation();
		svcInfo.setServiceId(serviceId);
		svcInfo.setParticipantId(participantId);
		
		ProcessList processList = new ProcessList();

		ProcessIdentifier procId = new ProcessIdentifier("PROCID_1");
		Identifier roleId = new Identifier("ROLE_1");
		ProcessInfo procInfo = new ProcessInfo();		
		procInfo.setProcessId(procId);
		procInfo.addRole(roleId);
		procInfo.addRole(new Identifier("ROLE_11"));
		processList.addProcessInfo(procInfo);
		
		EndpointInfo epInf = new EndpointInfo("test-1", new URL("http://this.is.a.result"));
		processList.addEndpoint(epInf);
		EndpointInfo ep2Inf = new EndpointInfo("test-2", new URL("http://this.is.another.result"));
		processList.addEndpoint(ep2Inf);
		
		svcInfo.addProcessInformation(processList);
		
		processList = new ProcessList();
		procInfo = new ProcessInfo();
		procInfo.setProcessId(new ProcessIdentifier("PROCID_2"));
		procInfo.addRole(new Identifier("ROLE_2"));
		processList.addProcessInfo(procInfo);
		svcInfo.addProcessInformation(processList);
		
		try {
			List<EndpointInfo> endpoints = new SMPClientBuilder()
					.setSMPLocator(new StaticLocator("http://localhost"))
					.setRequestExecutor(new TestDocExecutor())
					.addProcessor(new MockResultProcessor(TestDocExecutor.TEST_NS_URI, svcInfo))
					.build()
					.getEndpoints(participantId, roleId, serviceId, procId);
			
			assertFalse(Utils.isNullOrEmpty(endpoints));
			assertEquals(2, endpoints.size());
			assertTrue(endpoints.stream().allMatch(ep -> ( ep.getEndpointURL().equals(epInf.getEndpointURL())
					&& ep.getTransportProfile().equals(epInf.getTransportProfile())
					) || (ep.getEndpointURL().equals(ep2Inf.getEndpointURL())
							&& ep.getTransportProfile().equals(ep2Inf.getTransportProfile()))));			
		} catch (SMPQueryException e) {
			fail();
		}
	}
	
	@Test
	void testInvalidNullArgs() {
		
		Identifier participantId = new Identifier("PARTID_1", "test:scheme"); 
		Identifier serviceId = new Identifier("SVCID_1"); 		
		ProcessIdentifier procId = new ProcessIdentifier("PROCID_2");

		try {
			new SMPClientBuilder()
					.setSMPLocator(new StaticLocator("http://localhost"))
					.setRequestExecutor(new TestDocExecutor())
					.addProcessor(new MockResultProcessor(TestDocExecutor.TEST_NS_URI))
					.build()
					.getEndpoints(null, serviceId, procId);
			fail();
		} catch (IllegalArgumentException e) {			
		} catch (SMPQueryException e) {
			fail();
		}
		try {
			new SMPClientBuilder()
					.setSMPLocator(new StaticLocator("http://localhost"))
					.setRequestExecutor(new TestDocExecutor())
					.addProcessor(new MockResultProcessor(TestDocExecutor.TEST_NS_URI))
					.build()
					.getEndpoints(participantId, null, procId);
			fail();
		} catch (IllegalArgumentException e) {			
		} catch (SMPQueryException e) {
			fail();
		}
		try {
			new SMPClientBuilder()
					.setSMPLocator(new StaticLocator("http://localhost"))
					.setRequestExecutor(new TestDocExecutor())
					.addProcessor(new MockResultProcessor(TestDocExecutor.TEST_NS_URI))
					.build()
					.getEndpoint(participantId, null, procId, null);
			fail();
		} catch (IllegalArgumentException e) {			
		} catch (SMPQueryException e) {
			fail();
		}
	}	
	
	@Test
	void testGetEndpoint() throws MalformedURLException {
		
		Identifier participantId = new Identifier("PARTID_1", "test:scheme"); 
		Identifier serviceId = new Identifier("SVCID_1"); 
		ServiceInformation svcInfo = new ServiceInformation();
		svcInfo.setServiceId(serviceId);
		svcInfo.setParticipantId(participantId);
		
		ProcessList processList = new ProcessList();
		ProcessIdentifier selectedProcId = new ProcessIdentifier("PROCID_1");
		ProcessInfo procInfo = new ProcessInfo();		
		procInfo.setProcessId(selectedProcId);		
		processList.addProcessInfo(procInfo);
		EndpointInfo epInf = new EndpointInfo("test-1", new URL("http://this.is.a.result"));
		processList.addEndpoint(epInf);
		EndpointInfo ep2Inf = new EndpointInfo("test-2", new URL("http://this.is.another.result"));
		processList.addEndpoint(ep2Inf);
		
		svcInfo.addProcessInformation(processList);

		processList = new ProcessList();
		ProcessIdentifier procId = new ProcessIdentifier("PROCID_2");
		procInfo = new ProcessInfo();
		procInfo.setProcessId(procId);	
		processList.addProcessInfo(procInfo);
		svcInfo.addProcessInformation(processList);
		
		try {
			EndpointInfo endpoint = new SMPClientBuilder()
									.setSMPLocator(new StaticLocator("http://localhost"))
									.setRequestExecutor(new TestDocExecutor())
									.addProcessor(new MockResultProcessor(TestDocExecutor.TEST_NS_URI, svcInfo))
									.build()
									.getEndpoint(participantId, serviceId, selectedProcId, ep2Inf.getTransportProfile());
						
			assertNotNull(endpoint);
			assertEquals(ep2Inf.getEndpointURL(), endpoint.getEndpointURL());
			assertEquals(ep2Inf.getTransportProfile(), endpoint.getTransportProfile());			
		} catch (SMPQueryException e) {
			fail();
		}
	}	
	
	@Test
	void testGetEndpointNotFound() throws MalformedURLException {
		
		Identifier participantId = new Identifier("PARTID_1", "test:scheme"); 
		Identifier serviceId = new Identifier("SVCID_1"); 
		ServiceInformation svcInfo = new ServiceInformation();
		svcInfo.setServiceId(serviceId);
		svcInfo.setParticipantId(participantId);
		
		ProcessList processList = new ProcessList();
		ProcessIdentifier selectedProcId = new ProcessIdentifier("PROCID_1");
		ProcessInfo procInfo = new ProcessInfo();		
		procInfo.setProcessId(selectedProcId);		
		processList.addProcessInfo(procInfo);
		EndpointInfo epInf = new EndpointInfo("test-1", new URL("http://this.is.a.result"));
		processList.addEndpoint(epInf);
		EndpointInfo ep2Inf = new EndpointInfo("test-2", new URL("http://this.is.another.result"));
		processList.addEndpoint(ep2Inf);
		
		svcInfo.addProcessInformation(processList);

		processList = new ProcessList();
		ProcessIdentifier procId = new ProcessIdentifier("PROCID_2");
		procInfo = new ProcessInfo();
		procInfo.setProcessId(procId);	
		processList.addProcessInfo(procInfo);
		svcInfo.addProcessInformation(processList);
		
		try {
			EndpointInfo endpoint = new SMPClientBuilder()
									.setSMPLocator(new StaticLocator("http://localhost"))
									.setRequestExecutor(new TestDocExecutor())
									.addProcessor(new MockResultProcessor(TestDocExecutor.TEST_NS_URI, svcInfo))
									.build()
									.getEndpoint(participantId, serviceId, selectedProcId, "test-3");
						
			assertNull(endpoint);
		} catch (SMPQueryException e) {
			fail();
		}
	}	
	
	@Test
	void testGetEndpointsForNoProcess() throws MalformedURLException {
		
		Identifier participantId = new Identifier("PARTID_1", "test:scheme"); 
		Identifier serviceId = new Identifier("SVCID_1"); 
		ServiceInformation svcInfo = new ServiceInformation();
		svcInfo.setServiceId(serviceId);
		svcInfo.setParticipantId(participantId);
		
		ProcessList processList = new ProcessList();
		ProcessIdentifier selectedProcId = new ProcessIdentifier();
		ProcessInfo procInfo = new ProcessInfo();		
		procInfo.setProcessId(selectedProcId);		
		processList.addProcessInfo(procInfo);
		EndpointInfo epInf = new EndpointInfo("test-1", new URL("http://this.is.a.result"));
		processList.addEndpoint(epInf);
		EndpointInfo ep2Inf = new EndpointInfo("test-2", new URL("http://this.is.another.result"));
		processList.addEndpoint(ep2Inf);
		
		svcInfo.addProcessInformation(processList);

		processList = new ProcessList();
		ProcessIdentifier procId = new ProcessIdentifier("PROCID_2");
		procInfo = new ProcessInfo();
		procInfo.setProcessId(procId);	
		processList.addProcessInfo(procInfo);
		svcInfo.addProcessInformation(processList);
		
		try {
			List<EndpointInfo> endpoints = new SMPClientBuilder()
													.setSMPLocator(new StaticLocator("http://localhost"))
													.setRequestExecutor(new TestDocExecutor())
													.addProcessor(new MockResultProcessor(TestDocExecutor.TEST_NS_URI, svcInfo))
													.build()
													.getEndpoints(participantId, serviceId, new ProcessIdentifier());
						
			assertFalse(Utils.isNullOrEmpty(endpoints));
			assertEquals(2, endpoints.size());
			assertTrue(endpoints.stream().allMatch(ep -> ( ep.getEndpointURL().equals(epInf.getEndpointURL())
															&& ep.getTransportProfile().equals(epInf.getTransportProfile())
													 	) || (ep.getEndpointURL().equals(ep2Inf.getEndpointURL())
													 			&& ep.getTransportProfile().equals(ep2Inf.getTransportProfile()))));			
		} catch (SMPQueryException e) {
			fail();
		}
	}	

	@Test
	void testGetEndpointsIgnoreEmptyProcess() throws MalformedURLException {
		
		Identifier participantId = new Identifier("PARTID_1", "test:scheme"); 
		Identifier serviceId = new Identifier("SVCID_1"); 
		ServiceInformation svcInfo = new ServiceInformation();
		svcInfo.setServiceId(serviceId);
		svcInfo.setParticipantId(participantId);
		
		ProcessList processList = new ProcessList();
		ProcessIdentifier selectedProcId = new ProcessIdentifier();
		ProcessInfo procInfo = new ProcessInfo();		
		procInfo.setProcessId(selectedProcId);		
		processList.addProcessInfo(procInfo);
		EndpointInfo epInf = new EndpointInfo("test-1", new URL("http://this.is.a.result"));
		processList.addEndpoint(epInf);
		EndpointInfo ep2Inf = new EndpointInfo("test-2", new URL("http://this.is.another.result"));
		processList.addEndpoint(ep2Inf);		
		svcInfo.addProcessInformation(processList);

		processList = new ProcessList();
		procInfo = new ProcessInfo();
		processList.addProcessInfo(procInfo);
		svcInfo.addProcessInformation(processList);
		
		try {
			List<EndpointInfo> endpoints = new SMPClientBuilder()
													.setSMPLocator(new StaticLocator("http://localhost"))
													.setRequestExecutor(new TestDocExecutor())
													.addProcessor(new MockResultProcessor(TestDocExecutor.TEST_NS_URI, svcInfo))
													.build()
													.getEndpoints(participantId, serviceId, selectedProcId);
						
			assertFalse(Utils.isNullOrEmpty(endpoints));
			assertEquals(2, endpoints.size());
			assertTrue(endpoints.stream().allMatch(ep -> ( ep.getEndpointURL().equals(epInf.getEndpointURL())
															&& ep.getTransportProfile().equals(epInf.getTransportProfile())
													 	) || (ep.getEndpointURL().equals(ep2Inf.getEndpointURL())
													 			&& ep.getTransportProfile().equals(ep2Inf.getTransportProfile()))));			
		} catch (SMPQueryException e) {
			fail();
		}
	}
	
	@Test
	void testGetEndpointsIgnoreEmptyRoles() throws MalformedURLException {
		
		Identifier participantId = new Identifier("PARTID_1", "test:scheme"); 
		Identifier serviceId = new Identifier("SVCID_1"); 
		ServiceInformation svcInfo = new ServiceInformation();
		svcInfo.setServiceId(serviceId);
		svcInfo.setParticipantId(participantId);
		
		ProcessIdentifier procId = new ProcessIdentifier("PROCID_1");
		Identifier roleId = new Identifier("ROLE_1");
		ProcessList processList = new ProcessList();
		ProcessInfo procInfo = new ProcessInfo();		
		procInfo.setProcessId(procId);
		procInfo.addRole(roleId);
		procInfo.addRole(new Identifier("ROLE_11"));
		EndpointInfo epInf = new EndpointInfo("test-1", new URL("http://this.is.a.result"));
		processList.addEndpoint(epInf);
		EndpointInfo ep2Inf = new EndpointInfo("test-2", new URL("http://this.is.another.result"));
		processList.addEndpoint(ep2Inf);		
		svcInfo.addProcessInformation(processList);

		processList = new ProcessList();
		procInfo = new ProcessInfo();
		procInfo.setProcessId(new ProcessIdentifier("PROCID_2"));
		processList.addProcessInfo(procInfo);
		svcInfo.addProcessInformation(processList);
		
		try {
			List<EndpointInfo> endpoints = new SMPClientBuilder()
					.setSMPLocator(new StaticLocator("http://localhost"))
					.setRequestExecutor(new TestDocExecutor())
					.addProcessor(new MockResultProcessor(TestDocExecutor.TEST_NS_URI, svcInfo))
					.build()
					.getEndpoints(participantId, roleId, serviceId, procId);
			
			assertFalse(Utils.isNullOrEmpty(endpoints));
			assertEquals(2, endpoints.size());
			assertTrue(endpoints.stream().allMatch(ep -> ( ep.getEndpointURL().equals(epInf.getEndpointURL())
					&& ep.getTransportProfile().equals(epInf.getTransportProfile())
					) || (ep.getEndpointURL().equals(ep2Inf.getEndpointURL())
							&& ep.getTransportProfile().equals(ep2Inf.getTransportProfile()))));			
		} catch (SMPQueryException e) {
			fail();
		}
	}	
	
	@Test
	void testGetEndpointsMatchToEmptyProcId() throws MalformedURLException {
		
		Identifier participantId = new Identifier("PARTID_1", "test:scheme"); 
		Identifier serviceId = new Identifier("SVCID_1"); 
		ServiceInformation svcInfo = new ServiceInformation();
		svcInfo.setServiceId(serviceId);
		svcInfo.setParticipantId(participantId);
		
		ProcessList processList = new ProcessList();
		ProcessInfo procInfo = new ProcessInfo();		
		procInfo.addRole(new Identifier("ROLE_11"));
		EndpointInfo epInf = new EndpointInfo("test-1", new URL("http://this.is.a.result"));
		processList.addEndpoint(epInf);
		EndpointInfo ep2Inf = new EndpointInfo("test-2", new URL("http://this.is.another.result"));
		processList.addEndpoint(ep2Inf);		
		svcInfo.addProcessInformation(processList);

		processList = new ProcessList();
		procInfo = new ProcessInfo();
		procInfo.setProcessId(new ProcessIdentifier("PROCID_2"));
		processList.addProcessInfo(procInfo);
		svcInfo.addProcessInformation(processList);
		
		try {
			List<EndpointInfo> endpoints = new SMPClientBuilder()
					.setSMPLocator(new StaticLocator("http://localhost"))
					.setRequestExecutor(new TestDocExecutor())
					.addProcessor(new MockResultProcessor(TestDocExecutor.TEST_NS_URI, svcInfo))
					.build()
					.getEndpoints(participantId, serviceId, new ProcessIdentifier("MATCH_EMPTY"));
			
			assertFalse(Utils.isNullOrEmpty(endpoints));
			assertEquals(2, endpoints.size());
			assertTrue(endpoints.stream().allMatch(ep -> ( ep.getEndpointURL().equals(epInf.getEndpointURL())
					&& ep.getTransportProfile().equals(epInf.getTransportProfile())
					) || (ep.getEndpointURL().equals(ep2Inf.getEndpointURL())
							&& ep.getTransportProfile().equals(ep2Inf.getTransportProfile()))));			
		} catch (SMPQueryException e) {
			fail();
		}
	}
	
	@Test
	void testGetEndpointsMatchEmptyRoles() throws MalformedURLException {
		
		Identifier participantId = new Identifier("PARTID_1", "test:scheme"); 
		Identifier serviceId = new Identifier("SVCID_1"); 
		ServiceInformation svcInfo = new ServiceInformation();
		svcInfo.setServiceId(serviceId);
		svcInfo.setParticipantId(participantId);
		
		ProcessList processList = new ProcessList();
		ProcessIdentifier procId = new ProcessIdentifier("PROCID_1");
		ProcessInfo procInfo = new ProcessInfo();		
		procInfo.setProcessId(procId);
		processList.addProcessInfo(procInfo);
		EndpointInfo epInf = new EndpointInfo("test-1", new URL("http://this.is.a.result"));
		processList.addEndpoint(epInf);
		EndpointInfo ep2Inf = new EndpointInfo("test-2", new URL("http://this.is.another.result"));
		processList.addEndpoint(ep2Inf);
		
		svcInfo.addProcessInformation(processList);
		
		processList = new ProcessList();
		procInfo = new ProcessInfo();
		procInfo.setProcessId(new ProcessIdentifier("PROCID_2"));
		procInfo.addRole(new Identifier("ROLE_2"));
		processList.addProcessInfo(procInfo);
		svcInfo.addProcessInformation(processList);
		
		try {
			List<EndpointInfo> endpoints = new SMPClientBuilder()
					.setSMPLocator(new StaticLocator("http://localhost"))
					.setRequestExecutor(new TestDocExecutor())
					.addProcessor(new MockResultProcessor(TestDocExecutor.TEST_NS_URI, svcInfo))
					.build()
					.getEndpoints(participantId, new Identifier("MATCH_EMPTY"), serviceId, procId);
			
			assertFalse(Utils.isNullOrEmpty(endpoints));
			assertEquals(2, endpoints.size());
			assertTrue(endpoints.stream().allMatch(ep -> ( ep.getEndpointURL().equals(epInf.getEndpointURL())
					&& ep.getTransportProfile().equals(epInf.getTransportProfile())
					) || (ep.getEndpointURL().equals(ep2Inf.getEndpointURL())
							&& ep.getTransportProfile().equals(ep2Inf.getTransportProfile()))));			
		} catch (SMPQueryException e) {
			fail();
		}
	}
	
	@Test
	void testGetEndpointsIgnoreCatchAllProcInfo() throws MalformedURLException {
		
		Identifier participantId = new Identifier("PARTID_1", "test:scheme"); 
		Identifier serviceId = new Identifier("SVCID_1"); 
		ServiceInformation svcInfo = new ServiceInformation();
		svcInfo.setServiceId(serviceId);
		svcInfo.setParticipantId(participantId);
		
		ProcessIdentifier procId = new ProcessIdentifier("PROCID_1");
		Identifier roleId = new Identifier("ROLE_1");
		ProcessList processList = new ProcessList();
		ProcessInfo procInfo = new ProcessInfo();		
		procInfo.setProcessId(procId);
		procInfo.addRole(roleId);
		procInfo.addRole(new Identifier("ROLE_11"));
		EndpointInfo epInf = new EndpointInfo("test-1", new URL("http://this.is.a.result"));
		processList.addEndpoint(epInf);
		EndpointInfo ep2Inf = new EndpointInfo("test-2", new URL("http://this.is.another.result"));
		processList.addEndpoint(ep2Inf);		
		svcInfo.addProcessInformation(processList);

		processList = new ProcessList();
		procInfo = new ProcessInfo();
		processList.addProcessInfo(procInfo);
		svcInfo.addProcessInformation(processList);
		
		try {
			List<EndpointInfo> endpoints = new SMPClientBuilder()
					.setSMPLocator(new StaticLocator("http://localhost"))
					.setRequestExecutor(new TestDocExecutor())
					.addProcessor(new MockResultProcessor(TestDocExecutor.TEST_NS_URI, svcInfo))
					.build()
					.getEndpoints(participantId, roleId, serviceId, procId);
			
			assertFalse(Utils.isNullOrEmpty(endpoints));
			assertEquals(2, endpoints.size());
			assertTrue(endpoints.stream().allMatch(ep -> ( ep.getEndpointURL().equals(epInf.getEndpointURL())
					&& ep.getTransportProfile().equals(epInf.getTransportProfile())
					) || (ep.getEndpointURL().equals(ep2Inf.getEndpointURL())
							&& ep.getTransportProfile().equals(ep2Inf.getTransportProfile()))));			
		} catch (SMPQueryException e) {
			fail();
		}
	}	
}
