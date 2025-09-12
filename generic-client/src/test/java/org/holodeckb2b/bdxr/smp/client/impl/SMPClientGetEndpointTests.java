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

import java.net.MalformedURLException;
import java.net.URL;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Set;

import org.holodeckb2b.bdxr.common.datamodel.Identifier;
import org.holodeckb2b.bdxr.common.datamodel.impl.IdentifierImpl;
import org.holodeckb2b.bdxr.common.datamodel.impl.ProcessIdentifierImpl;
import org.holodeckb2b.bdxr.smp.client.api.SMPClientBuilder;
import org.holodeckb2b.bdxr.smp.client.api.SMPQueryException;
import org.holodeckb2b.bdxr.smp.datamodel.EndpointInfo;
import org.holodeckb2b.bdxr.smp.datamodel.ProcessInfo;
import org.holodeckb2b.bdxr.smp.datamodel.Redirection;
import org.holodeckb2b.bdxr.smp.datamodel.ServiceMetadata;
import org.holodeckb2b.bdxr.smp.datamodel.impl.EndpointInfoV1Impl;
import org.holodeckb2b.bdxr.smp.datamodel.impl.ProcessGroupImpl;
import org.holodeckb2b.bdxr.smp.datamodel.impl.ProcessInfoImpl;
import org.holodeckb2b.bdxr.smp.datamodel.impl.RedirectionV2Impl;
import org.holodeckb2b.bdxr.smp.datamodel.impl.ServiceMetadataImpl;
import org.holodeckb2b.brdx.smp.testhelpers.MockRequestExecutor;
import org.holodeckb2b.brdx.smp.testhelpers.MockResultProcessor;
import org.holodeckb2b.commons.util.Utils;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class SMPClientGetEndpointTests {
	private static final Identifier P_ID = new IdentifierImpl("PARTID_1", "test:scheme");
	private static final Identifier SVC_ID = new IdentifierImpl("SVCID_1");

	@Test
	void testOneMatchingPg() throws MalformedURLException {
		ProcessInfo proc = new ProcessInfoImpl(new ProcessIdentifierImpl("PROCID_1"), null);

		EndpointInfo ep1 = new EndpointInfoImpl("test-1", new URL("http://this.is.a.result"));
		EndpointInfo ep2 = new EndpointInfoImpl("test-2", new URL("http://this.is.another.result"));

		ServiceMetadata smd = new ServiceMetadataImpl(P_ID, SVC_ID,
										Set.of(new ProcessGroupImpl(Set.of(proc), Set.of(ep1, ep2), null))
										, null);

		String docNS = "http://test.holodeck-b2b.org/smp/ns/oneMatch";

		Collection<? extends EndpointInfo> endpoints = assertDoesNotThrow(() ->
				new SMPClientBuilder().setSMPLocator(new StaticLocator("http://localhost"))
										.setRequestExecutor(new MockRequestExecutor().addResponse(200, null, docNS))
										.addProcessor(new MockResultProcessor(docNS, smd))
										.build()
										.getEndpoints(P_ID, SVC_ID, proc.getProcessId()));

		assertFalse(Utils.isNullOrEmpty(endpoints));
		assertEquals(2, endpoints.size());
		assertTrue(endpoints.stream().allMatch(ep -> ep.equals(ep1) || ep.equals(ep2)));
	}

	@Test
	void testMatchOnDefaultProc() throws MalformedURLException {
		ProcessInfo proc1 = new ProcessInfoImpl(new ProcessIdentifierImpl("PROCID_1"), null);

		EndpointInfo ep1 = new EndpointInfoImpl("test-1", new URL("http://this.is.a.result"));
		EndpointInfo ep2 = new EndpointInfoImpl("test-2", new URL("http://this.is.another.result"));

		ServiceMetadata smd = new ServiceMetadataImpl(P_ID, SVC_ID,
										Set.of(new ProcessGroupImpl(Set.of(proc1), Set.of(ep1), null),
											   new ProcessGroupImpl(null, Set.of(ep2), null))
										, null);

		String docNS = "http://test.holodeck-b2b.org/smp/ns/onRole";

		Collection<? extends EndpointInfo> endpoints = assertDoesNotThrow(() ->
				new SMPClientBuilder().setSMPLocator(new StaticLocator("http://localhost"))
										.setRequestExecutor(new MockRequestExecutor().addResponse(200, null, docNS))
										.addProcessor(new MockResultProcessor(docNS, smd))
										.build()
										.getEndpoints(P_ID, SVC_ID, new ProcessIdentifierImpl("PROCID_2")));

		assertFalse(Utils.isNullOrEmpty(endpoints));
		assertEquals(1, endpoints.size());
		assertTrue(endpoints.iterator().next().equals(ep2));
	}

	@Test
	void testMatchOnRole() throws MalformedURLException {
		Identifier role = new IdentifierImpl("role");
		ProcessInfo proc1 = new ProcessInfoImpl(new ProcessIdentifierImpl("PROCID_1"), null);
		ProcessInfo proc2 = new ProcessInfoImpl(new ProcessIdentifierImpl("PROCID_1"), Set.of(role),null);
		ProcessInfo proc3 = new ProcessInfoImpl(new ProcessIdentifierImpl("PROCID_1"),
												Set.of(new IdentifierImpl("AnotherRole")),null);

		EndpointInfo ep1 = new EndpointInfoImpl("test-1", new URL("http://this.is.a.result"));
		EndpointInfo ep2 = new EndpointInfoImpl("test-2", new URL("http://this.is.another.result"));

		ServiceMetadata smd = new ServiceMetadataImpl(P_ID, SVC_ID,
										Set.of(new ProcessGroupImpl(Set.of(proc1, proc3), Set.of(ep1), null),
											   new ProcessGroupImpl(Set.of(proc2), Set.of(ep2), null))
										, null);

		String docNS = "http://test.holodeck-b2b.org/smp/ns/onRole";

		Collection<? extends EndpointInfo> endpoints = assertDoesNotThrow(() ->
				new SMPClientBuilder().setSMPLocator(new StaticLocator("http://localhost"))
										.setRequestExecutor(new MockRequestExecutor().addResponse(200, null, docNS))
										.addProcessor(new MockResultProcessor(docNS, smd))
										.build()
										.getEndpoints(P_ID, role, SVC_ID, proc2.getProcessId()));

		assertFalse(Utils.isNullOrEmpty(endpoints));
		assertEquals(1, endpoints.size());
		assertTrue(endpoints.iterator().next().equals(ep2));
	}

	@Test
	void testMatchOnDefaultRole() throws MalformedURLException {
		ProcessInfo proc1 = new ProcessInfoImpl(new ProcessIdentifierImpl("PROCID_1"), null);
		ProcessInfo proc2 = new ProcessInfoImpl(new ProcessIdentifierImpl("PROCID_1"),
												Set.of(new IdentifierImpl("AnotherRole")),null);
		ProcessInfo proc3 = new ProcessInfoImpl(new ProcessIdentifierImpl("PROCID_2"),
												Set.of(new IdentifierImpl("AnotherRole")),null);

		EndpointInfo ep1 = new EndpointInfoImpl("test-1", new URL("http://this.is.a.result"));
		EndpointInfo ep2 = new EndpointInfoImpl("test-2", new URL("http://this.is.another.result"));

		ServiceMetadata smd = new ServiceMetadataImpl(P_ID, SVC_ID,
										Set.of(new ProcessGroupImpl(Set.of(proc1, proc3), Set.of(ep1), null),
											   new ProcessGroupImpl(Set.of(proc2), Set.of(ep2), null))
										, null);

		String docNS = "http://test.holodeck-b2b.org/smp/ns/onDefaultRole";

		Collection<? extends EndpointInfo> endpoints = assertDoesNotThrow(() ->
				new SMPClientBuilder().setSMPLocator(new StaticLocator("http://localhost"))
										.setRequestExecutor(new MockRequestExecutor().addResponse(200, null, docNS))
										.addProcessor(new MockResultProcessor(docNS, smd))
										.build()
										.getEndpoints(P_ID, new IdentifierImpl("Role"), SVC_ID, proc1.getProcessId()));

		assertFalse(Utils.isNullOrEmpty(endpoints));
		assertEquals(1, endpoints.size());
		assertTrue(endpoints.iterator().next().equals(ep1));
	}

	@Test
	void testOnTransportProfile() throws MalformedURLException {
		ProcessInfo proc = new ProcessInfoImpl(new ProcessIdentifierImpl("PROCID_1"), null);

		EndpointInfo ep1 = new EndpointInfoImpl("test-1", new URL("http://this.is.a.result"));
		EndpointInfo ep2 = new EndpointInfoImpl("test-2", new URL("http://this.is.another.result"));

		ServiceMetadata smd = new ServiceMetadataImpl(P_ID, SVC_ID,
										Set.of(new ProcessGroupImpl(Set.of(proc), Set.of(ep1, ep2), null)), null);

		String docNS = "http://test.holodeck-b2b.org/smp/ns/onTransport";

		EndpointInfo endpoint = assertDoesNotThrow(() ->
				new SMPClientBuilder().setSMPLocator(new StaticLocator("http://localhost"))
										.setRequestExecutor(new MockRequestExecutor().addResponse(200, null, docNS))
										.addProcessor(new MockResultProcessor(docNS, smd))
										.build()
										.getEndpoint(P_ID, SVC_ID, proc.getProcessId(), ep1.getTransportProfile()));

		assertNotNull(endpoint);
		assertEquals(ep1.getEndpointURL(), endpoint.getEndpointURL());
	}

	@Test
	void testActive() throws MalformedURLException {
		ProcessInfo proc = new ProcessInfoImpl(new ProcessIdentifierImpl("PROCID_1"), null);

		EndpointInfoImpl ep1 = new EndpointInfoImpl("test-1", new URL("http://this.is.a.result"));
		ep1.setServiceActivationDate(ZonedDateTime.now().minusMonths(2));
		ep1.setServiceExpirationDate(ZonedDateTime.now().plusMonths(2));
		EndpointInfoImpl ep2 = new EndpointInfoImpl("test-1", new URL("http://this.is.another.result"));
		ep2.setServiceActivationDate(ZonedDateTime.now().plusMonths(2));

		ServiceMetadata smd = new ServiceMetadataImpl(P_ID, SVC_ID,
										Set.of(new ProcessGroupImpl(Set.of(proc), Set.of(ep1, ep2), null)), null);

		String docNS = "http://test.holodeck-b2b.org/smp/ns/onActive";

		EndpointInfo endpoint = assertDoesNotThrow(() ->
				new SMPClientBuilder().setSMPLocator(new StaticLocator("http://localhost"))
										.setRequestExecutor(new MockRequestExecutor().addResponse(200, null, docNS))
										.addProcessor(new MockResultProcessor(docNS, smd))
										.build()
										.getEndpoint(P_ID, SVC_ID, proc.getProcessId(), "test-1"));

		assertNotNull(endpoint);
		assertEquals(ep1.getEndpointURL(), endpoint.getEndpointURL());
	}

	@Test
	void testNoMatchTransportProfile() throws MalformedURLException {
		ProcessInfo proc = new ProcessInfoImpl(new ProcessIdentifierImpl("PROCID_1"), null);

		EndpointInfo ep1 = new EndpointInfoImpl("test-1", new URL("http://this.is.a.result"));
		EndpointInfo ep2 = new EndpointInfoImpl("test-2", new URL("http://this.is.another.result"));

		ServiceMetadata smd = new ServiceMetadataImpl(P_ID, SVC_ID,
										Set.of(new ProcessGroupImpl(Set.of(proc), Set.of(ep1, ep2), null)), null);

		String docNS = "http://test.holodeck-b2b.org/smp/ns/onTransport";

		EndpointInfo endpoint = assertDoesNotThrow(() ->
				new SMPClientBuilder().setSMPLocator(new StaticLocator("http://localhost"))
										.setRequestExecutor(new MockRequestExecutor().addResponse(200, null, docNS))
										.addProcessor(new MockResultProcessor(docNS, smd))
										.build()
										.getEndpoint(P_ID, SVC_ID, proc.getProcessId(), "profile-3"));

		assertNull(endpoint);
	}

	@Test
	void testNotFound() {
		assertTrue(assertDoesNotThrow(() ->
				new SMPClientBuilder().setSMPLocator(new StaticLocator("http://localhost"))
										.setRequestExecutor(new MockRequestExecutor().addResponse(404, null, null))
										.addProcessor(new MockResultProcessor(null, null))
										.build()
										.getEndpoints(P_ID, new IdentifierImpl("Role"), SVC_ID,
													  new ProcessIdentifierImpl("PROC_ID"))).isEmpty());
	}

	@Test
	void testNoMatches() throws MalformedURLException {
		ProcessInfo proc1 = new ProcessInfoImpl(new ProcessIdentifierImpl("PROCID_1"), null);
		ProcessInfo proc2 = new ProcessInfoImpl(new ProcessIdentifierImpl("PROCID_1"),
												Set.of(new IdentifierImpl("AnotherRole")),null);
		ProcessInfo proc3 = new ProcessInfoImpl(new ProcessIdentifierImpl("PROCID_2"),
												Set.of(new IdentifierImpl("AnotherRole")),null);

		EndpointInfo ep1 = new EndpointInfoImpl("test-1", new URL("http://this.is.a.result"));
		EndpointInfo ep2 = new EndpointInfoImpl("test-2", new URL("http://this.is.another.result"));

		ServiceMetadata smd = new ServiceMetadataImpl(P_ID, SVC_ID,
										Set.of(new ProcessGroupImpl(Set.of(proc1, proc3), Set.of(ep1), null),
											   new ProcessGroupImpl(Set.of(proc2), Set.of(ep2), null))
										, null);

		String docNS = "http://test.holodeck-b2b.org/smp/ns/onDefaultRole";

		assertTrue(assertDoesNotThrow(() ->
				new SMPClientBuilder().setSMPLocator(new StaticLocator("http://localhost"))
										.setRequestExecutor(new MockRequestExecutor().addResponse(200, null, docNS))
										.addProcessor(new MockResultProcessor(docNS, smd))
										.build()
										.getEndpoints(P_ID, new IdentifierImpl("Role"), SVC_ID,
														new ProcessIdentifierImpl("PROCID_3"))).isEmpty());
	}

	@Test
	void testRedirection() throws MalformedURLException {
		ProcessInfo proc = new ProcessInfoImpl(new ProcessIdentifierImpl("PROCID_1"), null);

		Redirection  r = new RedirectionV2Impl(new URL("http://this.is.another.smp"));
		EndpointInfo ep1 = new EndpointInfoImpl("test-1", new URL("http://this.is.a.result"));
		EndpointInfo ep2 = new EndpointInfoImpl("test-2", new URL("http://this.is.another.result"));

		ServiceMetadata smd1 = new ServiceMetadataImpl(P_ID, SVC_ID,
										Set.of(new ProcessGroupImpl(Set.of(proc), r, null))
										, null);
		ServiceMetadata smd2 = new ServiceMetadataImpl(P_ID, SVC_ID,
										Set.of(new ProcessGroupImpl(Set.of(proc), Set.of(ep1, ep2), null))
										, null);

		String docNS1 = "http://test.holodeck-b2b.org/smp/ns/redirection";
		String docNS2 = "http://test.holodeck-b2b.org/smp/ns/redirected";

		MockRequestExecutor reqExecutor = new MockRequestExecutor();
		Collection<? extends EndpointInfo> endpoints = assertDoesNotThrow(() ->
				new SMPClientBuilder().setSMPLocator(new StaticLocator("http://localhost"))
										.setRequestExecutor(reqExecutor
															.addResponse(200, null, docNS1)
															.addResponse(200, null, docNS2))
										.addProcessor(new MockResultProcessor(docNS1, smd1))
										.addProcessor(new MockResultProcessor(docNS2, smd2))
										.build()
										.getEndpoints(P_ID, SVC_ID, proc.getProcessId()));

		assertEquals(2, reqExecutor.getRequestURLs().size());
		assertTrue(reqExecutor.getRequestURLs().get(1).toString().startsWith(r.getNewSMPURL().toString()));

		assertFalse(Utils.isNullOrEmpty(endpoints));
		assertEquals(2, endpoints.size());
		assertTrue(endpoints.stream().allMatch(ep -> ep.equals(ep1) || ep.equals(ep2)));
	}

	@Test
	void testProcGroupRedirectAfterGlobalOne() throws MalformedURLException {
		ProcessInfo proc = new ProcessInfoImpl(new ProcessIdentifierImpl("PROCID_1"), null);

		Redirection  r1 = new RedirectionV2Impl(new URL("http://this.is.another.smp"));
		Redirection  r2 = new RedirectionV2Impl(new URL("http://this.is.yet.another.smp"));
		EndpointInfo ep1 = new EndpointInfoImpl("test-1", new URL("http://this.is.a.result"));
		EndpointInfo ep2 = new EndpointInfoImpl("test-2", new URL("http://this.is.another.result"));

		ServiceMetadata smd1 = new ServiceMetadataImpl(P_ID, SVC_ID,
										Set.of(new ProcessGroupImpl(null, r1, null))
										, null);
		ServiceMetadata smd2 = new ServiceMetadataImpl(P_ID, SVC_ID,
										Set.of(new ProcessGroupImpl(Set.of(proc), r2, null))
										, null);
		ServiceMetadata smd3 = new ServiceMetadataImpl(P_ID, SVC_ID,
										Set.of(new ProcessGroupImpl(Set.of(proc), Set.of(ep1, ep2), null))
										, null);

		String docNS1 = "http://test.holodeck-b2b.org/smp/ns/redirection/global";
		String docNS2 = "http://test.holodeck-b2b.org/smp/ns/redirection/procgroup";
		String docNS3 = "http://test.holodeck-b2b.org/smp/ns/redirected";

		MockRequestExecutor reqExecutor = new MockRequestExecutor();
		Collection<? extends EndpointInfo> endpoints = assertDoesNotThrow(() ->
										new SMPClientBuilder().setSMPLocator(new StaticLocator("http://localhost"))
												.setRequestExecutor(reqExecutor
														.addResponse(200, null, docNS1)
														.addResponse(200, null, docNS2)
														.addResponse(200, null, docNS3))
												.addProcessor(new MockResultProcessor(docNS1, smd1))
												.addProcessor(new MockResultProcessor(docNS2, smd2))
												.addProcessor(new MockResultProcessor(docNS3, smd3))
												.setMaxRedirections(3)
												.build()
												.getEndpoints(P_ID, SVC_ID, proc.getProcessId()));

		assertEquals(3, reqExecutor.getRequestURLs().size());
		assertTrue(reqExecutor.getRequestURLs().get(2).toString().startsWith(r2.getNewSMPURL().toString()));

		assertFalse(Utils.isNullOrEmpty(endpoints));
		assertEquals(2, endpoints.size());
		assertTrue(endpoints.stream().allMatch(ep -> ep.equals(ep1) || ep.equals(ep2)));
	}


	@Test
	void testTooManyRedirectionsOnProcGroup() throws MalformedURLException {
		ProcessInfo proc = new ProcessInfoImpl(new ProcessIdentifierImpl("PROCID_1"), null);

		Redirection  r = new RedirectionV2Impl(new URL("http://this.is.another.smp"));
		EndpointInfo ep1 = new EndpointInfoImpl("test-1", new URL("http://this.is.a.result"));
		EndpointInfo ep2 = new EndpointInfoImpl("test-2", new URL("http://this.is.another.result"));

		ServiceMetadata smd1 = new ServiceMetadataImpl(P_ID, SVC_ID,
										Set.of(new ProcessGroupImpl(Set.of(proc), r, null))
										, null);
		ServiceMetadata smd2 = new ServiceMetadataImpl(P_ID, SVC_ID,
										Set.of(new ProcessGroupImpl(Set.of(proc), Set.of(ep1, ep2), null))
										, null);

		String docNS1 = "http://test.holodeck-b2b.org/smp/ns/redirection";
		String docNS2 = "http://test.holodeck-b2b.org/smp/ns/redirected";

		SMPQueryException ex = assertThrows(SMPQueryException.class, () ->
										new SMPClientBuilder().setSMPLocator(new StaticLocator("http://localhost"))
												.setRequestExecutor(new MockRequestExecutor()
														.addResponse(200, null, docNS1)
														.addResponse(200, null, docNS1)
														.addResponse(200, null, docNS2))
												.addProcessor(new MockResultProcessor(docNS1, smd1))
												.addProcessor(new MockResultProcessor(docNS2, smd2))
												.build()
												.getEndpoints(P_ID, SVC_ID, proc.getProcessId()));

		assertTrue(ex.getMessage().contains("redirections"));
	}

	@Test
	void testTooManyRedirectsAfterGlobalOne() throws MalformedURLException {
		ProcessInfo proc = new ProcessInfoImpl(new ProcessIdentifierImpl("PROCID_1"), null);

		Redirection  r = new RedirectionV2Impl(new URL("http://this.is.another.smp"));
		EndpointInfo ep1 = new EndpointInfoImpl("test-1", new URL("http://this.is.a.result"));
		EndpointInfo ep2 = new EndpointInfoImpl("test-2", new URL("http://this.is.another.result"));

		ServiceMetadata smd1 = new ServiceMetadataImpl(P_ID, SVC_ID,
										Set.of(new ProcessGroupImpl(null, r, null))
										, null);
		ServiceMetadata smd2 = new ServiceMetadataImpl(P_ID, SVC_ID,
										Set.of(new ProcessGroupImpl(Set.of(proc), r, null))
										, null);
		ServiceMetadata smd3 = new ServiceMetadataImpl(P_ID, SVC_ID,
										Set.of(new ProcessGroupImpl(Set.of(proc), Set.of(ep1, ep2), null))
										, null);

		String docNS1 = "http://test.holodeck-b2b.org/smp/ns/redirection/global";
		String docNS2 = "http://test.holodeck-b2b.org/smp/ns/redirection/procgroup";
		String docNS3 = "http://test.holodeck-b2b.org/smp/ns/redirected";

		SMPQueryException ex = assertThrows(SMPQueryException.class, () ->
										new SMPClientBuilder().setSMPLocator(new StaticLocator("http://localhost"))
												.setRequestExecutor(new MockRequestExecutor()
														.addResponse(200, null, docNS1)
														.addResponse(200, null, docNS2)
														.addResponse(200, null, docNS3))
												.addProcessor(new MockResultProcessor(docNS1, smd1))
												.addProcessor(new MockResultProcessor(docNS2, smd2))
												.addProcessor(new MockResultProcessor(docNS3, smd3))
												.build()
												.getEndpoints(P_ID, SVC_ID, proc.getProcessId()));

		assertTrue(ex.getMessage().contains("redirections"));
	}
}
