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
package org.holodeckb2b.bdxr.smp.api;

import java.util.List;

import org.holodeckb2b.bdxr.smp.datamodel.EndpointInfo;
import org.holodeckb2b.bdxr.smp.datamodel.Identifier;
import org.holodeckb2b.bdxr.smp.datamodel.ProcessIdentifier;

/**
 * Defines the interface of the SMP Client that can be used to request the meta-data about a participant in the 
 * network from a SMP server. To create an SMP Client instance use the {@link SMPClientBuilder} to setup the network
 * specific parameters and create the client instance. 
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public interface ISMPClient {

	/**
     * Gets the endpoint's meta-data for the given participant, service and process and that supports the requested
     * transport profile.
     * <p>NOTE: All versions of the SMP specification a specific process identifier is defined to indicate that a 
     * service is not bound to any specific process (<i>busdox:noprocess</i> in PEPPOL version and <i>bdx:noprocess</i>)
     * in the OASIS versions). To search for the meta-data of such a service a {@link ProcessIdentifier} with the 
     * <i>no process</i> indicator should be provided as the <code>processId</code> parameter of this method.
     *
     * @param participantId		Participant's Id
     * @param serviceId			Service Id
     * @param processId			Process Id
     * @param transportProfile	Requested transport profile name
     * @return	The endpoint meta-data if there exists an endpoint for this participant, service and process and which
     * 			supports the requested transport profile, <code>null</code> otherwise.
     * @throws SMPQueryException 	When an error occurs in the lookup of the SMP location or querying the SMP server
     */
	EndpointInfo getEndpoint(final Identifier participantId,
    						 final Identifier serviceId,
    						 final Identifier processId,
    						 final String     transportProfile) throws SMPQueryException;
    
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
     * 			<code>null</code> otherwise.
     * @throws SMPQueryException 	When an error occurs in the lookup of the SMP location or querying the SMP server
     */
    List<EndpointInfo> getEndpoints(final Identifier participantId,
							        final Identifier serviceId,
							        final Identifier processId) throws SMPQueryException;
    
	/**
     * Gets the endpoint's meta-data for the given participant acting in the specified role for the given service and 
     * process and that supports the requested transport profile.
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
     * @param transportProfile	Requested transport profile name
     * @return	The endpoint meta-data if there exists an endpoint for this participant, service and process and which
     * 			supports the requested transport profile, <code>null</code> otherwise.
     * @throws SMPQueryException 	When an error occurs in the lookup of the SMP location or querying the SMP server
     * @since 2.0.0
     */
	EndpointInfo getEndpoint(final Identifier participantId,
							 final Identifier role,
    						 final Identifier serviceId,
    						 final Identifier processId,
    						 final String     transportProfile) throws SMPQueryException;    

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
	 * 			<code>null</code> otherwise.
	 * @throws SMPQueryException 	When an error occurs in the lookup of the SMP location or querying the SMP server
	 * @since 2.0.0
	 */
	List<EndpointInfo> getEndpoints(final Identifier participantId,
								    final Identifier role,
								    final Identifier serviceId,
								    final Identifier processId) throws SMPQueryException;    
}
