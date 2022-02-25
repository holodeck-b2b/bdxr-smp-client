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
package org.holodeckb2b.bdxr.smp.client.api;

import java.time.LocalDateTime;
import org.holodeckb2b.bdxr.smp.datamodel.QueryResult;

/**
 * Represents an entry in the {@link IResultCache} that is used for caching the results of SMP queries. Caching as
 * specified in the OASIS SMP V2 specification is based only the last modified time stamp of the meta-data. The Holodeck
 * SMP client however also support "pure" local caching based on the time stamp of the last query. Therefore the entries
 * in the cache contain both the last modified and last query time stamps.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @see IResultCache
 * @see SMPClientBuilder#setResultCache(IResultCache)
 * @see SMPClientBuilder#enableLocalCaching()
 */
public interface ICachedResult {

	/**
	 * Gets the SMP result that was cached in the object representation as defined by in the SMP data model package
	 * {@link org.holodeckb2b.bdxr.smp.datamodel}.
	 *
	 * @return the cached SMP result
	 */
	QueryResult		getQueryResult();

	/**
	 * Gets the time stamp the meta-data included in the query result were last modified, as indicated by the SMP server
	 * in the <i>Last-Modified</i> HTTP header.
	 * <p>NOTE: Support for caching was introduced as an optional feature in OASIS SMP V2 specification, so the this
	 * time stamp may not be available for all results.
	 * <p>NOTE 2: Although the SMP server should be able to handle any time stamp when queried, we just copy the string
	 * value of the <i>Last-Modified</i> it provides to prevent any issues in time zone conversions.
	 *
	 * @return	the last modified time stamp of the cached query result
	 */
	String	getLastModified();

	/**
	 * Gets the time stamp of the last time the query the cached result applies to was executed.
	 *
	 * @return the last time the related query was executed
	 */
	LocalDateTime	getLastQueried();
}
