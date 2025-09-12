/*
 * Copyright (C) 2022 The Holodeck B2B Team
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
package org.holodeckb2b.bdxr.smp.peppol;

import org.holodeckb2b.bdxr.common.datamodel.impl.ProcessIdentifierImpl;
import org.holodeckb2b.bdxr.smp.datamodel.IDScheme;
import org.holodeckb2b.bdxr.smp.datamodel.impl.IDSchemeImpl;

/**
 * Represents a Peppol Process Identifier.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class ProcessID extends ProcessIdentifierImpl {
	private static final long serialVersionUID = -2028512758224367184L;
	
	/**
	 * The required identifier scheme for process identifiers as specified in Policy 24 of
	 * the Peppol "Policy for use of identifiers"
	 */
	public static final IDScheme	CENBII = new IDSchemeImpl("cenbii-procid-ubl", true);

	/**
	 * Create a new ProcessID in the default "cenbii-procid-ubl" scheme.
	 * 
	 * @param procId		the process identifier value
	 */
	public ProcessID(String procId) {
		super(procId, CENBII);
	}
	
	/**
	 * Creates a new ProcessID in the specified scheme.
	 * <p>NOTE: Although the scheme must the cenbii scheme these constructor was added to be consistent with  
	 * {@link DocumentID} and allow for additional schemes that may be added in future versions of the Peppol specs. 
	 * 
	 * @param procId		the process identifier value
	 * @param schemeId		the scheme identifier
	 * @throws IllegalArgumentException if the given scheme identifier does not represent the cenbii-procid-ubl scheme
	 * @since 3.1.0
	 */	
	public ProcessID(String procId, String schemeId) {
		super();
		if (!CENBII.getSchemeId().equals(schemeId))
			throw new IllegalArgumentException("The identifier scheme for a Peppol Process ID must be CENBII");		
		
		setValue(procId, CENBII);
	}
}
