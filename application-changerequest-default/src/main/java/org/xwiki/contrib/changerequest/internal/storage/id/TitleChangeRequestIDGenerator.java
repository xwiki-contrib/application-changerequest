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
package org.xwiki.contrib.changerequest.internal.storage.id;

import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.storage.ChangeRequestIDGenerator;
import org.xwiki.model.validation.EntityNameValidation;

/**
 * Implementation of {@link ChangeRequestIDGenerator} that relies on the change request title.
 * This is the default implementation.
 *
 * @version $Id$
 * @since 0.1
 */
@Component
@Singleton
public class TitleChangeRequestIDGenerator implements ChangeRequestIDGenerator
{
    static final int MAX_LENGTH = 64;

    @Inject
    @Named("SlugEntityNameValidation")
    private Provider<EntityNameValidation> slugEntityNameValidation;

    @Override
    public String generateId(ChangeRequest changeRequest)
    {
        String sanitizedTitle = this.slugEntityNameValidation.get().transform(changeRequest.getTitle());
        String uuid = UUID.randomUUID().toString();
        if (sanitizedTitle.length() + uuid.length() > MAX_LENGTH + 1) {
            int offset = MAX_LENGTH - uuid.length() - 1;
            sanitizedTitle = sanitizedTitle.substring(0, offset);
        }
        return String.format("%s-%s", sanitizedTitle, uuid);
    }
}
