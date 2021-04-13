/*
 * Copyright (C) 2020 The Holodeck B2B Team
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

import org.holodeckb2b.bdxr.smp.datamodel.Identifier;

/**
 * Represents a <i>Participant Identifier</i> as used in the PEPPOL network based on the PEPPOL <i>Policy for use of
 * Identifiers</i>, version 4.0. Policy 2 states that the Participant ID must be treated case insensitively. Therefore
 * this class does not allow to create case sensitive identifiers.
 * <p>NOTE: The default {@link Identifier} class could also be used here as case insensitive handling is default but
 * since the DocumentID and ProcessID require case sensitive handling we also have a specific class for ParticipantID. 
 *   
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  2.0.0
 */
public class ParticipantID extends Identifier {

    /**
     * Default constructor that can be used by sub classes if they want to allow <code>null</code> values.
     */
    protected ParticipantID() {
    	super();
    }
    
    /**
     * Creates a new identifier without indicating a scheme
     *
     * @param id    The identifier value
     */
    public ParticipantID(final String id) {
        super(id, null, false);
    }

    /**
     * Creates a new identifier that is defined in the given scheme.
     *
     * @param id        The identifier value
     * @param scheme    The scheme in which the id is defined
     */
    public ParticipantID(final String id, final String scheme) {
    	super(id, scheme, false);
    }
}
