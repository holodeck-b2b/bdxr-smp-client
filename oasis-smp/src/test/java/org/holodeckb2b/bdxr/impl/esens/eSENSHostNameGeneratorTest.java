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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.holodeckb2b.bdxr.smp.api.IHostNameGenerator;
import org.holodeckb2b.bdxr.smp.datamodel.Identifier;
import org.junit.jupiter.api.Test;

/**
 * Tests the generation of host name for SML queries based on the e-SENS rules.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class eSENSHostNameGeneratorTest {

    private static final String     VALID_SML_DOMAIN = "test-sml.holodeck-b2b.com";
    private static final Identifier VALID_ID = new Identifier("0088:9999", "iso6523");
    private static final String     B32_SHA256_HASH = "ZAS3CFEF3EL2XW3TCIMYF6YT253A735R7X7E2F6NYJGUEOFZ7JLQ";
    private static final Identifier NO_SCHEME_ID = new Identifier("0000:xxxx");
    private static final Identifier INVALID_SCHEME_ID = new Identifier("0000:yyyy", "gs1");


    @Test
    public void testNullSMLDomain() {
        try {
            IHostNameGenerator  generator = new eSENSHostNameGenerator(null);
            fail("null SML domain should not be accepted");
        } catch (IllegalArgumentException correct) {
        }
    }

    @Test
    public void testEmptySMLDomain() {
        try {
            IHostNameGenerator  generator = new eSENSHostNameGenerator("");
            fail("Empty SML domain should not be accepted");
        } catch (IllegalArgumentException correct) {
        }
    }

    @Test
    public void testGenerateForValidParticipantID() {
        IHostNameGenerator generator = new eSENSHostNameGenerator(VALID_SML_DOMAIN);
        assertEquals(B32_SHA256_HASH + "." + VALID_SML_DOMAIN,
                     generator.getHostNameForParticipant(VALID_ID));
    }

    @Test
    public void testNoSchemeParticipantId() {
        IHostNameGenerator generator = new eSENSHostNameGenerator(VALID_SML_DOMAIN);
        try {
            generator.getHostNameForParticipant(NO_SCHEME_ID);
            fail("Should not generate a host name for identifier without scheme");
        } catch (IllegalArgumentException noScheme) {
        }
    }

    @Test
    public void testInvalidSchemeParticipantId() {
        IHostNameGenerator generator = new eSENSHostNameGenerator(VALID_SML_DOMAIN);
        try {
            generator.getHostNameForParticipant(INVALID_SCHEME_ID);
            fail("Should not generate a host name for identifier with unknown scheme");
        } catch (IllegalArgumentException noScheme) {
        }
    }
}
