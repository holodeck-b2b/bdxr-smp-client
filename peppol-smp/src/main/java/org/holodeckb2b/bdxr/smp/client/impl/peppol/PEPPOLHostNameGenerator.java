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
import org.holodeckb2b.bdxr.smp.client.api.IHostNameGenerator;
import org.holodeckb2b.bdxr.smp.datamodel.Identifier;
import org.holodeckb2b.commons.util.Utils;

/**
 * Is a {@link IHostNameGenerator} that generates the host name according to the rules specified by the PEPPOL eDelivery
 * Network. These specify that the host name must be constructed as follows:<br>
 * <code><i>"B-"</i> + «MD5 hash of the <b>lower case</b> participant identifier» + "." + «identifier scheme» + "." + «SML Domain»</code>
 * <br>The SML domain to append should be provided to the generator upon creation.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class PEPPOLHostNameGenerator implements IHostNameGenerator {
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

        return "B-" + DigestUtils.md5Hex(participantId.getValue().toLowerCase()) + "." + schemeId + "." + smlDomain;
    }

}
