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
package org.holodeckb2b.bdxr.smp.client.api;

import java.util.Collection;

import org.holodeckb2b.bdxr.common.datamodel.Identifier;
import org.holodeckb2b.bdxr.common.datamodel.ProcessIdentifier;
import org.holodeckb2b.bdxr.smp.datamodel.EndpointInfo;
import org.holodeckb2b.bdxr.smp.datamodel.ServiceGroup;
import org.holodeckb2b.bdxr.smp.datamodel.ServiceMetadata;

/**
 * Defines the interface of the SMP Client that can be used to request the meta-data about a participant in the
 * network from a SMP server. To create an SMP Client instance use the {@link SMPClientBuilder} to setup the network
 * specific parameters and create the client instance.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public interface ISMPClient {

	/**
     * Gets the meta-data of the currently active endpoint for the given participant, service and process and that
	 * supports the requested transport profile.
	 * <p>NOTE: If there are multiple matching endpoints (which should not happen) the client will select one to use
     * <p>NOTE 2: All versions of the SMP specification define a specific process identifier to indicate that a service
	 * is not bound to any specific process (<i>busdox:noprocess</i> in PEPPOL version and <i>bdx:noprocess</i> in the
	 * OASIS versions). To search for the meta-data of such a service a {@link ProcessIdentifier} with the <i>no
	 * process</i> indicator should be provided as the <code>processId</code> parameter of this method.
     *
     * @param participantId		Participant's Id
     * @param serviceId			Service Id
     * @param processId			Process Id
     * @param transportProfile	Requested transport profile identifier
     * @return	The endpoint meta-data if there exists an active endpoint for this participant, service and process and
	 *			which supports the requested transport profile, <code>null</code> otherwise.
     * @throws SMPQueryException 	When an error occurs in the lookup of the SMP location or querying the SMP server
     */
	default EndpointInfo getEndpoint(final Identifier participantId,
    						 final Identifier serviceId,
    						 final ProcessIdentifier processId,
    						 final Identifier transportProfile) throws SMPQueryException {
		return getEndpoint(participantId, null, serviceId, processId, transportProfile, false);
	}
	
	/**
	 * Gets the meta-data of the currently active endpoint for the given participant, service and process and that
	 * supports the requested transport profile with the option to override the result stored in the cache.
	 *
	 * @param participantId		Participant's Id
	 * @param serviceId			Service Id
	 * @param processId			Process Id
	 * @param transportProfile	Requested transport profile identifier
	 * @param overrideCache		<code>true</code> when the cached result should be ignored and the SMP server should 
	 * 							always be queried. <code>false</code> if a cached result can be used. 
	 * @return	The endpoint meta-data if there exists an active endpoint for this participant, service and process and
	 *			which supports the requested transport profile, <code>null</code> otherwise.
	 * @throws SMPQueryException 	When an error occurs in the lookup of the SMP location or querying the SMP server
	 * @since 3.2.0
	 * @see #getEndpoint(Identifier, Identifier, ProcessIdentifier, Identifier)
	 */
	default EndpointInfo getEndpoint(final Identifier participantId,
									 final Identifier serviceId,
									 final ProcessIdentifier processId,
									 final Identifier transportProfile,
									 final boolean    overrideCache) throws SMPQueryException {
		return getEndpoint(participantId, null, serviceId, processId, transportProfile, overrideCache);
	}

    /**
     * Gets the meta-data of all endpoints for the given participant, service and process.
     * <p>NOTE: All versions of the SMP specification a specific process identifier is defined to indicate that a
     * service is not bound to any specific process (<i>busdox:noprocess</i> in PEPPOL version and <i>bdx:noprocess</i>)
     * in the OASIS versions). To search for the meta-data of such a service a {@link ProcessIdentifier} with the
     * <i>no process</i> indicator should be provided as the <code>processId</code> parameter of this method.
     *
     * @param participantId		Participant's Id
     * @param serviceId			Service Id
     * @param processId			Process Id
     * @return	The endpoint meta-data if there exist endpoints for this participant, service and process,
     * 			an empty collection otherwise.
     * @throws SMPQueryException 	When an error occurs in the lookup of the SMP location or querying the SMP server
	 * @since 3.0.0	Result type is now a more generic collection of endpoints.
     */
    default Collection<? extends EndpointInfo> getEndpoints(final Identifier participantId,
													final Identifier serviceId,
													final ProcessIdentifier processId) throws SMPQueryException {
    	return getEndpoints(participantId, null, serviceId, processId, false);
    }
    
    /**
     * Gets the meta-data of all endpoints for the given participant, service and process with the option to override 
     * the result stored in the cache.
     *
     * @param participantId		Participant's Id
     * @param serviceId			Service Id
     * @param processId			Process Id
	 * @param overrideCache		<code>true</code> when the cached result should be ignored and the SMP server should 
	 * 							always be queried. <code>false</code> if a cached result can be used. 
     * @return	The endpoint meta-data if there exist endpoints for this participant, service and process,
     * 			an empty collection otherwise.
     * @throws SMPQueryException 	When an error occurs in the lookup of the SMP location or querying the SMP server
     * @since 3.2.0
     * @see #getEndpoints(Identifier, Identifier, ProcessIdentifier) 
     */
    default Collection<? extends EndpointInfo> getEndpoints(final Identifier participantId,
												    		final Identifier serviceId,
												    		final ProcessIdentifier processId,
												    		final boolean overrideCache) throws SMPQueryException {
    	return getEndpoints(participantId, null, serviceId, processId, overrideCache);
    }

	/**
     * Gets the meta-data of the currently active endpoint for the given participant acting in the specified role for
	 * the given service and process and that supports the requested transport profile.
	 * <p>NOTE: If there are multiple matching endpoints (which should not happen) the client will select one to use
     * <p>NOTE 2: The role a participant plays in a process was added in the OASIS SMP v2 specification and therefore it
     * can also only be evaluated if the SMP server supports this version. This implies that when the response is an
     * older version this criterion cannot be applied and therefore there will be no result to the query.
     * <p>NOTE 3: All versions of the SMP specification a specific process identifier is defined to indicate that a
     * service is not bound to any specific process (<i>busdox:noprocess</i> in PEPPOL version and <i>bdx:noprocess</i>)
     * in the OASIS versions). To search for the meta-data of such a service a {@link ProcessIdentifier} with the
     * <i>no process</i> indicator should be provided as the <code>processId</code> parameter of this method.
     *
     * @param participantId		Participant's Id
     * @param role				Role of the participant
     * @param serviceId			Service Id
     * @param processId			Process Id
     * @param transportProfile	Requested transport profile identifier
     * @return	The endpoint meta-data if there exists an active endpoint for this participant, service and process and
	 *			which supports the requested transport profile, <code>null</code> otherwise.
     * @throws SMPQueryException 	When an error occurs in the lookup of the SMP location or querying the SMP server
     * @since 2.0.0
     */
	default EndpointInfo getEndpoint(final Identifier participantId,
							 final Identifier role,
    						 final Identifier serviceId,
    						 final ProcessIdentifier processId,
    						 final Identifier     transportProfile) throws SMPQueryException {
		return getEndpoint(participantId, role, serviceId, processId, transportProfile, false);
	}

	/**
	 * Gets the meta-data of the currently active endpoint for the given participant acting in the specified role for
	 * the given service and process and that supports the requested transport profile with the option to override the 
	 * result stored in the cache.
	 *
	 * @param participantId		Participant's Id
	 * @param role				Role of the participant
	 * @param serviceId			Service Id
	 * @param processId			Process Id
	 * @param transportProfile	Requested transport profile identifier
	 * @param overrideCache		<code>true</code> when the cached result should be ignored and the SMP server should 
	 * 							always be queried. <code>false</code> if a cached result can be used. 
	 * @return	The endpoint meta-data if there exists an active endpoint for this participant, service and process and
	 *			which supports the requested transport profile, <code>null</code> otherwise.
	 * @throws SMPQueryException 	When an error occurs in the lookup of the SMP location or querying the SMP server
	 * @since 3.2.0
     * @see #getEndpoint(Identifier, Identifier, Identifier, ProcessIdentifier, Identifier)
	 */
	EndpointInfo getEndpoint(final Identifier participantId,
			final Identifier role,
			final Identifier serviceId,
			final ProcessIdentifier processId,
			final Identifier transportProfile,
			final boolean 	 overrideCache) throws SMPQueryException;

	/**
	 * Gets the meta-data of all endpoints for the given participant acting in the specified role for the given service
	 * and process and that supports the requested transport profile.
     * <p>NOTE: The role a participant plays in a process was added in the OASIS SMP v2 specification and therefore it
     * can also only be evaluated if the SMP server supports this version. This implies that when the response is an
     * older version this criterion cannot be applied and therefore there will be no result to the query.
     * <p>NOTE 2: All versions of the SMP specification a specific process identifier is defined to indicate that a
     * service is not bound to any specific process (<i>busdox:noprocess</i> in PEPPOL version and <i>bdx:noprocess</i>)
     * in the OASIS versions). To search for the meta-data of such a service a {@link ProcessIdentifier} with the
     * <i>no process</i> indicator should be provided as the <code>processId</code> parameter of this method.
	 *
	 * @param participantId		Participant's Id
	 * @param role				Role of the participant
	 * @param serviceId			Service Id
	 * @param processId			Process Id
	 * @return	The endpoint meta-data if there exist endpoints for this participant, role, service and process,
	 * 			an empty collection otherwise.
	 * @throws SMPQueryException 	When an error occurs in the lookup of the SMP location or querying the SMP server
	 * @since 2.0.0
	 * @since 3.0.0	Result type is now a more generic collection of endpoints.
	 */
	default Collection<? extends EndpointInfo> getEndpoints(final Identifier participantId,
													final Identifier role,
													final Identifier serviceId,
													final ProcessIdentifier processId) throws SMPQueryException {
		return getEndpoints(participantId, role, serviceId, processId, false);
	}

	/**
	 * Gets the meta-data of all endpoints for the given participant acting in the specified role for the given service
	 * and process and that supports the requested transport profile with the option to override a cached result.
	 *
	 * @param participantId		Participant's Id
	 * @param role				Role of the participant
	 * @param serviceId			Service Id
	 * @param processId			Process Id
	 * @param overrideCache		<code>true</code> when the cached result should be ignored and the SMP server should 
	 * 							always be queried. <code>false</code> if a cached result can be used. 
	 * @return	The endpoint meta-data if there exist endpoints for this participant, role, service and process,
	 * 			an empty collection otherwise.
	 * @throws SMPQueryException 	When an error occurs in the lookup of the SMP location or querying the SMP server
	 * @since 3.2.0
	 * @see #getEndpoints(Identifier, Identifier, Identifier, ProcessIdentifier)	
	 */
	Collection<? extends EndpointInfo> getEndpoints(final Identifier participantId,
			final Identifier role,
			final Identifier serviceId,
			final ProcessIdentifier processId,
			final boolean overrideCache) throws SMPQueryException;
	
	/**
	 * Gets all meta-data of a Service provided by a Participant.
	 *
	 * @param participantId		Participant's Id
     * @param serviceId			Service Id
	 * @return	The service meta-data returned by the SMP server, <code>null</code> if no result was available
	 * @throws SMPQueryException	When an error occurs in the lookup of the SMP location or querying the SMP server
	 * @since 3.0.0
	 */
	default ServiceMetadata getServiceMetadata(final Identifier participantId, final Identifier serviceId)
																							throws SMPQueryException {
		return getServiceMetadata(participantId, serviceId, false);
	}
	
	/**
	 * Gets all meta-data of a Service provided by a Participant with the option to override the result stored in the
	 * cache. Overriding the cached result may be useful when a problem occurs in processing based on the current
	 * result, for example when the published certificate has been revoked. 
	 *
	 * @param participantId		Participant's Id
	 * @param serviceId			Service Id
	 * @param overrideCache		<code>true</code> when the cached result should be ignored and the SMP server should 
	 * 							always be queried. <code>false</code> if a cached result can be used. 
	 * @return	The service meta-data returned by the SMP server, <code>null</code> if no result was available
	 * @throws SMPQueryException	When an error occurs in the lookup of the SMP location or querying the SMP server
	 * @since 3.2.0
	 */
	ServiceMetadata getServiceMetadata(final Identifier participantId, final Identifier serviceId, 
									   final boolean overrideCache) throws SMPQueryException;

	/**
	 * Gets the service group for a participant, i.e. the overview of all registered services for that participant.
	 *
	 * @param participantId		Participant's Id
	 * @return	The service group meta-data returned by the SMP server, <code>null</code> if no result was available
	 * @throws SMPQueryException	When an error occurs in the lookup of the SMP location or querying the SMP server
	 * @since 3.0.0
	 */
	default ServiceGroup<?> getServiceGroup(final Identifier participantId) throws SMPQueryException {
		return getServiceGroup(participantId, false);
	}
	
	/**
	 * Gets the service group for a participant with the option to override the result stored in the cache.
	 *
	 * @param participantId		Participant's Id
	 * @param overrideCache		<code>true</code> when the cached result should be ignored and the SMP server should 
	 * 							always be queried. <code>false</code> if a cached result can be used. 
	 * @return	The service group meta-data returned by the SMP server, <code>null</code> if no result was available
	 * @throws SMPQueryException	When an error occurs in the lookup of the SMP location or querying the SMP server
	 * @since 4.0.0
	 */
	ServiceGroup<?> getServiceGroup(final Identifier participantId, final boolean overrideCache) 
																							throws SMPQueryException;
}
