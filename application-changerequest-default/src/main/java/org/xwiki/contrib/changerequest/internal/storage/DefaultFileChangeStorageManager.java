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

import java.io.IOException;
import java.io.OutputStream;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.suigeneris.jrcs.rcs.Version;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.internal.UserReferenceConverter;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.storage.FileChangeStorageManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.DocumentRevisionProvider;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiAttachmentContent;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Default implementation of {@link FileChangeStorageManager}.
 * The file changes are located in attachments that are attached to the related {@link ChangeRequest}.
 *
 * @version $Id$
 * @since 0.1
 */
@Component
@Singleton
public class DefaultFileChangeStorageManager implements FileChangeStorageManager
{
    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private UserReferenceConverter converter;

    @Inject
    private DocumentReferenceResolver<ChangeRequest> changeRequestDocumentReferenceResolver;

    @Inject
    private DocumentRevisionProvider documentRevisionProvider;

    private enum DocumentVersion
    {
        OLD,
        CURRENT,
        FILECHANGE
    }

    private XWikiDocument getChangeRequestDocument(ChangeRequest changeRequest) throws XWikiException
    {
        XWikiContext context = this.contextProvider.get();
        DocumentReference documentReference = this.changeRequestDocumentReferenceResolver.resolve(changeRequest);
        return context.getWiki().getDocument(documentReference, context);
    }

    @Override
    public void saveFileChange(FileChange fileChange) throws ChangeRequestException
    {
        XWikiContext context = this.contextProvider.get();
        XWiki wiki = context.getWiki();
        try {
            XWikiDocument document = wiki.getDocument(fileChange.getTargetEntity(), context);
            // We need to clone the document, else the change we perform are kept in cache and we don't want that.
            document = document.clone();
            document.setContent(fileChange.getContentChange());
            Version version = new Version(fileChange.getSourceVersion());
            Version next = version.next();
            document.setVersion(next.toString());
            document.setContentAuthorReference(this.converter.convert(fileChange.getAuthor()));

            String filename = fileChange.getId() + ".xml";
            XWikiDocument changeRequestDocument = this.getChangeRequestDocument(fileChange.getChangeRequest());
            XWikiAttachment attachment = new XWikiAttachment(changeRequestDocument, filename);
            attachment.setContentStore(wiki.getDefaultAttachmentContentStore().getHint());
            XWikiAttachmentContent attachmentContent = new XWikiAttachmentContent(attachment);

            OutputStream contentOutputStream = attachmentContent.getContentOutputStream();
            document.toXML(contentOutputStream, true, true, true, false, context);
            contentOutputStream.close();
            attachment.setAttachment_content(attachmentContent);
            attachment.setMetaDataDirty(true);
            changeRequestDocument.setAttachment(attachment);
            changeRequestDocument.setContentDirty(true);
            wiki.saveDocument(changeRequestDocument, context);
        } catch (XWikiException | IOException e) {
            throw new ChangeRequestException(
                String.format("Error while storing filechange [%s]", fileChange));
        }
    }

    private DocumentModelBridge getDocumentFromFileChange(FileChange fileChange, DocumentVersion version)
        throws ChangeRequestException
    {
        XWikiDocument result;
        try {
            if (version == DocumentVersion.CURRENT) {
                XWikiContext context = contextProvider.get();
                result = context.getWiki().getDocument(fileChange.getTargetEntity(), context);
            } else {
                result = this.documentRevisionProvider.getRevision(fileChange.getTargetEntity(),
                    fileChange.getSourceVersion());
            }
            if (version == DocumentVersion.FILECHANGE) {
                result.setVersion("merged");
                result.setContent(fileChange.getContentChange());
            }
            return result;
        } catch (XWikiException e) {
            throw new ChangeRequestException(
                String.format("Error while loading the document corresponding to the file change [{}]", fileChange));
        }
    }

    @Override
    public DocumentModelBridge getModifiedDocumentFromFileChange(FileChange fileChange)
        throws ChangeRequestException
    {
        return getDocumentFromFileChange(fileChange, DocumentVersion.FILECHANGE);
    }

    @Override
    public DocumentModelBridge getCurrentDocumentFromFileChange(FileChange fileChange)
        throws ChangeRequestException
    {
        return getDocumentFromFileChange(fileChange, DocumentVersion.CURRENT);
    }

    @Override
    public DocumentModelBridge getPreviousDocumentFromFileChange(FileChange fileChange)
        throws ChangeRequestException
    {
        return getDocumentFromFileChange(fileChange, DocumentVersion.OLD);
    }
}
