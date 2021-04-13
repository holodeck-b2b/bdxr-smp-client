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
package org.holodeckb2b.bdxr.smp.impl;

import java.net.MalformedURLException;
import java.net.URL;

import org.holodeckb2b.bdxr.smp.api.ISMPLocator;
import org.holodeckb2b.bdxr.smp.api.SMPLocatorException;
import org.holodeckb2b.bdxr.smp.datamodel.Identifier;

/**
 * Is an implementation of the {@link ISMPLocator} interface that returns a single SMP server for all participants.
 * This is useful in a environment where the connection meta-data is centrally managed, for example when there is one
 * central entity that authorizes who can be part of the network.   
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class StaticLocator implements ISMPLocator {
	/**
	 * The base URL of the SMP that should be used for all lookups
	 */
	private URL	smp;
	
	/**
	 * Creates a new instance of the static SMP locator that will direct all SMP queries to the specified URL.  
	 * 
	 * @param smpURL	The base URL of the single SMP to use for lookups
	 */
	public StaticLocator(final String smpURL) {
		try {
			this.smp = new URL(smpURL);
		} catch (NullPointerException | MalformedURLException invalidURI) {
			throw new IllegalArgumentException("Invalid URL specified");
		}
	}
	
	@Override
	public URL locateSMP(Identifier participant) throws SMPLocatorException {
		return smp;
	}

}
