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
package org.holodeckb2b.bdxr.smp.client.impl.peppol;

import org.apache.commons.codec.digest.DigestUtils;
import org.holodeckb2b.bdxr.common.datamodel.Identifier;
import org.holodeckb2b.bdxr.smp.client.api.IHostNameGenerator;
import org.holodeckb2b.commons.util.Utils;
import org.xbill.DNS.utils.base32;

/**
 * Is a {@link IHostNameGenerator} that generates the host name according to the rules specified the PEPPOL eDelivery 
 * Network's SML specification version 1.3.0 and Policy for use of Identifiers version 4.4.0. These specify that the 
 * host name must be constructed as follows:<br>
 * <code>«Base32 encoding of the SHA-256 hash of the <b>lower case</b> participant identifier with trailing '=' removed» 
 * 		+ "." + «identifier scheme» + "." + «SML Domain»</code>
 * <br>The SML domain to append should be provided to the generator upon creation.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class PEPPOLHostNameGenerator implements IHostNameGenerator {
	/**
	 * The base32 encoder configured according to the PEPPOL specification, i.e. no padding
	 */
	private static final base32 BASE32 = new base32(base32.Alphabet.BASE32, false, false);
		
	/**
     * The SML domain to append to the generated host names
     */
    private final String  smlDomain;

    /**
     * Create a new host name generator that will append the provided domain name to generated host names.
     *
     * @param smlDomain     The SML domain
     */
    public PEPPOLHostNameGenerator(final String smlDomain) {
        if (Utils.isNullOrEmpty(smlDomain))
            throw new IllegalArgumentException("SML domain shall not be empty or null");
        this.smlDomain = smlDomain;
    }

    @Override
    public String getHostNameForParticipant(Identifier participantId) {
        final String schemeId = participantId.getScheme() != null ? participantId.getScheme().getSchemeId() : null;
        if (Utils.isNullOrEmpty(schemeId))
            throw new IllegalArgumentException("The participant identifier scheme must be set");

        return BASE32.toString(DigestUtils.sha256(participantId.getValue().toLowerCase())) + "." + schemeId + "." + smlDomain;
    }

}
