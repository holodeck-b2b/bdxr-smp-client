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
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.holodeckb2b.bdxr.common.datamodel.Identifier;
import org.holodeckb2b.bdxr.common.datamodel.ProcessIdentifier;
import org.holodeckb2b.bdxr.smp.client.api.ICachedResult;
import org.holodeckb2b.bdxr.smp.client.api.ISMPClient;
import org.holodeckb2b.bdxr.smp.client.api.ISMPResponse;
import org.holodeckb2b.bdxr.smp.client.api.SMPClientBuilder;
import org.holodeckb2b.bdxr.smp.client.api.SMPLocatorException;
import org.holodeckb2b.bdxr.smp.client.api.SMPQueryException;
import org.holodeckb2b.bdxr.smp.datamodel.EndpointInfo;
import org.holodeckb2b.bdxr.smp.datamodel.ProcessGroup;
import org.holodeckb2b.bdxr.smp.datamodel.QueryResult;
import org.holodeckb2b.bdxr.smp.datamodel.Redirection;
import org.holodeckb2b.bdxr.smp.datamodel.RedirectionV1;
import org.holodeckb2b.bdxr.smp.datamodel.RedirectionV2;
import org.holodeckb2b.bdxr.smp.datamodel.ServiceGroup;
import org.holodeckb2b.bdxr.smp.datamodel.ServiceMetadata;
import org.holodeckb2b.bdxr.smp.datamodel.SignedQueryResult;
import org.holodeckb2b.commons.Pair;
import org.holodeckb2b.commons.util.Utils;

/**
 * Is the implementation of {@link ISMPClient} and controls the process of requesting the meta-data about a
 * participant in the network from a SMP server.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class SMPClient implements ISMPClient {
	private static final Logger	log = LogManager.getLogger(SMPClient.class);

    /**
     * The configuration to be used by this client
     */
    private SMPClientConfig 	cfg;

	private final SMPResultReader resultReader;

    /**
     * Creates a new client using the given configuration. It is recommended to use the {@link SMPClientBuilder} for
	 * creating new instance of the SMP client.
     *
     * @param config	The configuration to use for the new client;
     */
    public SMPClient(final SMPClientConfig config) {
        this.cfg = config;
		resultReader = new SMPResultReader(cfg);
    }

    @Override
	public EndpointInfo getEndpoint(final Identifier participantId,
									final Identifier role,
		    						final Identifier serviceId,
		    						final ProcessIdentifier processId,
		    						final Identifier     transportProfile,
		    						final boolean    overrideCache) throws SMPQueryException  {
        if (transportProfile == null || Utils.isNullOrEmpty(transportProfile.getValue()))
        	throw new IllegalArgumentException("No transport profile identifier provided");

    	log.debug("Lookup requested; (participant, service, process, role, transport) = ({},{},{},{}, {})",
                	participantId, serviceId, processId, role, transportProfile.toString());

		// First get all endpoints for the participant, role, serviceId and processId, then filter the result
		Collection<? extends EndpointInfo> endpoints = getEndpoints(participantId, role, serviceId, processId);

    	Optional<? extends EndpointInfo> findEP = endpoints.parallelStream()
									                .filter(ep -> transportProfile.equals(ep.getTransportProfileId())
																  && isActive(ep))
									                .findFirst();

		log.debug("{} endpoint found for (participant, service, process, role, transport) = ({},{},{},{},{})",
				  findEP.isPresent() ? "Returning" : "No", participantId, serviceId, processId, role, transportProfile);
    	return findEP.orElse(null);
    }

	/**
	 * Checks is the given endpoint is currently active.
	 *
	 * @param ep	the endpoint meta-data
	 * @return		<code>true</code> iff the current time is between the endpoint's activation and expiration dates. If
	 *				either is not specified it assumed the endpoint does not have an activation/expiration date.
	 */
	private boolean isActive(EndpointInfo ep) {
		final ZonedDateTime act = ep.getServiceActivationDate();
		final ZonedDateTime exp = ep.getServiceExpirationDate();
		final ZonedDateTime now = ZonedDateTime.now();
		return (act == null || now.isAfter(act)) && (exp == null || now.isBefore(exp));
	}

    @Override
	public Collection<? extends EndpointInfo> getEndpoints(final Identifier participantId,
														   final Identifier role,
														   final Identifier serviceId,
														   final ProcessIdentifier processId,
														   final boolean overrideCache) throws SMPQueryException  {

		if (participantId == null || serviceId == null || processId == null)
        	throw new IllegalArgumentException("Missing either participant, service or process ID argument");

    	log.debug("Retrieve Endpoints for (participant, service, process, role) = ({},{}, {}, {})",
					participantId, serviceId, processId, role);

		int redirections = 0;
		Redirection redirect = null;
		do {
			Pair<ServiceMetadata, Integer> rSmd = _getServiceMetadata(participantId, serviceId, redirect, overrideCache, 
																	  redirections);
			ServiceMetadata smd = rSmd.value1();

			if (smd == null) {
				log.info("No ServiceMetadata found for (participant, service) = ({},{})", participantId, serviceId);
				return Collections.emptyList();
			}
			log.trace("Check support for requested process and role");
			redirections = rSmd.value2();
			/* First filter groups on matching processes and roles, with empty process and roles list assumed to match
			 * all processes/roles
			 */
			List<? extends ProcessGroup> pg = smd.getProcessMetadata().parallelStream()
								.filter(pl -> Utils.isNullOrEmpty(pl.getProcessInfo())
										|| pl.getProcessInfo().parallelStream()
												.anyMatch(pi -> pi.getProcessId().equals(processId)
														&& (role == null || Utils.isNullOrEmpty(pi.getRoles())
															|| pi.getRoles().stream()
																		.anyMatch(r -> r.equals(role)))))
								.collect(Collectors.toList());
			/* The list can still include multiple elements because some of them could map to all processes and/or have
			 * different roles. Now we filter out the specific ones that match to the given process and role identifiers.
			 */
			if (pg.size() > 1) {
				pg = pg.parallelStream().filter(g -> !Utils.isNullOrEmpty(g.getProcessInfo())).collect(Collectors.toList());
				if (pg.size() > 1 && role != null)
					pg = pg.parallelStream()
						   .filter(g -> g.getProcessInfo().parallelStream()
												.anyMatch(pi -> pi.getProcessId().equals(processId)
															&& !Utils.isNullOrEmpty(pi.getRoles())
															&& pi.getRoles().stream()
																		.anyMatch(r -> r.equals(role)))
															)
							.collect(Collectors.toList());
			}
			if (pg.isEmpty()) {
				log.warn("Requested (participant, service, process, role) is not supported; ({},{},{},{})",
						 participantId, serviceId, processId, role);
				return Collections.emptyList();
			}
			if (pg.size() != 1) {
				log.error("Unable to determine unique process meta-data from SMP result!");
				throw new SMPQueryException("Ambigious result based on query arguments or SMP data");
			}
			final ProcessGroup p = pg.get(0);
			redirect = p.getRedirection();
			if (redirect != null) {
				log.debug("Found redirection for (service, process, role) = ({},{},{})", serviceId, processId, role);
				redirections++;
			} else {
				log.info("Returning endpoints found for (participant, service, process, role) = ({},{}, {}, {})",
						 participantId, serviceId, processId, role);
				return p.getEndpoints();
			}
		} while (redirections <= cfg.maxRedirects);
		log.error("Exceeded the number of allowed redirections");
		throw new SMPQueryException("Exceeded the number of allowed redirections");
	}

	@Override
	public ServiceMetadata getServiceMetadata(final Identifier participantId, final Identifier serviceId,
											  final boolean overrideCache) throws SMPQueryException {
		if (participantId == null || serviceId == null)
        	throw new IllegalArgumentException("Missing either participant or service ID argument");

    	log.debug("Retrieve ServiceMetadata for (participant, service) = ({},{})", participantId, serviceId);
		ServiceMetadata smd = _getServiceMetadata(participantId, serviceId, null, overrideCache, 0).value1();
		log.info("{} ServiceMetadata found for (participant, service) = ({},{})", smd != null ? "Returning" : "No",
				participantId, serviceId);
		return smd;
	}

	@Override
	public ServiceGroup<?> getServiceGroup(Identifier participantId, boolean overrideCache) throws SMPQueryException {
		if (participantId == null)
        	throw new IllegalArgumentException("Missing participant ID argument");

    	log.debug("Retrieve ServiceGroup for participant = {}", participantId);
		try {
			log.trace("Getting URL of SMP handling participant");
			String baseURL = cfg.smpLocator.locateSMP(participantId).toString();
			if (!baseURL.endsWith("/"))
				baseURL += "/";

			ServiceGroup<?> sg = (ServiceGroup<?>) 
									retrieveMetadata(new URL(baseURL + participantId.getURLEncoded()), overrideCache);

			log.info("{} ServiceGroup for participant {}", sg != null ? "Returning" : "No", participantId);
			return sg;
		} catch (SMPLocatorException ex) {
			log.error("An error occurred in locating the SMP server for participant {}."
					 + "\n\tDetails: {}\n\tCaused by: {}", participantId, ex.getMessage(),
														  Utils.getExceptionTrace(ex));
			throw new SMPQueryException("Could not locate the SMP server for participant", ex);
		} catch (MalformedURLException invalidURL) {
			log.error("Could not construct valid query URL for retrieving service group");
			throw new SMPQueryException("Could not construct valid query URL");
		} catch (ClassCastException notAServiceGroup) {
			log.error("Response from SMP server was not a ServiceGroup!");
			throw new SMPQueryException("Invalid ServiceGroup response from SMP server");
		}
	}

	/**
	 * Internal method to retrieve the <i>ServiceMetadata</i> for the given participant and service. It handles the
	 * "generic" redirections, i.e. those that apply regardless of the process in which the requested service might be
	 * used.
	 * <p>Redirection is handled by recursion. On the initial call, when no redirection is given, the SMP URL will be
	 * retrieved using the configured locator. When the given redirection contains a certificate, it will be used to
	 * check the signing certificate of the new query result.
	 *
	 * @param participantId		participant identifier
	 * @param serviceId			service identifier
	 * @param redirection		redirection info, <code>null</code> on initial call
	 * @param overrideCache		<code>true</code> when the cached result should be ignored and the SMP server should 
	 * 							always be queried. <code>false</code> if a cached result can be used. 
	 * @param redirections		the number of already followed redirections
	 * @return	the found meta-data, or <code>null</code> if not found
	 * @throws SMPQueryException	when an error occurs retrieving the meta-data. This may be caused by a communication
	 *								error with the SMP, a incorrect redirection or exceeding the maximum number of
	 *								redirections.
	 */
    private Pair<ServiceMetadata, Integer> _getServiceMetadata(final Identifier participantId,
															   final Identifier serviceId,
															   final Redirection redirection,
															   final boolean overrideCache,
															   final int redirections) throws SMPQueryException {
		if (redirections > cfg.maxRedirects) {
			log.error("Exceeded the number of allowed redirections");
			throw new SMPQueryException("Exceeded the number of allowed redirections");
		}
		URL smpURL;
		if (redirection == null) {
			try {
				log.debug("Getting URL of SMP handling participant");
				smpURL = cfg.smpLocator.locateSMP(participantId);
			} catch (SMPLocatorException ex) {
				log.error("An error occurred in locating the SMP server for participant {}."
						 + "\n\tDetails: {}\n\tCaused by: {}", participantId, ex.getMessage(),
															  Utils.getExceptionTrace(ex));
				throw new SMPQueryException("Could not locate the SMP server for participant", ex);
			}
		} else
			smpURL = redirection.getNewSMPURL();

		URL queryURL;
		try {
			String baseURL = smpURL.toString();
			if (!baseURL.endsWith("/"))
				baseURL += "/";
			queryURL = new URL(String.format("%s%s/services/%s", baseURL, participantId.getURLEncoded(),
																  serviceId.getURLEncoded()));
		} catch (MalformedURLException invalidURL) {
			log.error("Could not construct valid query URL for retrieving meta-data");
			throw new SMPQueryException("Could not construct valid query URL");
		}

		ServiceMetadata metadata = (ServiceMetadata) retrieveMetadata(queryURL, overrideCache);
		if (metadata != null) {
			if (redirection != null) {
				if (redirection instanceof RedirectionV2) {
					X509Certificate rCert = ((RedirectionV2) redirection).getSMPCertificate();
					if (rCert != null) {
						log.debug("Check redirection certificate");
						if (!(metadata instanceof SignedQueryResult)) {
							log.error("Expected signed meta-data, but received unsigned");
							throw new SMPQueryException("Incorrect redirection; no certificate");
						}
						if (!rCert.equals(((SignedQueryResult) metadata).getSigningCertificate())) {
							log.error("Signing certificate of redirected SMP does not match expected certificate");
							throw new SMPQueryException("Incorrect redirection; certificate mismatch");
						}
					}
				} else {
					boolean[] rSubjectUID = ((RedirectionV1) redirection).getSMPSubjectUniqueID();
					if (rSubjectUID != null && rSubjectUID.length > 0) {
						log.debug("Check redirection certificate");
						if (!(metadata instanceof SignedQueryResult)) {
							log.error("Expected signed meta-data, but received unsigned");
							throw new SMPQueryException("Incorrect redirection; no certificate");
						}
						if (!Arrays.equals(rSubjectUID,
									((SignedQueryResult) metadata).getSigningCertificate().getSubjectUniqueID())) {
							log.error("Signing certificate of redirected SMP does not match expected certificate");
							throw new SMPQueryException("Incorrect redirection; certificate mismatch");
						}
					}
				}
			}
			Collection<? extends ProcessGroup> processes = metadata.getProcessMetadata();
			if (processes.size() == 1) {
				ProcessGroup pg = processes.iterator().next();
				Redirection r = pg.getRedirection();
				if (r != null && Utils.isNullOrEmpty(pg.getProcessInfo())) {
					log.debug("Following redirection to {}", r.getNewSMPURL().toString());
					return _getServiceMetadata(participantId, serviceId, r, overrideCache, redirections + 1);
				}
			}
		}
		return new Pair<>(metadata, redirections);
	}

	/**
	 * Helper method to execute the query to the SMP server. Handles caching of the results.
	 *
	 * @param queryURL	the URL to retrieve the request meta data from the server
	 * @param overrideCache		<code>true</code> when the cached result should be ignored and the SMP server should 
	 * 							always be queried. <code>false</code> if a cached result can be used. 
	 * @return	the retrieved meta-data if available, <code>null</code> if the requested meta-data are not found
	 * @throws SMPQueryException	when an error occurs retrieving the meta-data from the SMP server.
	 */
	private QueryResult retrieveMetadata(final URL queryURL, final boolean overrideCache) throws SMPQueryException {
		// If caching is used and not overridden, check if there is a cached result
		ICachedResult cached = !overrideCache && cfg.resultCache != null ? cfg.resultCache.getCachedResult(queryURL) 
																		 : null;

		// If local caching is enabled and a result for this query was cached, check if it can be re-used
		if (cached != null && cfg.useLocalCaching
			&& LocalDateTime.now().isBefore(cached.getLastQueried().plusMinutes(cfg.maxLocalCacheTime))) {
			log.info("Re-using cached response for query {}", queryURL.toString());
			return cached.getQueryResult();
		}
		ISMPResponse response = null;
		try {
			log.debug("Query the SMP: {}", queryURL.toString());
			response = cfg.requestExecutor.executeRequest(queryURL,
														  cached != null ? cached.getLastModified() : null);
			int statusCode = response.getStatusCode();
			if (statusCode == ISMPResponse.NOT_MODIFIED) {
				log.info("Meta-data not modified, re-using cache response for query {}", queryURL.toString());
				cfg.resultCache.updateLastQueried(queryURL, LocalDateTime.now());
				return cached.getQueryResult();
			} else if (statusCode == ISMPResponse.NOT_FOUND) {
				log.info("No meta-data not found for query {}", queryURL.toString());
				return null;
			} else if (statusCode != ISMPResponse.OK) {
				log.warn("SMP server returned error code ({}) on query {}", statusCode, queryURL.toString());
				throw new SMPQueryException("SMP Server error (" + statusCode + ")");
			}
			QueryResult result = resultReader.handleResponse(response.getInputStream());
			if (cfg.resultCache != null) {
				log.debug("Store result in cache for re-use");
				cfg.resultCache.storeResult(queryURL, result, response.getLastModified(), LocalDateTime.now());
			}
			return result;
        } catch (Throwable t) {
			if (t instanceof SMPQueryException)
				throw t;
			log.error("An unexpected error occurred querying the SMP (queryURL={}). Error details: {}",
						queryURL.toString(), Utils.getExceptionTrace(t));
			throw new SMPQueryException("Unexpected error during SMP query execution", t);
		} finally {
			if (response != null)
				response.close();
		}
    }
}
