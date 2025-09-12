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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.holodeckb2b.bdxr.common.datamodel.Identifier;
import org.holodeckb2b.bdxr.common.datamodel.impl.IdentifierImpl;
import org.holodeckb2b.bdxr.smp.datamodel.IDScheme;
import org.holodeckb2b.bdxr.smp.datamodel.impl.IDSchemeImpl;

/**
 * Represents a Peppol Document Identifier as specified in section 5 of the Peppol <i>Policy for use of identifiers</i>.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 3.1.0	added support for new Peppol wildcard identifier scheme
 */
public class DocumentID extends IdentifierImpl {
	private static final long serialVersionUID = 7105921917201334861L;
	
	/**
	 * Scheme identifier of the default Peppol identifier scheme for document identifiers
	 */
	public static final IDScheme BUSDOX_QNS = new IDSchemeImpl("busdox-docid-qns", true);
	/**
	 * Scheme identifier of the Peppol wildcard identifier scheme. Introduced in version 4.2.0 of the PUI.
	 */
	public static final IDScheme PEPPOL_WILDCARD = new IDSchemeImpl("peppol-doctype-wildcard", true);
	
	/**
	 * The allowed identifier schemes for process identifiers as specified in Policy 16 of the Peppol "Policy for use of 
	 * identifiers".  
	 */
	public static final Set<IDScheme>  ALLOWED_SCHEMES = Set.of(BUSDOX_QNS, PEPPOL_WILDCARD);
	
	/**
	 * Regex pattern to parse a Peppol DocumentID as specified in policy 20 of the Peppol PUI.
	 */
	private static final Pattern PATTERN = Pattern.compile("(?<syntaxID>.*)##(?<custID>.+)::(?<version>.*)");
	
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
	
	/**
     * Creates a new instance by parsing the given URL encoded string representation of the Peppol DocumentID  
     * 
     * @param encodedId the URL encoded representation of the Peppol DocumentID
     * @return new instance representing the Peppol DocumentID
     * @throws IllegalArgumentException if the given scheme identifier does not represent either the busdox-docid-qns or
	 * 									Peppol wildcard scheme.
	 * @since 4.0.0
     */
 	public static DocumentID from(String encodedId) {
		Identifier id = IdentifierImpl.from(encodedId);
		
		if (!ALLOWED_SCHEMES.contains(id.getScheme()))
			throw new IllegalArgumentException("Invalid identifier scheme for Peppol Document Identifier");
				
		return new DocumentID(id.getValue(), id.getScheme().getSchemeId());
	}
	
 	/**
 	 * Gets the Syntax Specific ID of this document identifier. As specified by policy 20 of the PUI the Syntax Specific 
 	 * ID is the part of the Document ID before the first "##" character sequence. 
 	 * 
 	 * @return the Syntax Specific ID of this document identifier
 	 */
 	public String getSyntaxID() {
 		return getPart("syntaxID");
 	} 	
 	
 	/**
 	 * Gets the Custimization ID of this document identifier. As specified by policy 20 of the PUI the Custimization ID
 	 * is the part of the Document ID between the first "##" and following "::" character sequences. 
 	 * 
 	 * @return the Custimization ID of this document identifier
 	 */
 	public String getCustimizationID() {
 		return getPart("custID");
 	}
 	
 	/**
 	 * Gets the Version ID of this document identifier. As specified by policy 20 of the PUI the Version ID is the part 
 	 * of the Document ID after the last "::" character sequence. 
 	 * 
 	 * @return the Version ID of this document identifier
 	 */
 	public String getVersionID() {
 		return getPart("version");
 	}
 	
 	/**
 	 * Helper method to get a part of the document identifier.
 	 * 
 	 * @param name	the name of the part 
 	 * @return		the value of the part
 	 */
 	private String getPart(String name) {
 		Matcher matcher = PATTERN.matcher(value);
 		matcher.matches();
 		return matcher.group(name);
 	}
}
