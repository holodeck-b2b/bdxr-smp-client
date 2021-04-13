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
package org.holodeckb2b.bdxr.examples;

import java.security.cert.X509Certificate;

import org.holodeckb2b.bdxr.impl.peppol.DocumentID;
import org.holodeckb2b.bdxr.impl.peppol.PEPPOLHostNameGenerator;
import org.holodeckb2b.bdxr.impl.peppol.PEPPOLResultProcessor;
import org.holodeckb2b.bdxr.impl.peppol.ParticipantID;
import org.holodeckb2b.bdxr.impl.peppol.ProcessID;
import org.holodeckb2b.bdxr.impl.peppol.SMLLocator;
import org.holodeckb2b.bdxr.smp.api.ISMPClient;
import org.holodeckb2b.bdxr.smp.api.SMPClientBuilder;
import org.holodeckb2b.bdxr.smp.api.SMPQueryException;
import org.holodeckb2b.bdxr.smp.datamodel.EndpointInfo;
import org.holodeckb2b.bdxr.smp.datamodel.Identifier;

/**
 * Is a simple example application that shows how the {@link ISMPClient} can be used for querying PEPPOL SMP servers.
 * The application takes 
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class PEPPOLSMPClient {
	/**
	 * The SML domain used by OpenPEPPOL for testing 
	 */
    private static final String OPENPEPPOL_TEST_SML_DOMAIN = "acc.edelivery.tech.ec.europa.eu";
    /**
     * The transport protocol identifier used in OpenPEPPOL for AS2 
     */
    private static final String OPENPEPPOL_AS2_TRANSPORT_ID = "busdox-transport-as2-ver1p0";
    /**
     * The transport protocol identifier used in OpenPEPPOL for AS4 
     */
	private static final String OPENPEPPOL_AS4_TRANSPORT_ID = "peppol-transport-as4-v2_0";
	
    
	public static void main(String[] args) {
		
		if (args.length < 4) {
			System.out.println("Missing arguments! This program requires 4 parameters which are the identifiers to use for querying the SMP:");
			System.out.println("1) participant identifier");
			System.out.println("2) document identifier");
			System.out.println("3) process identifier");
			System.out.println("4) transport protocol indicator: as2 or as4");
			System.out.println("Specify only the identifiers value, without scheme as these are fixed");
			System.exit(-1);
		}
		
		final Identifier participantId = new ParticipantID(args[0], "iso6523-actorid-upis");
		final Identifier serviceId = new DocumentID(args[1], "busdox-docid-qns");
		final Identifier processId = new ProcessID(args[2], "cenbii-procid-ubl");
		String transportId = null;
		if ("as2".equalsIgnoreCase(args[3]))
			transportId = OPENPEPPOL_AS2_TRANSPORT_ID;
		else if ("as4".equalsIgnoreCase(args[3]))
			transportId = OPENPEPPOL_AS4_TRANSPORT_ID;
		else {
			System.out.println("Unknown transport protocol [" + args[3] + "] specified!");
			System.exit(-1);
		}
		
		System.out.println("Performing SMP query using parameters:");
		System.out.println("- Participant identifier: " + participantId.toString());
		System.out.println("- Document identifier   : " + serviceId.toString());
		System.out.println("- Process identifier    : " + processId.toString());
		System.out.println("- Transport protocol 	: " + args[3]);
		
		final ISMPClient lookupClient = new SMPClientBuilder().setSMPLocator(new SMLLocator(
															new PEPPOLHostNameGenerator(OPENPEPPOL_TEST_SML_DOMAIN)))
															  .addProcessor(new PEPPOLResultProcessor())
															  .build();
		try {
			System.out.println("Executing SMP query...");
			EndpointInfo endpointInfo = lookupClient.getEndpoint(participantId, serviceId, processId, transportId);
			System.out.println("Executed SMP query!");
			
			System.out.print("The participant can receive the given document for the specified process through ");
			System.out.println("the AP available at " + endpointInfo.getEndpointURL());
			if (OPENPEPPOL_AS4_TRANSPORT_ID.equals(endpointInfo.getTransportProfile())) {
				System.out.println("AS4 Messages send to the AP should be encrypted using Certificate:");
				X509Certificate cert = endpointInfo.getCertificates().get(0).getX509Cert();
				System.out.println("\tSubject         : " + cert.getSubjectDN().getName());
				System.out.println("\tIssuer/serialNo : " + cert.getIssuerX500Principal().getName() + "/" +
															cert.getSerialNumber().toString());
			}			
		} catch (SMPQueryException queryFailed) {
			System.out.println("A problem occurred when executing the SMP query!");
			queryFailed.printStackTrace();
		}
	}

}
