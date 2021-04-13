package org.holodeckb2b.brdx.smp.testhelpers;

import org.holodeckb2b.bdxr.smp.api.ISMPResultProcessor;
import org.holodeckb2b.bdxr.smp.api.SMPQueryException;
import org.holodeckb2b.bdxr.smp.datamodel.ISMPQueryResult;
import org.holodeckb2b.bdxr.smp.datamodel.ServiceInformation;
import org.w3c.dom.Document;

public class MockResultProcessor implements ISMPResultProcessor {	
	private final String supportedNS;
	private final ISMPQueryResult	result;
	
	private boolean called = false;
	
	public MockResultProcessor(final String forNS) {
		this(forNS, null);
	}
	
	public MockResultProcessor(final String forNS, final ISMPQueryResult reqResult) {
		supportedNS = forNS;
		result = reqResult != null ? reqResult : new ServiceInformation();
	}
	
	@Override
	public ISMPQueryResult processResult(Document xmlDocument) throws SMPQueryException {
		called = true;
		return result;
	}

	@Override
	public boolean canProcess(String namespaceURI) {
		return supportedNS.equals(namespaceURI);
	}

	public boolean wasCalled() {
		return called;
	}
}
