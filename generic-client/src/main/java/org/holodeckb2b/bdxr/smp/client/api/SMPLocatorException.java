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
package org.holodeckb2b.bdxr.smp.client.api;

/**
 * Is an exception to indicate that an error occurred when locating the SMP server for a participant.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class SMPLocatorException extends SMPQueryException {

    public SMPLocatorException(final String errorMessage) {
        super(errorMessage);
    }

    public SMPLocatorException(final String errorMessage, final Throwable cause) {
        super(errorMessage, cause);
    }
}
