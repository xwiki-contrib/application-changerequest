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

import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.XWikiContext;

/**
 * Specific resolver to resolve a {@link ChangeRequest} as a {@link DocumentReference} since they are stored as
 * document.
 *
 * @version $Id$
 * @since 0.1
 */
@Component
@Singleton
public class ChangeRequestDocumentReferenceResolver implements DocumentReferenceResolver<ChangeRequest>
{
    @Inject
    private Provider<XWikiContext> contextProvider;

    @Override
    public DocumentReference resolve(ChangeRequest changeRequest, Object... parameters)
    {
        return new DocumentReference(new LocalDocumentReference(Arrays.asList("XWiki", "ChangeRequest"),
            changeRequest.getId()), this.contextProvider.get().getWikiReference());
    }
}
