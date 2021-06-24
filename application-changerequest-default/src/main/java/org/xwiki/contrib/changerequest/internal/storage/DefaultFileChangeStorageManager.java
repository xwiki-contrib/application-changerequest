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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.internal.UserReferenceConverter;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.storage.FileChangeStorageManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.user.UserReferenceSerializer;

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
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyMMddHHmmssZ");

    private static final String FILE_CHANGE_CONSTANT_NAME = "filechange";

    private static final Pattern FILE_CHANGE_NAME_PATTERN =
        Pattern.compile(String.format("^%s-.+-.+-[0-9]{12}[\\+\\-][0-9]{4}$", FILE_CHANGE_CONSTANT_NAME));

    private static final String ATTACHMENT_EXTENSION = ".xml";

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private UserReferenceConverter converter;

    @Inject
    private DocumentReferenceResolver<ChangeRequest> changeRequestDocumentReferenceResolver;

    @Inject
    private DocumentRevisionProvider documentRevisionProvider;

    @Inject
    @Named("document")
    private UserReferenceResolver<DocumentReference> userReferenceResolver;

    @Inject
    private UserReferenceConverter userReferenceConverter;

    @Inject
    private UserReferenceSerializer<String> userReferenceSerializer;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

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
    public void save(FileChange fileChange) throws ChangeRequestException
    {
        if (!fileChange.isSaved()) {
            XWikiContext context = this.contextProvider.get();
            XWiki wiki = context.getWiki();
            try {
                XWikiDocument modifiedDocument = (XWikiDocument) fileChange.getModifiedDocument();
                modifiedDocument.setContentAuthorReference(this.converter.convert(fileChange.getAuthor()));

                String fileChangeId = String.format("%s-%s-%s-%s",
                    FILE_CHANGE_CONSTANT_NAME,
                    this.entityReferenceSerializer.serialize(fileChange.getTargetEntity()),
                    this.userReferenceSerializer.serialize(fileChange.getAuthor()),
                    DATE_FORMAT.format(new Date()));
                fileChange.setId(fileChangeId);
                String filename = fileChange.getId() + ATTACHMENT_EXTENSION;
                XWikiDocument changeRequestDocument = this.getChangeRequestDocument(fileChange.getChangeRequest());
                XWikiAttachment attachment = new XWikiAttachment(changeRequestDocument, filename);
                attachment.setContentStore(wiki.getDefaultAttachmentContentStore().getHint());
                XWikiAttachmentContent attachmentContent = new XWikiAttachmentContent(attachment);

                OutputStream contentOutputStream = attachmentContent.getContentOutputStream();
                modifiedDocument.toXML(contentOutputStream, true, true, true, false, context);
                contentOutputStream.close();
                attachment.setAttachment_content(attachmentContent);
                attachment.setMetaDataDirty(true);
                changeRequestDocument.setAttachment(attachment);
                changeRequestDocument.setContentDirty(true);
                wiki.saveDocument(changeRequestDocument, context);
                fileChange.setSaved(true);
            } catch (XWikiException | IOException e) {
                throw new ChangeRequestException(
                    String.format("Error while storing filechange [%s]", fileChange));
            }
        }
    }

    @Override
    public Optional<FileChange> load(ChangeRequest changeRequest, String fileChangeId)
        throws ChangeRequestException
    {
        Optional<FileChange> result = Optional.empty();
        if (FILE_CHANGE_NAME_PATTERN.matcher(fileChangeId).matches()) {
            try {
                XWikiDocument changeRequestDocument = this.getChangeRequestDocument(changeRequest);
                String filename = fileChangeId + ATTACHMENT_EXTENSION;
                XWikiAttachment attachment = changeRequestDocument.getAttachment(filename);
                if (attachment != null) {
                    FileChange fileChange = new FileChange(changeRequest);
                    XWikiDocument document = new XWikiDocument(null);
                    document.fromXML(attachment.getContentInputStream(contextProvider.get()));
                    fileChange
                        .setId(fileChangeId)
                        .setTargetEntity(document.getDocumentReferenceWithLocale())
                        .setModifiedDocument(document)
                        .setSourceVersion(document.getVersion())
                        .setAuthor(this.userReferenceResolver.resolve(document.getContentAuthorReference()))
                        .setCreationDate(document.getContentUpdateDate())
                        .setSaved(true);
                    result = Optional.of(fileChange);
                }
            } catch (XWikiException e) {
                throw new ChangeRequestException(
                    String.format("Error while loading file change with id [%s]", fileChangeId), e);
            }
        }
        return result;
    }

    @Override
    public void merge(FileChange fileChange) throws ChangeRequestException
    {
        XWikiContext context = contextProvider.get();
        XWiki wiki = context.getWiki();
        try {
            XWikiDocument document = wiki.getDocument(fileChange.getTargetEntity(), context);
            XWikiDocument modifiedDocument = (XWikiDocument) fileChange.getModifiedDocument();
            // FIXME: we should also set the xclass/xobjects etc
            document.setContent(modifiedDocument.getContent());
            document.setContentAuthorReference(this.userReferenceConverter.convert(fileChange.getAuthor()));
            String saveMessage = "Save after change request merge";
            wiki.saveDocument(document, saveMessage, context);
        } catch (XWikiException e) {
            throw new ChangeRequestException(
                String.format("Error while merging the file change [%s]", fileChange), e);
        }
    }

    private DocumentModelBridge getDocumentFromFileChange(FileChange fileChange, DocumentVersion version)
        throws ChangeRequestException
    {
        XWikiContext context = contextProvider.get();
        XWikiDocument result;
        try {
            XWikiDocument currentDocument = context.getWiki().getDocument(fileChange.getTargetEntity(), context);
            switch (version) {
                case OLD:
                    // TODO: we'll need a fallback if the source version has been deleted for some reason.
                    result = this.documentRevisionProvider.getRevision(fileChange.getTargetEntity(),
                        fileChange.getSourceVersion());
                    break;

                case FILECHANGE:
                    result = (XWikiDocument) fileChange.getModifiedDocument();
                    break;

                case CURRENT:
                default:
                    result = currentDocument;
            }

            return result;
        } catch (XWikiException e) {
            throw new ChangeRequestException(
                String.format("Error while loading the document corresponding to the file change [%s]", fileChange));
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
