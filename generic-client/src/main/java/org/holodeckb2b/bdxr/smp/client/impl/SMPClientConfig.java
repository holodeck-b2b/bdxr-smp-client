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

import java.util.ArrayList;
import java.util.List;

import org.holodeckb2b.bdxr.smp.client.api.ICertificateFinder;
import org.holodeckb2b.bdxr.smp.client.api.IRequestExecutor;
import org.holodeckb2b.bdxr.smp.client.api.IResultCache;
import org.holodeckb2b.bdxr.smp.client.api.ISMPClient;
import org.holodeckb2b.bdxr.smp.client.api.ISMPLocator;
import org.holodeckb2b.bdxr.smp.client.api.ISMPResultProcessor;
import org.holodeckb2b.bdxr.smp.client.api.ITrustValidator;
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
	ISMPLocator smpLocator;
	/**
	 * The {@link IRequestExecutor} implementation the <code>SMPClient</code> should use to execute the SMP queries.
	 */
	IRequestExecutor requestExecutor;
	/**
	 * The {@link ICertificateFinder} implementation the <code>SMPClient</code> should use to get the certificate used
	 * by the SMP for signing the results.
	 */
	ICertificateFinder certFinder;
	/**
	 * The {@link ITrustValidator} implementation the <code>SMPClient</code> should use to validate that certificate
	 * used by the SMP is trusted and the results can be used.
	 */
	ITrustValidator trustValidator;
    /**
     * List of {@link ISMPResultProcessor} implementations that can convert XML documents to object representation
     */
	List<ISMPResultProcessor> processors;
	/**
	 * The maximum number of redirections that the SMP client should follow before ending the query process.
	 * @since 3.0.0
	 */
	int	maxRedirects;
	/**
	 * The {@link IResultCache} implementation that should be used to temporarily store query results for re-use.
	 * @since 3.0.0
	 */
	IResultCache	resultCache;
	/**
	 * Indicates whether the client should use local caching to reduce the number of HTTP requests.
	 * @since 3.0.0
	 */
	boolean useLocalCaching;
	/**
	 * The number of minutes that a query result from the local cache may be used before the server should be queried
	 * again.
	 * @since 3.0.0
	 */
	int		maxLocalCacheTime;
	/**
	 * Indicates whether <i>secure validation</i> should be used by the SMP client when validating the XML signature of 
	 * the response.
	 * @since 3.1.0  
	 */
	Boolean secureSignatureValidation;

	/**
	 * Create a new SMP Client configuration with the default request executor and certificate finder, one allowed
	 * redirection and no caching of query results.
	 */
	public SMPClientConfig() {
		requestExecutor = new DefaultRequestExecutor();
        certFinder = new DefaultCertFinder();
		maxRedirects = 1;
		useLocalCaching = false;
		secureSignatureValidation = true;
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
	 * Adds a {@link ISMPResultProcessor} implementation.
	 *
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

	/**
	 * Sets the maximum number of redirections that the SMP client should follow before ending the query process.
	 *
	 * @param max the new maximum number of redirections
	 * @since 3.0.0
	 */
	public void setMaxRedirections(int max) {
		this.maxRedirects = max;
	}

	/**
	 * Gets the maximum number of redirections that the SMP client should follow before ending the query process.
	 *
	 * @return the maximum number of redirections
	 * @since 3.0.0
	 */
	public int getMaxRedirections() {
		return this.maxRedirects;
	}

	/**
	 * Sets the {@link IResultCache} implementation that should be used to temporarily store query results for re-use.
	 * The cache must already be configured and initialised.
	 *
	 * @param cache the result cache implementation to use
	 * @since 3.0.0
	 */
	public void setResultCache(IResultCache cache) {
		this.resultCache = cache;
	}

	/**
	 * Gets the {@link IResultCache} implementation that should be used to temporarily store query results for re-use.
	 *
	 * @return the result cache implementation to use
	 * @since 3.0.0
	 */
	public IResultCache getResultCache() {
		return this.resultCache;
	}

	/**
	 * Sets the indicator whether the client should use local caching to reduce the number of HTTP requests. If local 
	 * caching is enabled and if not set already, sets the maximum time a result may be cached to the default value of 
	 * 15 minutes.
	 *
	 * @param cacheLocally <code>true</code> when the client should use local caching, <code>false</code> if not
	 * @since 3.0.0
	 */
	public void setLocalCaching(boolean cacheLocally) {
		this.useLocalCaching = cacheLocally;
		if (this.maxLocalCacheTime <= 0)
			this.maxLocalCacheTime = 15;
	}

	/**
	 * Indicates whether the client should use local caching to reduce the number of HTTP requests.
	 *  
	 * @return <code>true</code> when the client should use local caching, <code>false</code> if not
	 * @since 3.1.0
	 */
	public boolean useLocalCaching() {
		return useLocalCaching;
	}
	
	/**
	 * Sets the number of minutes that a query result from the local cache may be used before the server should be
	 * queried again.
	 * <p>NOTE: This method does not enable the local caching of results. That must be enabled explicitly by calling
	 * {@link #setLocalCaching(boolean)}
	 *
	 * @param maxTime number of minutes a cached result can be re-used, must be at least 1
	 * @since 3.0.0
	 */
	public void setMaxLocalCacheTime(int maxTime) {
		if (maxTime < 1)
			throw new IllegalArgumentException("Max time for local cache must be at least 1");
		this.maxLocalCacheTime = maxTime;
	}
	
	/**
	 * Sets the number of minutes that a query result from the local cache may be used before the server should be
	 * queried again.
	 * 
	 * @return number of minutes a cached result can be re-used, must be at least 1
	 * @since 3.1.0
	 */
	public int getMaxLocalCacheTime() {
		return maxLocalCacheTime;
	}
	
	/**
	 * Sets the indicator whether <i>secure validation</i> should be used by the SMP client when validating the XML 
	 * signature of the response.
	 * 
	 * @param enable	<code>true</code> when secure validation should be used, 
	 * 					<code>false</code> if secure validation should be disabled
	 * @since 3.1.0
	 */
	public void setSecureSignatureValidation(boolean enable) {
		this.secureSignatureValidation = enable;
	}
	
	/**
	 * Indicates whether <i>secure validation</i> should be used by the SMP client when validating the XML signature of 
	 * the response.
	 * 
	 * @return <code>true</code> when secure validation should be used, <code>false</code> if not
	 * @since 3.1.0
	 */
	public Boolean useSecureSignatureValidation() {
		return secureSignatureValidation;
	}
}
