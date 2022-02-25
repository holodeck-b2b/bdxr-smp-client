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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.holodeckb2b.bdxr.smp.client.api.IRequestExecutor;
import org.holodeckb2b.bdxr.smp.client.api.ISMPResponse;
import org.holodeckb2b.bdxr.smp.client.api.SMPQueryException;

/**
 * A mock executor that returns a very simple XML Document with a configurable namespace. The executor can be configured
 * to return different namespace on subsequent executions.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class MockRequestExecutor implements IRequestExecutor {

	private static final String MOCK_XML_TEMPL = "<MockResult xmlns=\"%s\"/>";

	private List<ISMPResponse>	responses = new ArrayList<>();
	private List<URL>	requests = new ArrayList<>();

	private int	execCount = 0;

	@Override
	public ISMPResponse executeRequest(URL requestURL, String lastModified) throws SMPQueryException, UnsupportedOperationException {
		requests.add(requestURL);
		if (execCount < responses.size())
			return responses.get(execCount++);
		else
			return responses.get(responses.size() - 1);
	}

	public MockRequestExecutor addResponse(int status, String lastModified, String docNS) {
		responses.add(
			new ISMPResponse() {
				@Override
				public InputStream getInputStream() throws SMPQueryException {
					if (docNS != null)
						return new ByteArrayInputStream(String.format(MOCK_XML_TEMPL, docNS).getBytes());
					else
						throw new SMPQueryException("On request");
				}
				@Override
				public void close() {}
				@Override
				public int getStatusCode() throws SMPQueryException { return status; }
				@Override
				public String getLastModified() throws SMPQueryException { return lastModified; }
			});
		return this;
	}

	public List<URL> getRequestURLs() {
		return requests;
	}
}
