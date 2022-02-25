package org.holodeckb2b.brdx.smp.testhelpers;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;

import org.holodeckb2b.bdxr.smp.api.IRequestExecutor;
import org.holodeckb2b.bdxr.smp.api.ISMPResponseConnection;
import org.holodeckb2b.bdxr.smp.api.ISMPResultProcessor;
import org.holodeckb2b.bdxr.smp.api.SMPQueryException;

/**
 * A mock executor that return a very simple XML Document with a configurable namespace. A default namesapce is defined
 * for easy testing. By setting a test {@link ISMPResultProcessor} for the configured namespace tests can ensure that a 
 * specific SMP result set is processed.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 2.0.0
 */
public class TestDocExecutor implements IRequestExecutor {

	public static final String TEST_NS_URI = "http://test.holodeck-b2b.org/bdxr/smp";
	
	private static final String MOCK_XML_TEMPL = "<TestResult xmlns=\"%s\"/>";
	
	private String docNsURI;
	
	public TestDocExecutor() {
		docNsURI = TEST_NS_URI;
	}
	
	public TestDocExecutor(String nsURI) {
		docNsURI = nsURI;
	}
	
	
	@Override
	public ISMPResponseConnection executeRequest(URL requestURL)
			throws SMPQueryException, UnsupportedOperationException {
		
		return new ISMPResponseConnection() {
			
			@Override
			public InputStream getInputStream() throws SMPQueryException {
				return new ByteArrayInputStream(String.format(MOCK_XML_TEMPL, docNsURI).getBytes());
			}
			
			@Override
			public void close() {
			}
		};
	}

}
