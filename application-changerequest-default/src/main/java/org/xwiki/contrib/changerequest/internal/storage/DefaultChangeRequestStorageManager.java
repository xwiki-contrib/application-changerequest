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
package org.xwiki.contrib.changerequest.internal.storage;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.internal.UserReferenceConverter;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.storage.ChangeRequestStorageManager;
import org.xwiki.contrib.changerequest.storage.FileChangeStorageManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Default implementation of {@link ChangeRequestStorageManager}.
 * The change request are stored as XWiki documents located on a dedicated space.
 *
 * @version $Id$
 * @since 0.1
 */
@Component
@Singleton
public class DefaultChangeRequestStorageManager implements ChangeRequestStorageManager
{
    private static final LocalDocumentReference CHANGE_REQUEST_XCLASS =
        new LocalDocumentReference("ChangeRequest", "ChangeRequestClass");
    @Inject
    private UserReferenceConverter userReferenceConverter;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private FileChangeStorageManager fileChangeStorageManager;

    @Inject
    private DocumentReferenceResolver<ChangeRequest> changeRequestDocumentReferenceResolver;

    @Override
    public void saveChangeRequest(ChangeRequest changeRequest) throws ChangeRequestException
    {
        XWikiContext context = this.contextProvider.get();
        XWiki wiki = context.getWiki();
        DocumentReference reference = this.changeRequestDocumentReferenceResolver.resolve(changeRequest);
        try {
            XWikiDocument document = wiki.getDocument(reference, context);
            document.setTitle(changeRequest.getTitle());
            document.setContent(changeRequest.getDescription());
            document.setContentAuthorReference(this.userReferenceConverter.convert(changeRequest.getCreator()));
            BaseObject xObject = document.getXObject(CHANGE_REQUEST_XCLASS, 0, true, context);
            xObject.set("status", "draft", context);
            wiki.saveDocument(document, context);
            for (FileChange fileChange : changeRequest.getFileChanges()) {
                this.fileChangeStorageManager.saveFileChange(fileChange);
            }
        } catch (XWikiException e) {
            e.printStackTrace();
        }
    }
}
