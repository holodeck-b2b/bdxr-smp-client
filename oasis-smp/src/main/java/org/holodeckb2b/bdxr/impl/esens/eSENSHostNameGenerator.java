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
package org.holodeckb2b.bdxr.impl.esens;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.digest.DigestUtils;
import org.holodeckb2b.bdxr.smp.api.IHostNameGenerator;
import org.holodeckb2b.bdxr.smp.datamodel.Identifier;
import org.holodeckb2b.commons.util.Utils;

/**
 * Is a {@link IHostNameGenerator} that generates the host name according to the <a href=
 * "http://wiki.ds.unipi.gr/display/ESENS/PR+-+BDXL+1.4.0#PR-BDXL1.4.0-ImplementationGuidelines">e-SENS
 * specifications</a>. The algorithm to generate the hostname is as follows:<ol>
 * <li>Canonicalize the party identifier by creating an ebCore PartyId URN representation of the participant's id</li>
 * <li>Hash the canonicalized party identifier using the SHA-256 algorithm.</li>
 * <li>Encode the digest obtained in (2) using BASE32.</li>
 * <li>Remove any trailing '=' padding characters from the encoded digest.</li>
 * <li>Append the specified domain name to the encoded participant identifier obtained in (4).</li></ol>
 * <p>The domain name to which the generated host name is prefixed in step (5) must be provided upon initialization.
 * <p>NOTE 1: The e-SENS specification assumes that the participant identifier used is part of the ISO6523 naming
 * scheme or has no naming scheme at all (unregistered).
 * <p>NOTE 2: The ebCore PartyId URN contains both an identifier of the catalog as well of the scheme itself. This is 
 * different from the OASIS SMP Specification which only defines the scheme term. The ebCore catalog is handled here 
 * as the equivalent of the SMP scheme and the separate scheme identifier from ebCore is understood to be part of the
 * identifier value itself.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class eSENSHostNameGenerator implements IHostNameGenerator {
    /**
     * The allowed naming schemes
     */
    private final static Set allowedSchemes;
    static {
        allowedSchemes = new HashSet(2);
        allowedSchemes.add("iso6523");
        allowedSchemes.add("unregistered");
    }
    /**
     * The SML domain to append to the generated host names
     */
    private String  smlDomain;

    /**
     * Create a new host name generator that will append the provided domain name to generated host names.
     *
     * @param smlDomain     The SML domain
     */
    public eSENSHostNameGenerator(final String smlDomain) {
    	if (Utils.isNullOrEmpty(smlDomain))
    		throw new IllegalArgumentException("SML domain shall not be empty or null");
        this.smlDomain = smlDomain;
    }
    
    /**
     * Generates the host name for the given participant identifier according to e-SENS specification. 
     * 
     * @param participantId   The participant identifier
     * @return                Host name to use for BDXL lookup
     */
    @Override
    public String getHostNameForParticipant(Identifier participantId) {
        final String scheme = participantId.getScheme();
        if (!allowedSchemes.contains(scheme))
            throw new IllegalArgumentException(scheme + " is not a valid naming scheme according to e-SENS specs");

        final String canonicalId = "urn:oasis:tc:ebcore:partyid-type:" + scheme + ":" + participantId.getValue();
   	    byte[] encodedId = new Base32().encode(DigestUtils.sha256(canonicalId));

        // Strip all '=' (ASCII value = 61) at the end of the Base32 encoded value
        int  i = encodedId.length;
        while (encodedId[i-1] == 61)
            i--;

        return new String(encodedId, 0, i) + "." + smlDomain;
    }
}
