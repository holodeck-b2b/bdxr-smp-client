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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.holodeckb2b.bdxr.common.datamodel.Identifier;
import org.holodeckb2b.bdxr.smp.client.api.IHostNameGenerator;
import org.holodeckb2b.bdxr.smp.client.api.ISMPLocator;
import org.holodeckb2b.bdxr.smp.client.api.SMPLocatorException;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.NAPTRRecord;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

/**
 * Implements the {@link ISMPLocator} interface for locating the SMP for the participant using the OASIS BDXL
 * specification, i.e. using NAPTR DNS records from the DNS entry for the generated host name based on the participant's
 * identifier.
 * <p>When creating an instance of this locator the {@link IHostNameGenerator} to use for generating host names and the
 * NAPTR service name for the record containing the SMP URL must be provided.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class BDXLLocator implements ISMPLocator {
    private static final Logger log = LogManager.getLogger(BDXLLocator.class);

    /**
     * The host name generator to create the domain name to use for executing the SML query
     */
    private final IHostNameGenerator    hostnameGenerator;
    /**
     * The NAPTR service name used to identify the record holding the SMP URL
     */
    private final String	naptrService;

    /**
     * Create a new <code>BDXLLocator</code> instance that will use the given generator to create the host names for
     * participants and NAPTR service name to get SMP location.
     *
     * @param hostnameGenerator     The host name to use for generation of host names
     * @param svcName				NAPTR service name of record holding SMP URL
     */
    public BDXLLocator(IHostNameGenerator hostnameGenerator, String svcName) {
        this.hostnameGenerator = hostnameGenerator;
        this.naptrService = svcName;
    }

    /**
     * Executes the BDXL query to locate the SMP for the given participant identifier
     *
     * {@inheritDoc}
     */
    @Override
    public URL locateSMP(Identifier participant) throws SMPLocatorException {
        log.debug("Generate host name for participant identifier {}", participant.toString());
        final String hostname = hostnameGenerator.getHostNameForParticipant(participant);
        URL smpURL = null;
        try {
        	log.debug("Retrieving SMP location using {} U-NAPTR record for {}", naptrService, hostname);
        	smpURL = retrieveURL(hostname);
        } catch (SMPLocatorException queryError) {
        	log.error("Could not retrieve SMP registration for participant ({}) due to DNS error: {}",
        			  participant.toString(), queryError.getMessage());
        	throw queryError;
        }
        if (smpURL == null) {
        	log.warn("Participant with identifier {}::{} not registered.", participant.getScheme(),
        			participant.getValue());
        	throw new SMPLocatorException("Participant not registered");
        }
        log.debug("Found SMP URL for participant ({}) = {}", participant.toString(), smpURL.toString());
        return smpURL;
    }

    /**
     * Retrieves the URL of the SMP registered from the U-NAPTR record of the given host name. As specified
     * in <a href="https://tools.ietf.org/html/rfc4848">RFC4848</a> the U-NAPTR record can include a
     * "redirection" to another host name.
     *
     * @param hostname initial host name to query for the SMP URL
     * @return	the SMP URL retrieved from the U-NAPTR record for the given host name or its replacement
     * @throws SMPLocatorException if the retrieved U-NAPTR record contains an invalid regexp
     */
    private URL retrieveURL(final String hostname) throws SMPLocatorException {
        // Fetch all records of type NAPTR registered on hostname.
		log.trace("Retrieving all NAPTR records for {}", hostname);
        org.xbill.DNS.Record[] records = null;
		try {
			records = new Lookup(hostname, Type.NAPTR).run();
		} catch (TextParseException dnsQueryError) {
            log.error("Error in DNS query execution: {}", dnsQueryError.getMessage());
            throw new SMPLocatorException("Error in execution of DNS query", dnsQueryError);
		}
        if (records == null) {
        	log.debug("No NAPTR records found for {}", hostname);
        	return null;
        }
        // Loop records found.
        for (org.xbill.DNS.Record record : records) {
            // Simple cast possible because we only retrieved NAPTR records
            NAPTRRecord naptrRecord = (NAPTRRecord) record;
            /* Handle those having the requested service: for BDXL we support only the records
             * with U flag which should contain a URL to the service or ones without flag which
             * should redirect to another DNS entry that should queried for the service URL
             */
            if (!naptrService.equals(naptrRecord.getService()))
            	continue;

        	if ("U".equalsIgnoreCase(naptrRecord.getFlags())) {
                log.trace("Found U-NAPTR record, retrieving URL");
                /*
                 * As BDXL is based on U-NAPTR the regular expression in the NAPTR record must always
                 * be in the format "!.*!<URL>!" there is no need for evaluation and the URL part of
                 * the expression can be directly used.
                 */
                final String regexp = naptrRecord.getRegexp();
                final String[] parts = regexp != null ? regexp.split("!") : null;
                try {
                	return new URL(parts != null && parts.length > 2 ? parts[2] : null);
				} catch (MalformedURLException e) {
					log.error("Invalid U-NAPTR record: {}", regexp);
					throw new SMPLocatorException("Invalid U-NAPTR record");
                }
        	} else if ("".equalsIgnoreCase(naptrRecord.getFlags())) {
        		log.trace("Found replacement NAPTR record, requery with replacement");
        		return retrieveURL(naptrRecord.getReplacement().toString());
        	}
        }
        // No U-NAPTR records found
    	log.debug("No U-NAPTR records for {} service found for {}", naptrService, hostname);
    	return null;
    }
}
