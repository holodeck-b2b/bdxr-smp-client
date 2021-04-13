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
package org.holodeckb2b.bdxr.smp.impl;

import java.util.ArrayList;
import java.util.List;

import org.holodeckb2b.bdxr.smp.api.ICertificateFinder;
import org.holodeckb2b.bdxr.smp.api.IRequestExecutor;
import org.holodeckb2b.bdxr.smp.api.ISMPClient;
import org.holodeckb2b.bdxr.smp.api.ISMPLocator;
import org.holodeckb2b.bdxr.smp.api.ISMPResultProcessor;
import org.holodeckb2b.bdxr.smp.api.ITrustValidator;
import org.holodeckb2b.commons.util.Utils;

/**
 * Contains the configuration for a {@link ISMPClient} instance.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class SMPClientConfig {
	/**
	 * The {@link ISMPLocator} implementation the <code>SMPClient</code> should use to find the location of the SMP 
	 * serving a specific participant.
	 */
	private ISMPLocator smpLocator;
	/**
	 * The {@link IRequestExecutor} implementation the <code>SMPClient</code> should use to execute the SMP queries.
	 */
	private IRequestExecutor requestExecutor;
	/**
	 * The {@link ICertificateFinder} implementation the <code>SMPClient</code> should use to get the certificate used 
	 * by the SMP for signing the results.
	 */
	private ICertificateFinder certFinder;
	/**
	 * The {@link ITrustValidator} implementation the <code>SMPClient</code> should use to validate that certificate 
	 * used by the SMP is trusted and the results can be used.
	 */
	private ITrustValidator trustValidator;
    /**
     * List of {@link ISMPResultProcessor} implementations that can convert XML documents to object representation
     */
	private List<ISMPResultProcessor> processors;

	/**
	 * Create a new SMP Client configuration with the default request executor, processor map and certificate finder
	 */
	public SMPClientConfig() {    	
		requestExecutor = new DefaultRequestExecutor();
        certFinder = new DefaultCertFinder();
    }

	/**
	 * Sets the {@link ISMPLocator} implementation the <code>SMPClient</code> should use to find the location of the 
	 * SMP serving a specific participant.
	 * 
	 * @param locator	The SMP locator implementation
	 */
	public void setSMPLocator(final ISMPLocator locator) {
		this.smpLocator = locator;
	}

	/**
	 * Gets the {@link ISMPLocator} implementation the <code>SMPClient</code> should use to find the location of the 
	 * SMP serving a specific participant.
	 * 
	 * return The SMP locator implementation
	 */
	public ISMPLocator getSMPLocator() {
		return this.smpLocator;
	}	
	
	/**
	 * Sets the {@link IRequestExecutor} implementation that the new <code>SMPClient</code> should use to execute
	 * the SMP queries.
	 * 
	 * @param executor	The request executor implementation
	 * @return this builder
	 */
	public void setRequestExecutor(IRequestExecutor executor) {
		this.requestExecutor = executor;
	}
	
	/**
	 * Gets the {@link IRequestExecutor} implementation that the new <code>SMPClient</code> should use to execute
	 * the SMP queries.
	 * 
	 * @return The request executor implementation
	 */
	public IRequestExecutor getRequestExecutor() {
		return this.requestExecutor;
	}
	
	/**
	 * Sets the (custom) {@link ICertificateFinder} implementation that the new <code>SMPClient</code> should use to 
	 * get the certificate used by the SMP for signing the results. The certificate finder only needs to be set when 
	 * non standard implementation is used in which certificates are only referenced to instead of embedded within the
	 * <code>ds:Signature</code> element as prescribed by specifications. 
	 * 
	 * @param finder	The certificate finder implementation
	 */
	public void setCertificateFinder(ICertificateFinder finder) {
		this.certFinder = finder;
	}
	
	/**
	 * Gets the {@link ICertificateFinder} implementation  that the new <code>SMPClient</code> should use to get the
	 * certificate used by the SMP for signing the results. 
	 * 
	 * @return The certificate finder implementation
	 */
	public ICertificateFinder getCertificateFinder() {
		return this.certFinder;
	}
	
	/**
	 * Sets the {@link ITrustValidator} implementation  that the new <code>SMPClient</code> should use to validate 
	 * that certificate used by the SMP is trusted and the results can be used. Using trust validating is <b>optional
	 * </b>. If no validator is specified the SMP Client will only verify the correctness of the hashes and not 
	 * check the validity of the certificate. 
	 * 
	 * @param validator	The trust validator implementation
	 */
	public void setTrustValidator(ITrustValidator validator) {
		this.trustValidator = validator;
	}
	
	/**
	 * Gets the {@link ITrustValidator} implementation  that the new <code>SMPClient</code> should use to validate 
	 * that certificate used by the SMP is trusted and the results can be used. 
	 * 
	 * @return	The trust validator implementation
	 */
	public ITrustValidator getTrustValidator() {
		return this.trustValidator;
	}
	
	/**
	 * Sets the list of {@link ISMPResultProcessor} that can handle the result and transform the XML documents into 
	 * the object model representation.
	 * 
	 * @param processorList	list of {@link ISMPResultProcessor}s. Must not be empty.
	 */
	public void setProcessors(List<ISMPResultProcessor> processorList) {
		if (Utils.isNullOrEmpty(processorList))
			throw new IllegalArgumentException("The processor list must not be empty");
		this.processors = processorList;
	}
	
	/**
	 * Sets the {@link ISMPResultProcessor} implementation that should be used for the given result namespace URI. 
	 * When there already exists a mapping for the given name space URI it will be replaced.
	 * 
	 * @param namespaceURI	String containing the name space URI 
	 * @param processor		The {@link ISMPResultProcessor} to use for result with the given namespace
	 */
	public void addProcessor(ISMPResultProcessor processor) {
		if (processor == null)
			throw new IllegalArgumentException();
		if (this.processors == null) 
			this.processors = new ArrayList<>();
		this.processors.add(processor);
	}
	
	/**
	 * Gets the list of registered {@link ISMPResultProcessor} that will handle the result and transform the XML 
	 * documents into the object model representation.
	 * 
	 * @return The list of registered {@link ISMPResultProcessor}s.
	 */
	public List<ISMPResultProcessor> getProcessors() {
		return this.processors;		
	}	
}
