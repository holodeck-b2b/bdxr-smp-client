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

import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.holodeckb2b.bdxr.smp.client.api.ISMPResponse;
import org.holodeckb2b.commons.testing.HttpBackendMock;
import org.holodeckb2b.commons.util.Utils;
import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class DefaultRequestExecutorTest {

	private static HttpBackendMock		smpServer;

	private static String mockURL;

	@BeforeAll
	static void startSMPServerMock() throws Exception {
		smpServer = new HttpBackendMock(0);
		smpServer.start();
		mockURL = "http://localhost:" + smpServer.getPort();
	}

	@AfterAll
	static void stopSMPServerMock() {
		if (smpServer != null)
			smpServer.stop();
	}

	@Test
	void testSuccess() {
		final String data = "Hello World!";

		smpServer.setSuccessCode(200);
		smpServer.setResponseEntityBody(data.getBytes());
		smpServer.getResponseHeaders().clear();

		ISMPResponse r =
			assertDoesNotThrow(() -> new DefaultRequestExecutor().executeRequest(new URL(mockURL + "/accept"), null));

		assertNotNull(r);
		assertEquals(200, assertDoesNotThrow(() -> r.getStatusCode()));
		assertNull(assertDoesNotThrow(() -> r.getLastModified()));
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		assertDoesNotThrow(() -> Utils.copyStream(r.getInputStream(), baos));
		assertArrayEquals(data.getBytes(), baos.toByteArray());
	}

	@Test
	void testAddIfModifiedSince() {
		final String data = "Hello World!";
		final String lastMod = ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME);

		smpServer.setSuccessCode(200);
		smpServer.setResponseEntityBody(data.getBytes());
		smpServer.getResponseHeaders().clear();

		ISMPResponse r =
			assertDoesNotThrow(() -> new DefaultRequestExecutor().executeRequest(new URL(mockURL + "/accept"), lastMod));

		assertEquals(lastMod, smpServer.getRcvdHeaders().get("if-modified-since"));

		assertNotNull(r);
		assertEquals(200, assertDoesNotThrow(() -> r.getStatusCode()));
		assertNull(assertDoesNotThrow(() -> r.getLastModified()));
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		assertDoesNotThrow(() -> Utils.copyStream(r.getInputStream(), baos));
		assertArrayEquals(data.getBytes(), baos.toByteArray());
	}

	@Test
	void testWithModified() {
		final String data = "Hello World!";
		final String lastMod = ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME);
		smpServer.setSuccessCode(200);
		smpServer.setResponseEntityBody(data.getBytes());
		smpServer.getResponseHeaders().put("Last-Modified", lastMod);

		ISMPResponse r =
			assertDoesNotThrow(() -> new DefaultRequestExecutor().executeRequest(new URL(mockURL + "/accept"), null));

		assertNotNull(r);
		assertEquals(200, assertDoesNotThrow(() -> r.getStatusCode()));
		assertEquals(lastMod, assertDoesNotThrow(() -> r.getLastModified()));
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		assertDoesNotThrow(() -> Utils.copyStream(r.getInputStream(), baos));
		assertArrayEquals(data.getBytes(), baos.toByteArray());
	}

	@Test
	void testNotModified() {
		smpServer.setRejectionCode(304);
		final String lastMod = ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME);
		smpServer.getResponseHeaders().put("Last-Modified", lastMod);

		ISMPResponse r =
			assertDoesNotThrow(() -> new DefaultRequestExecutor().executeRequest(new URL(mockURL + "/reject"), null));

		assertNotNull(r);
		assertEquals(304, assertDoesNotThrow(() -> r.getStatusCode()));
		assertEquals(lastMod, assertDoesNotThrow(() -> r.getLastModified()));
	}

	@ParameterizedTest
	@ValueSource(ints = { 400, 403, 404, 501, 500 })
	void testErrorStatus(int statuscode) {
		smpServer.setRejectionCode(statuscode);

		ISMPResponse r =
			assertDoesNotThrow(() -> new DefaultRequestExecutor().executeRequest(new URL(mockURL + "/reject"), null));

		assertNotNull(r);
		assertEquals(statuscode, assertDoesNotThrow(() -> r.getStatusCode()));
		assertNull(assertDoesNotThrow(() -> r.getLastModified()));
		assertNull(assertDoesNotThrow(() -> r.getInputStream()));
	}
}
