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

import java.net.URL;
import java.time.LocalDateTime;
import org.holodeckb2b.bdxr.smp.datamodel.QueryResult;

/**
 * Defines the interface of the component responsible for caching query results. The cache is defined as a map from
 * query URLs to the resulting query response.
 * <p>Beside the three methods defined in this interface used by the SMP client to create, update and get entries from
 * the cache, implementations must also have a memory management function to ensure the cache does not create memory
 * overflows. When the cache needs to remove results because it would otherwise overflow it must evict the entry with
 * the oldest <i>last queried</i> time stamp. All operations on the cache must be thread safe.<br/>
 * How the cache is configured and initialised is out of scope of this interface and left to implementations.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @see SMPClientBuilder#setResultCache(IResultCache)
 */
public interface IResultCache {

	/**
	 * Stores the query result and its associated <i>last modified</i> time stamp for the given query URL in the cache.
	 * <p>When the cache does not yet contain a result for the given URL, a new cache entry must be created and added to
	 * the cache. If the new entry causes an overflow the oldest entry should be evicted.<br/>
	 * If the cache entry for the given query URL has a <i>last queried</i> older than the given query time, it should
	 * be updated. Otherwise the update should be ignored, so the latest query result is cached.
	 *
	 * @param query		query URL
	 * @param result	query result
	 * @param lastModified	value of the <i>Last-Modified</i> as provided by the SMP server. May be <code>null</code>
	 *						when the server does not support caching
	 * @param queryTime		time stamp when the query was last executed by the client
	 * @return the cache entry for the given URL. Note that the query result in the returned entry ma be different from
	 *		   the given one in case there already existed an entry with a newer query time.
	 */
	ICachedResult	storeResult(URL query, QueryResult result, String lastModified, LocalDateTime queryTime);

	/**
	 * Gets the cache entry for the given query URL.
	 *
	 * @param query		query URL
	 * @return the cache entry for the given URL
	 */
	ICachedResult	getCachedResult(URL query);

	/**
	 * Sets the <i>last queried</i> time stamp for the given query URL.
	 * <p>The cache should only update the query time in case the given time is later than the one currently stored for
	 * the given URL.
	 *
	 * @param query		query URL
	 * @param queryTime time stamp when the query was last executed by the client
	 */
	void updateLastQueried(URL query, LocalDateTime queryTime);
}
