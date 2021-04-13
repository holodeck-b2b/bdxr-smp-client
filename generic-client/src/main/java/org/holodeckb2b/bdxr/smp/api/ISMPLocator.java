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

import java.net.URL;

import org.holodeckb2b.bdxr.smp.datamodel.Identifier;
import org.holodeckb2b.bdxr.smp.impl.SMPClient;

/**
 * Defines the interface for the component responsible for locating the <i>service metadata provider</i> where a
 * participant has registered the information on the documents it can receive.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public interface ISMPLocator {

    /**
     * Gets the base URL of the SMP that serves the meta-data for the given participant and which the SMP Client will 
     * extend to execute the query.
     * <p>NOTE: The {@link SMPClient} expects the result of the locator to be a <b>complete base URL</b> to which it
     * can simply add the correct context path to execute the query, e.g. just add <i>/{part. id}/services/{svc ID}</i>.
     * This implies that for OASIS SMP V2 lookups the locator must ensure that the <i>bdxr-smp-2</i> is included in the
     * response.
     *
     * @param participant   The identifier of the participant
     * @return              Base URL of the SMP serving the participant
     * @throws SMPLocatorException  When there is a problem in locating the SMP for the given participant. This can be
     *                              caused by in error in the lookup, an invalid participant identifier or that there
     *                              is no SMP registered for the participant.
     */
    URL locateSMP(final Identifier participant) throws SMPLocatorException;
}
