/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.contrib.changerequest.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ChangeRequestConfiguration;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.InvalidEntityReferenceException;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.text.StringUtils;

/**
 * Resolver helper to retrieve a document reference from a change request identifier.
 *
 * @version $Id$
 * @since 1.5.1
 */
@Component
@Singleton
@Named("changerequestid")
public class ChangeRequestIdDocumentReferenceResolver implements DocumentReferenceResolver<String>
{
    @Inject
    private ChangeRequestConfiguration configuration;

    @Override
    public DocumentReference resolve(String documentReferenceRepresentation, Object... parameters)
    {
        if (StringUtils.isBlank(documentReferenceRepresentation)) {
            throw new InvalidEntityReferenceException("The provided change request representation cannot be empty.");
        }

        SpaceReference spaceLocation = configuration.getChangeRequestSpaceLocation();
        return new DocumentReference(documentReferenceRepresentation, spaceLocation);
    }
}
