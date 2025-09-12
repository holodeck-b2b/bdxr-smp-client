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
import java.util.Set;

import org.holodeckb2b.bdxr.common.datamodel.Identifier;
import org.holodeckb2b.bdxr.common.datamodel.impl.IdentifierImpl;
import org.holodeckb2b.bdxr.common.datamodel.impl.ProcessIdentifierImpl;
import org.holodeckb2b.bdxr.smp.client.api.SMPClientBuilder;
import org.holodeckb2b.bdxr.smp.client.api.SMPQueryException;
import org.holodeckb2b.bdxr.smp.datamodel.EndpointInfo;
import org.holodeckb2b.bdxr.smp.datamodel.ProcessInfo;
import org.holodeckb2b.bdxr.smp.datamodel.Redirection;
import org.holodeckb2b.bdxr.smp.datamodel.ServiceGroup;
import org.holodeckb2b.bdxr.smp.datamodel.ServiceMetadata;
import org.holodeckb2b.bdxr.smp.datamodel.impl.EndpointInfoV1Impl;
import org.holodeckb2b.bdxr.smp.datamodel.impl.ProcessGroupImpl;
import org.holodeckb2b.bdxr.smp.datamodel.impl.ProcessInfoImpl;
import org.holodeckb2b.bdxr.smp.datamodel.impl.RedirectionV2Impl;
import org.holodeckb2b.bdxr.smp.datamodel.impl.ServiceGroupV1Impl;
import org.holodeckb2b.bdxr.smp.datamodel.impl.ServiceMetadataImpl;
import org.holodeckb2b.brdx.smp.testhelpers.MockRequestExecutor;
import org.holodeckb2b.brdx.smp.testhelpers.MockResultProcessor;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class SMPClientOtherTests {
	private static final Identifier P_ID = new IdentifierImpl("PARTID_1", "test:scheme");
	private static final Identifier SVC1_ID = new IdentifierImpl("SVCID_1");

	@Test
	void testGetSMD() throws MalformedURLException {
		ProcessInfo proc = new ProcessInfoImpl(new ProcessIdentifierImpl("PROCID_1"), null);

		EndpointInfo ep1 = new EndpointInfoV1Impl("test-1", new URL("http://this.is.a.result"));
		EndpointInfo ep2 = new EndpointInfoV1Impl("test-2", new URL("http://this.is.another.result"));

		ServiceMetadata smd = new ServiceMetadataImpl(P_ID, SVC1_ID,
										Set.of(new ProcessGroupImpl(Set.of(proc), Set.of(ep1, ep2), null))
										, null);

		String docNS = "http://test.holodeck-b2b.org/smp/ns/smd";

		MockRequestExecutor reqExecutor = new MockRequestExecutor();
		ServiceMetadata rSmd = assertDoesNotThrow(() ->
				new SMPClientBuilder().setSMPLocator(new StaticLocator("http://localhost"))
										.setRequestExecutor(reqExecutor.addResponse(200, null, docNS))
										.addProcessor(new MockResultProcessor(docNS, smd))
										.build()
										.getServiceMetadata(P_ID, SVC1_ID));

		assertNotNull(rSmd);
		assertEquals(smd, rSmd);

		assertEquals("/" + P_ID.getURLEncoded() + "/services/" + SVC1_ID.getURLEncoded(),
						reqExecutor.getRequestURLs().get(0).getPath());
	}

	@Test
	void testSMDNotFound() {
		assertNull(assertDoesNotThrow(() ->
				new SMPClientBuilder().setSMPLocator(new StaticLocator("http://localhost"))
										.setRequestExecutor(new MockRequestExecutor().addResponse(404, null, null))
										.addProcessor(new MockResultProcessor(null, null))
										.build()
										.getServiceMetadata(P_ID, SVC1_ID)));
	}

	@Test
	void testRedirection() throws MalformedURLException {
		ProcessInfo proc = new ProcessInfoImpl(new ProcessIdentifierImpl("PROCID_1"), null);

		Redirection  r = new RedirectionV2Impl(new URL("http://this.is.another.smp"));
		EndpointInfo ep1 = new EndpointInfoV1Impl("test-1", new URL("http://this.is.a.result"));

		ServiceMetadata smd1 = new ServiceMetadataImpl(P_ID, SVC1_ID,
										Set.of(new ProcessGroupImpl(null, r, null))
										, null);
		ServiceMetadata smd2 = new ServiceMetadataImpl(P_ID, SVC1_ID,
										Set.of(new ProcessGroupImpl(Set.of(proc), Set.of(ep1), null))
										, null);

		String docNS1 = "http://test.holodeck-b2b.org/smp/ns/redirection/global";
		String docNS2 = "http://test.holodeck-b2b.org/smp/ns/redirection/procgroup";

		MockRequestExecutor reqExecutor = new MockRequestExecutor();
		ServiceMetadata rSmd = assertDoesNotThrow(() ->
										new SMPClientBuilder().setSMPLocator(new StaticLocator("http://localhost"))
												.setRequestExecutor(reqExecutor
														.addResponse(200, null, docNS1)
														.addResponse(200, null, docNS2))
												.addProcessor(new MockResultProcessor(docNS1, smd1))
												.addProcessor(new MockResultProcessor(docNS2, smd2))
												.build()
												.getServiceMetadata(P_ID, SVC1_ID));

		assertEquals(2, reqExecutor.getRequestURLs().size());
		assertTrue(reqExecutor.getRequestURLs().get(1).toString().startsWith(r.getNewSMPURL().toString()));

		assertNotNull(rSmd);
		assertEquals(smd2, rSmd);
	}

	@Test
	void testTooManyRedirects() throws MalformedURLException {
		ProcessInfo proc = new ProcessInfoImpl(new ProcessIdentifierImpl("PROCID_1"), null);

		Redirection  r2 = new RedirectionV2Impl(new URL("http://this.is.yet.another.smp"));
		Redirection  r1 = new RedirectionV2Impl(new URL("http://this.is.another.smp"));

		ServiceMetadata smd1 = new ServiceMetadataImpl(P_ID, SVC1_ID,
										Set.of(new ProcessGroupImpl(null, r1, null))
										, null);
		ServiceMetadata smd2 = new ServiceMetadataImpl(P_ID, SVC1_ID,
										Set.of(new ProcessGroupImpl(null, r2, null))
										, null);

		String docNS1 = "http://test.holodeck-b2b.org/smp/ns/redirection/global";
		String docNS2 = "http://test.holodeck-b2b.org/smp/ns/redirection/global/2";

		SMPQueryException ex = assertThrows(SMPQueryException.class, () ->
										new SMPClientBuilder().setSMPLocator(new StaticLocator("http://localhost"))
												.setRequestExecutor(new MockRequestExecutor()
														.addResponse(200, null, docNS1)
														.addResponse(200, null, docNS2))
												.addProcessor(new MockResultProcessor(docNS1, smd1))
												.addProcessor(new MockResultProcessor(docNS2, smd2))
												.build()
												.getServiceMetadata(P_ID, SVC1_ID));

		assertTrue(ex.getMessage().contains("redirections"));
	}

	void testServiceGroup() throws MalformedURLException {
		ServiceGroupV1Impl svcGrp = new ServiceGroupV1Impl();
		svcGrp.setParticipantId(P_ID);
		svcGrp.addServiceReference(new URL("http://test.holodeck-b2b.org/smp/ref/to/svc1"));
		svcGrp.addServiceReference(new URL("http://test.holodeck-b2b.org/smp/ref/to/svc2"));
		svcGrp.addServiceReference(new URL("http://test.holodeck-b2b.org/smp/ref/to/svc3"));

		String docNS = "http://test.holodeck-b2b.org/smp/serviceGroup";

		MockRequestExecutor reqExecutor = new MockRequestExecutor();
		ServiceGroup<?> sg = assertDoesNotThrow(() ->
										new SMPClientBuilder().setSMPLocator(new StaticLocator("http://localhost"))
												.setRequestExecutor(reqExecutor.addResponse(200, null, docNS))
												.addProcessor(new MockResultProcessor(docNS, svcGrp))
												.build()
												.getServiceGroup(P_ID));

		assertNotNull(sg);
		assertEquals(svcGrp, sg);

		assertEquals("/" + P_ID.getURLEncoded(), reqExecutor.getRequestURLs().get(0).getPath());
	}
}
