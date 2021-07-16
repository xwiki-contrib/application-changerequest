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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
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
import org.xwiki.store.merge.MergeDocumentResult;
import org.xwiki.store.merge.MergeManager;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.DocumentRevisionProvider;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiAttachmentContent;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.merge.MergeConfiguration;

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
    static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyMMddHHmmssZ");

    static final String FILE_CHANGE_CONSTANT_NAME = "filechange";

    private static final Pattern FILE_CHANGE_NAME_PATTERN =
        Pattern.compile(String.format("^%s-(?<id>.+-.+)\\.xml$", FILE_CHANGE_CONSTANT_NAME));

    private static final String ATTACHMENT_EXTENSION = "xml";

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
    @Named("uid")
    private EntityReferenceSerializer<String> uidReferenceSerializer;

    @Inject
    private MergeManager mergeManager;

    @Inject
    private Logger logger;

    private enum DocumentVersion
    {
        OLD,
        CURRENT,
        FILECHANGE
    }

    private XWikiDocument getFileChangeStorageDocument(ChangeRequest changeRequest, DocumentReference changedDocument)
        throws XWikiException
    {
        XWikiContext context = this.contextProvider.get();
        DocumentReference changeRequestDocReference =
            this.changeRequestDocumentReferenceResolver.resolve(changeRequest);
        DocumentReference fileChangeStorageReference =
            new DocumentReference(this.uidReferenceSerializer.serialize(changedDocument),
                changeRequestDocReference.getLastSpaceReference());
        return context.getWiki().getDocument(fileChangeStorageReference, context);
    }

    private String getFileChangeFileName(String id)
    {
        return String.format("%s-%s.%s", FILE_CHANGE_CONSTANT_NAME, id, ATTACHMENT_EXTENSION);
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
                modifiedDocument.setContentUpdateDate(fileChange.getCreationDate());
                String fileChangeId = String.format("%s-%s",
                    modifiedDocument.getId(),
                    modifiedDocument.getVersion());
                fileChange.setId(fileChangeId);

                String filename = this.getFileChangeFileName(fileChangeId);
                XWikiDocument fileChangeDocument = this.getFileChangeStorageDocument(fileChange.getChangeRequest(),
                    modifiedDocument.getDocumentReferenceWithLocale());
                fileChangeDocument.setHidden(true);
                XWikiAttachment attachment = new XWikiAttachment(fileChangeDocument, filename);
                attachment.setContentStore(wiki.getDefaultAttachmentContentStore().getHint());
                XWikiAttachmentContent attachmentContent = new XWikiAttachmentContent(attachment);

                OutputStream contentOutputStream = attachmentContent.getContentOutputStream();
                modifiedDocument.toXML(contentOutputStream, true, true, true, false, context);
                contentOutputStream.close();
                attachment.setAttachment_content(attachmentContent);
                attachment.setMetaDataDirty(true);
                fileChangeDocument.setAttachment(attachment);
                fileChangeDocument.setContentDirty(true);
                wiki.saveDocument(fileChangeDocument, context);
                fileChange.setSaved(true);
            } catch (XWikiException | IOException e) {
                throw new ChangeRequestException(
                    String.format("Error while storing filechange [%s]", fileChange));
            }
        }
    }

    @Override
    public List<FileChange> load(ChangeRequest changeRequest, DocumentReference changedDocument)
        throws ChangeRequestException
    {
        List<FileChange> result = new ArrayList<>();
        try {
            DocumentReference changedDocumentWithLocale = changedDocument;
            if (changedDocumentWithLocale.getLocale() == null) {
                changedDocumentWithLocale = new DocumentReference(changedDocument, Locale.ROOT);
            }
            XWikiDocument changeRequestDocument = this.getFileChangeStorageDocument(changeRequest, 
                changedDocumentWithLocale);
            if (changeRequestDocument.isNew()) {
                logger.warn("No file change found in [{}].", changeRequestDocument.getDocumentReference());
            } else {
                // TODO: check the order for attachments, we'd need to get the file storage sorted by date / version
                List<XWikiAttachment> attachmentList = changeRequestDocument.getAttachmentList();
                for (XWikiAttachment attachment : attachmentList) {
                    Matcher matcher = FILE_CHANGE_NAME_PATTERN.matcher(attachment.getFilename());
                    if (matcher.matches()) {
                        XWikiDocument document = new XWikiDocument(null);
                        document.fromXML(attachment.getContentInputStream(contextProvider.get()));
                        FileChange fileChange = new FileChange(changeRequest);
                        fileChange
                            .setId(matcher.group("id"))
                            .setTargetEntity(document.getDocumentReferenceWithLocale())
                            .setModifiedDocument(document)
                            .setSourceVersion(document.getVersion())
                            .setAuthor(this.userReferenceResolver.resolve(document.getContentAuthorReference()))
                            .setCreationDate(document.getContentUpdateDate())
                            .setSaved(true);
                        result.add(fileChange);
                    }
                }
            }
        } catch (XWikiException e) {
            throw new ChangeRequestException(
                String.format("Error while loading file changes for change request [%s] and reference [%s]",
                    changeRequest, changedDocument), e);
        }
        return result;
    }

    @Override
    public void merge(FileChange fileChange) throws ChangeRequestException
    {
        XWikiContext context = contextProvider.get();
        XWiki wiki = context.getWiki();
        try {
            DocumentModelBridge modifiedDoc =
                this.getModifiedDocumentFromFileChange(fileChange);
            DocumentModelBridge previousDoc =
                this.getPreviousDocumentFromFileChange(fileChange);
            DocumentModelBridge originalDoc =
                this.getCurrentDocumentFromFileChange(fileChange);
            MergeConfiguration mergeConfiguration = new MergeConfiguration();

            // We need the reference of the user and the document in the config to retrieve
            // the conflict decision in the MergeManager.
            mergeConfiguration.setUserReference(context.getUserReference());
            mergeConfiguration.setConcernedDocument(modifiedDoc.getDocumentReference());

            // The modified doc is actually the one we should save
            mergeConfiguration.setProvidedVersionsModifiables(true);

            MergeDocumentResult mergeDocumentResult =
                mergeManager.mergeDocument(previousDoc, modifiedDoc, originalDoc, mergeConfiguration);

            if (mergeDocumentResult.hasConflicts()) {
                throw new ChangeRequestException("Cannot merge the file change since it has conflicts.");
            }
            if (mergeDocumentResult.isModified()) {
                XWikiDocument document = (XWikiDocument) mergeDocumentResult.getMergeResult();
                document.setContentAuthorReference(this.userReferenceConverter.convert(fileChange.getAuthor()));
                String saveMessage = "Save after change request merge";
                wiki.saveDocument(document, saveMessage, context);
            }
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
            switch (version) {
                case OLD:
                    // TODO: we'll need a fallback if the source version has been deleted for some reason.
                    result = this.documentRevisionProvider.getRevision(fileChange.getTargetEntity(),
                        fileChange.getSourceVersion());
                    break;

                case FILECHANGE:
                    result = (XWikiDocument) fileChange.getModifiedDocument();
                    // we ensure to update the RCS version to not compare with the same version as previous version.
                    result.setRCSVersion(result.getRCSVersion().next());
                    break;

                case CURRENT:
                default:
                    result = context.getWiki().getDocument(fileChange.getTargetEntity(), context);
            }

            return result;
        } catch (XWikiException e) {
            throw new ChangeRequestException(
                String.format("Error while loading the document corresponding to the file change [%s]", fileChange), e);
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

    @Override
    public DocumentModelBridge getDocumentFromFileChange(FileChange fileChange, String version)
        throws ChangeRequestException
    {
        try {
            return this.documentRevisionProvider.getRevision(fileChange.getTargetEntity(), version);
        } catch (XWikiException e) {
            throw new ChangeRequestException(String.format(
                "Error while loading the document corresponding to the file change [%s] with version [%s]",
                fileChange, version), e);
        }
    }
}
