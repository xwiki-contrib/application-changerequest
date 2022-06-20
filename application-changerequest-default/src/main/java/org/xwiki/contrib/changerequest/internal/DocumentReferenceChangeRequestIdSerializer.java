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

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

/**
 * Serializer helper to retrieve a change request identifier from a document reference containing a change request
 * object. The serialize method will only return a result if given entity reference is a document reference.
 *
 * @version $Id$
 * @since 0.14
 */
@Component
@Singleton
@Named("changerequestid")
public class DocumentReferenceChangeRequestIdSerializer implements EntityReferenceSerializer<String>
{
    @Override
    public String serialize(EntityReference reference, Object... parameters)
    {
        String result = null;
        if (reference.getType() == EntityType.DOCUMENT) {
            DocumentReference documentReference = (DocumentReference) reference;
            result = documentReference.getLastSpaceReference().getName();
        }
        return result;
    }
}
