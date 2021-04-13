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
package org.holodeckb2b.bdxr.smp.api;

import org.holodeckb2b.bdxr.smp.datamodel.Identifier;

/**
 * Defines the interface for <i>host name generators</i> which based on the participant identifier generate a host name
 * that can be used by the {@link ISMPLocator} to locate the service meta-data provider.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public interface IHostNameGenerator {

    /**
     * Generate the host name for the given participant identifier.
     *
     * @param participantId  The participant's identifier
     * @return               Host name to use for SML query.
     */
    String getHostNameForParticipant(final Identifier participantId);
}
