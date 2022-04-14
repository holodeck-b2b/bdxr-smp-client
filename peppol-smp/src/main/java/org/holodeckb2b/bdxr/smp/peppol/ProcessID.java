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

import org.holodeckb2b.bdxr.smp.datamodel.IDScheme;
import org.holodeckb2b.bdxr.smp.datamodel.impl.IDSchemeImpl;
import org.holodeckb2b.bdxr.smp.datamodel.impl.ProcessIdentifierImpl;

/**
 * Represents a Peppol Process Identifier.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class ProcessID extends ProcessIdentifierImpl {

	/**
	 * The required identifier scheme for process identifiers as specified in Policy 24 of
	 * the Peppol "Policy for use of identifiers"
	 */
	public static final IDScheme	CENBII = new IDSchemeImpl("cenbii-procid-ubl", true);

	public ProcessID(String procId) {
		super(procId, CENBII);
	}
}
