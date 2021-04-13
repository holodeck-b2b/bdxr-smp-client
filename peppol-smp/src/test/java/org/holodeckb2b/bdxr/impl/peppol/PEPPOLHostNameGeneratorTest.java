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


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.holodeckb2b.bdxr.smp.api.IHostNameGenerator;
import org.holodeckb2b.bdxr.smp.datamodel.Identifier;
import org.junit.jupiter.api.Test;

/**
 * Tests the generation of host name for SML queries based on the PEPPOL rules.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class PEPPOLHostNameGeneratorTest {

    private static final String     VALID_SML_DOMAIN = "test-sml.holodeck-b2b.com";
    private static final Identifier VALID_ID = new Identifier("0088:9999", "iso6523-actorid-upis");
    private static final String     MD5_HASH = "626eb8aec73e5a0cf24c96e35be8ed08";
    private static final Identifier NO_SCHEME_ID = new Identifier("0000:xxxx");


    @Test
    public void testEmptyParameterSet() {
        try {
            IHostNameGenerator  generator = new PEPPOLHostNameGenerator("");
            fail("Empty SML domain should not be accepted");
        } catch (IllegalArgumentException correct) {
        }
    }

    @Test
    public void testNullOrEmptySMLDomain() {
    try {
            IHostNameGenerator  generator = new PEPPOLHostNameGenerator(null);
            fail("null SML domain should not be accepted");
        } catch (IllegalArgumentException correct) {
        }
    }

    @Test
    public void testGenerateForValidParticipantID() {
        IHostNameGenerator peppolGenerator = new PEPPOLHostNameGenerator(VALID_SML_DOMAIN);
        assertEquals("B-"+ MD5_HASH + "." + VALID_ID.getScheme() + "." + VALID_SML_DOMAIN,
                     peppolGenerator.getHostNameForParticipant(VALID_ID));
    }

    @Test
    public void testNoSchemeParticipantId() {
        IHostNameGenerator peppolGenerator = new PEPPOLHostNameGenerator(VALID_SML_DOMAIN);
        try {
            peppolGenerator.getHostNameForParticipant(NO_SCHEME_ID);
            fail("Should not generate a host name for identifier without scheme");
        } catch (IllegalArgumentException noScheme) {
        }
    }
}
