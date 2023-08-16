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

import java.util.Set;

import org.holodeckb2b.bdxr.smp.datamodel.IDScheme;
import org.holodeckb2b.bdxr.smp.datamodel.impl.IDSchemeImpl;
import org.holodeckb2b.bdxr.smp.datamodel.impl.IdentifierImpl;

/**
 * Represent a Peppol Document Identifier. The identifier schemes are restricted to the ones specified by Policy 16 of
 * the Peppol "Policy for use of identifiers" (version 4.2.0)
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 3.1.0	added support for new Peppol wildcard identifier scheme
 */
public class DocumentID extends IdentifierImpl {
	/**
	 * Scheme identifier of the default Peppol identifier scheme for document identifiers
	 */
	public static final IDScheme BUSDOX_QNS = new IDSchemeImpl("busdox-docid-qns", true);
	/**
	 * Scheme identifier of the Peppol wildcard identifier scheme
	 */
	public static final IDScheme PEPPOL_WILDCARD = new IDSchemeImpl("peppol-doctype-wildcard", true);
	
	/**
	 * The allowed identifier schemes for process identifiers as specified in Policy 16 of the Peppol "Policy for use of 
	 * identifiers", version 4.2.0 
	 */
	public static final Set<IDScheme>  ALLOWED_SCHEMES = Set.of(BUSDOX_QNS, PEPPOL_WILDCARD);
	
	/**
	 * Create a new DocumentID in the default "busdox-docid-qns" scheme.
	 * 
	 * @param docId		the document identifier value
	 */
	public DocumentID(String docId) {
		super(docId, BUSDOX_QNS);
	}
	
	/**
	 * Creates a new DocumentID in the specified scheme.
	 * 
	 * @param docId			the document identifier value
	 * @param schemeId		the scheme identifier
	 * @throws IllegalArgumentException if the given scheme identifier does not represent either the busdox-docid-qns or
	 * 									Peppol wildcard scheme.
	 * @since 3.1.0
	 */
	public DocumentID(String docId, String schemeId) {
		super();
		IDScheme idScheme = ALLOWED_SCHEMES.stream().filter(s -> s.getSchemeId().equals(schemeId))
													.findFirst().orElse(null);
		if (idScheme == null)
			throw new IllegalArgumentException("Invalid identifier scheme for Peppol Document Identifier");
		
		setValue(docId, idScheme);
	}
}
