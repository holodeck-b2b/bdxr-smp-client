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
 * Represents a <i>Process Identifier</i> as used in the PEPPOL network based on the PEPPOL <i>Policy for use of
 * Identifiers</i>, version 4.0. Policy 2 states that the Process ID must be treated <b>case sensitive</b>. Therefore
 * this class creates case sensitive identifiers.
 *   
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  2.0.0
 */
public class ProcessID extends Identifier {

    /**
     * Default constructor that can be used by sub classes if they want to allow <code>null</code> values.
     */
    protected ProcessID() {
    	super();
    	this.caseSensitive = true;
    }
    
    /**
     * Creates a new identifier without indicating a scheme
     *
     * @param id    The identifier value
     */
    public ProcessID(final String id) {
        super(id, null, true);
    }

    /**
     * Creates a new identifier that is defined in the given scheme.
     *
     * @param id        The identifier value
     * @param scheme    The scheme in which the id is defined
     */
    public ProcessID(final String id, final String scheme) {
    	super(id, scheme, true);
    }
}
