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
package org.holodeckb2b.bdxr.impl.peppol;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.holodeckb2b.bdxr.smp.api.IHostNameGenerator;
import org.holodeckb2b.bdxr.smp.api.ISMPLocator;
import org.holodeckb2b.bdxr.smp.api.SMPLocatorException;
import org.holodeckb2b.bdxr.smp.datamodel.Identifier;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.TextParseException;

/**
 * Implements the {@link ISMPLocator} interface for locating the SMP for the participant using the PEPPOL SML
 * specification, i.e. just doing a DNS lookup for the generated host name based on the participant's identifier.
 * <p>When creating an instance of this locator the {@link IHostNameGenerator} to use for generating host names must be
 * provided.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class SMLLocator implements ISMPLocator {
    private static final Logger log = LogManager.getLogger(SMLLocator.class);

    /**
     * The host name generator to create the domain name to use for executing the SML query
     */
    private final IHostNameGenerator    hostnameGenerator;

    /**
     * Create a new <code>SMLLocator</code> instance that will use the given generator to create the host names for
     * participants.
     *
     * @param hostnameGenerator     The host name to use for generation of host names
     */
    public SMLLocator(IHostNameGenerator hostnameGenerator) {
        this.hostnameGenerator = hostnameGenerator;
    }

    /**
     * Executes the SML query to locate the SMP for the given participant identifier
     *
     * {@inheritDoc}
     */
    @Override
    public URL locateSMP(Identifier participant) throws SMPLocatorException {
        try {
            log.debug("Generate host name for participant identifier {}::{}", participant.getScheme(),
                      participant.getValue());
            final String hostname = hostnameGenerator.getHostNameForParticipant(participant);
            if (new Lookup(hostname).run() == null) {
            	log.warn("Participant with identifier {}::{} not registered in SML (lookup hostname={})", 
             				participant.getScheme(), participant.getValue(), hostname);
                throw new SMPLocatorException("Participant not registered in SML");
            } else {
                log.debug("Found SMP location [http://{}] for participant {}::{}", hostname, participant.getScheme(),
                           participant.getValue());
                try {
                	return new URL(String.format("http://%s", hostname)); 					
				} catch (MalformedURLException e) {
					log.error("Invalid host name registered in DNS: {}", hostname);
					throw new SMPLocatorException("Participant not correctly registered in SML");
				}                
            }
        } catch (TextParseException dnsQueryError) {
            log.error("Error in DNS query execution: {}", dnsQueryError.getMessage());
            throw new SMPLocatorException("Error in execution of DNS query", dnsQueryError);
        } catch (IllegalArgumentException unsupportedId) {
            log.warn("Unsupported participant identifier: {}::{}", participant.getScheme(), participant.getValue());
            throw new SMPLocatorException("Unsupported participant identifier", unsupportedId);
        }
    }
}
