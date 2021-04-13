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

import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.digest.DigestUtils;
import org.holodeckb2b.bdxr.impl.oasis_smp1.OASISv1ResultProcessor;
import org.holodeckb2b.bdxr.smp.api.IHostNameGenerator;
import org.holodeckb2b.bdxr.smp.api.ISMPClient;
import org.holodeckb2b.bdxr.smp.api.SMPClientBuilder;
import org.holodeckb2b.bdxr.smp.api.SMPQueryException;
import org.holodeckb2b.bdxr.smp.datamodel.EndpointInfo;
import org.holodeckb2b.bdxr.smp.datamodel.Identifier;
import org.holodeckb2b.bdxr.smp.impl.BDXLLocator;

/**
 * Is a simple example application that shows how the {@link ISMPClient} can be used for querying the CEF Connectivity
 * test SMP server.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class CEFConnTestClient {
	/**
	 * The SML domain used by CEF for connectivity testing 
	 */
    private static final String CEFTEST_TEST_SML_DOMAIN = "connectivitytest.acc.edelivery.tech.ec.europa.eu";
    /**
     * The transport protocol identifier used for AS4 
     */
	private static final String AS4_TRANSPORT_ID = "bdxr-transport-ebms3-as4-v1p0";
	
    
	public static void main(String[] args) {
		new CEFConnTestClient().run(args);
	}
	
	public void run(String[] args) {
		if (args.length < 3) {
			System.out.println("Missing arguments! This program requires 3 parameters which are the identifiers to use for querying the SMP:");
			System.out.println("1) participant identifier");
			System.out.println("2) document identifier");
			System.out.println("3) process identifier");
			System.out.println("Specify only the identifiers value, without scheme as these are fixed");
			System.exit(-1);
		}
		
		final Identifier participantId = new Identifier(args[0], "connectivity-partid-qns");
		final Identifier serviceId = new Identifier(args[1], "connectivity-docid-qns");
		final Identifier processId = new Identifier(args[2], "connectivity-procid-qns");
		final String transportId = AS4_TRANSPORT_ID;
		
		System.out.println("Performing SMP query using parameters:");
		System.out.println("- Participant identifier: " + participantId.toString());
		System.out.println("- Document identifier   : " + serviceId.toString());
		System.out.println("- Process identifier    : " + processId.toString());
		System.out.println("- Transport profile 	: " + transportId);
		
		final ISMPClient lookupClient = new SMPClientBuilder().setSMPLocator(new BDXLLocator(
																					new CEFHostNameGenerator(),
																					"Meta:SMP"))
															  .addProcessor(new OASISv1ResultProcessor())
															  .build();
		try {
			System.out.println("Executing SMP query...");
			EndpointInfo endpointInfo = lookupClient.getEndpoint(participantId, serviceId, processId, transportId);
			System.out.println("Executed SMP query!");
			
			System.out.print("The participant can receive the given document for the specified process through ");
			System.out.println("the AP available at " + endpointInfo.getEndpointURL());
			if (AS4_TRANSPORT_ID.equals(endpointInfo.getTransportProfile())) {
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

	class CEFHostNameGenerator implements IHostNameGenerator {
	    
	    /**
	     * Generates the host name for the given participant identifier in CEF connectivity test environment 
	     * 
	     * @param participantId   The participant identifier
	     * @return                Host name to use for BDXL lookup
	     */
	    @Override
	    public String getHostNameForParticipant(Identifier participantId) {
	        final String canonicalId = participantId.getValue();
	   	    byte[] encodedId = new Base32().encode(DigestUtils.sha256(canonicalId));

	        // Strip all '=' (ASCII value = 61) at the end of the Base32 encoded value
	        int  i = encodedId.length;
	        while (encodedId[i-1] == 61)
	            i--;

	        return new String(encodedId, 0, i) + "." + participantId.getScheme() + "." + CEFTEST_TEST_SML_DOMAIN;
	    }
	}	
}
