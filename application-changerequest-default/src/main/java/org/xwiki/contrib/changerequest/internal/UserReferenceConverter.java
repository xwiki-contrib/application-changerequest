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

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.internal.document.DocumentUserReference;

/**
 * Allows to convert {@link UserReference} to {@link DocumentReference} when it's needed.
 * This component should be removed in the future, when all XWiki API are using {@link UserReference}.
 *
 * @version $Id$
 * @since 0.1
 */
@Component(roles = UserReferenceConverter.class)
@Singleton
public class UserReferenceConverter
{
    /**
     * Convert the given {@link UserReference} to a {@link DocumentReference}.
     *
     * @param userReference the reference to convert.
     * @return the {@link DocumentReference} of the user.
     */
    public DocumentReference convert(UserReference userReference)
    {
        DocumentReference reference = null;
        if (userReference instanceof DocumentUserReference) {
            DocumentUserReference documentUserReference = (DocumentUserReference) userReference;
            reference = documentUserReference.getReference();
        }
        return reference;
    }
}
