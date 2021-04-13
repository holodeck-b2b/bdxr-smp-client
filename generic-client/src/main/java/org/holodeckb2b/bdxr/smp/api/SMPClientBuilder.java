package org.holodeckb2b.bdxr.smp.api;

import java.util.List;

import org.holodeckb2b.bdxr.smp.impl.DefaultRequestExecutor;
import org.holodeckb2b.bdxr.smp.impl.SMPClient;
import org.holodeckb2b.bdxr.smp.impl.SMPClientConfig;
import org.holodeckb2b.commons.util.Utils;

/**
 * Is a <i>builder</i> of {@link ISMPClient}s and should be used to configure and create new SMP Clients for a specific
 * network configuration, such as the PEPPOL network. 
 * <p>At a minimum the {@link ISMPLocator} implementation to find the correct SMP server and one {@link 
 * ISMPResultProcessor} must be configured before calling the {@link #build()} method to create a new SMP Client. See 
 * the method documentation for all configuration options available.   
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class SMPClientBuilder {
	/**
	 * The configuration for the new SMP client
	 */
	private SMPClientConfig	newClientConfig = new SMPClientConfig();
	
	/**
	 * Sets the {@link ISMPLocator} implementation that the new SMP Client should use to find the location of the SMP 
	 * serving a specific participant.
	 * 
	 * @param locator	The SMP locator implementation
	 * @return	this builder
	 */
	public SMPClientBuilder setSMPLocator(ISMPLocator locator) {
		newClientConfig.setSMPLocator(locator);
		return this;
	}
	
	/**
	 * Sets the specific {@link IRequestExecutor} implementation the new SMP Client should use to execute the SMP 
	 * queries. If no specific implementation is set, the {@link DefaultRequestExecutor} will be used.
	 * 
	 * @param executor	The request executor implementation
	 * @return this builder
	 */
	public SMPClientBuilder setRequestExecutor(IRequestExecutor executor) {
		newClientConfig.setRequestExecutor(executor);
		return this;
	}
	
	/**
	 * Sets the specific {@link ICertificateFinder} implementation the new SMP Client should use to get the certificate 
	 * used by the SMP for signing the results. The certificate finder only needs to be set when the network uses a  
	 * non standard way to reference the certificate used for signing instead of embedded it within the <code>
	 * ds:Signature</code> element as prescribed by specifications. 
	 * 
	 * @param finder	The certificate finder implementation
	 * @return this builder
	 */
	public SMPClientBuilder setCertificateFinder(ICertificateFinder finder) {
		newClientConfig.setCertificateFinder(finder);
		return this;
	}
	
	/**
	 * Sets the {@link ITrustValidator} implementation the new SMP Client should use to validate that certificate used 
	 * by the SMP to sign the response is trusted and the results can be used. Using trust validating is <b>optional
	 * </b>. If no validator is specified the SMP Client will only verify the correctness of the hashes and not 
	 * check the validity of the certificate. It however it <b>recommended</b> to implement a trust validation 
	 * mechanism.
	 * 
	 * @param validator		The trust validator implementation
	 * @return this builder
	 */
	public SMPClientBuilder setTrustValidator(ITrustValidator validator) {
		newClientConfig.setTrustValidator(validator);
		return this;				
	}
	
	/**
	 * Sets the list of {@link ISMPResultProcessor}s that can handle the result and transform the XML documents into the 
	 * object model representation. This will replace any already configured list.
	 * 
	 * @param processorMap	The list of {@link ISMPResultProcessor}s. Must not be empty.
	 */	
	public SMPClientBuilder setProcessors(List<ISMPResultProcessor> processorList) {
		newClientConfig.setProcessors(processorList);
		return this;
	}
	
	/**
	 * Adds the {@link ISMPResultProcessor} implementation to the list of processors. 
	 * <p>NOTE: Only the first registered processor that can handle a result will be used. 
	 * 
	 * @param processor		The {@link ISMPResultProcessor} to use
	 */
	public SMPClientBuilder addProcessor(ISMPResultProcessor processor) {
		newClientConfig.addProcessor(processor);
		return this;
	}
	
	/**
	 * Builds a new {@link ISMPClient} instance configured according to the settings provided to the builder.
	 * 
	 * @return 	The new SMP client 
	 * @throws  IllegalStateException	when no SMP Locator or result processor(s) have been configured.
	 */
	public ISMPClient build() {
		if (newClientConfig.getSMPLocator() == null)
			throw new IllegalStateException("No SMP locator specified, unable to build client");
		if (Utils.isNullOrEmpty(newClientConfig.getProcessors()))
			throw new IllegalStateException("No result processor(s) specified, unable to build client");
		
		return new SMPClient(newClientConfig);
	}
}
