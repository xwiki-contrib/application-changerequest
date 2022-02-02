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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestStatus;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.internal.FileChangeVersionManager;
import org.xwiki.contrib.changerequest.internal.UserReferenceConverter;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.storage.FileChangeStorageManager;
import org.xwiki.localization.LocaleUtils;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.store.merge.MergeDocumentResult;
import org.xwiki.store.merge.MergeManager;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.DocumentRevisionProvider;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiAttachmentContent;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.merge.MergeConfiguration;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.util.Util;

import static org.xwiki.contrib.changerequest.internal.storage.FileChangeXClassInitializer.AUTHOR_PROPERTY;
import static org.xwiki.contrib.changerequest.internal.storage.FileChangeXClassInitializer.CREATION_DATE_PROPERTY;
import static org.xwiki.contrib.changerequest.internal.storage.FileChangeXClassInitializer.FILECHANGE_XCLASS;
import static org.xwiki.contrib.changerequest.internal.storage.FileChangeXClassInitializer.FILENAME_PROPERTY;
import static org.xwiki.contrib.changerequest.internal.storage.FileChangeXClassInitializer.PREVIOUS_PUBLISHED_VERSION_DATE_PROPERTY;
import static org.xwiki.contrib.changerequest.internal.storage.FileChangeXClassInitializer.PREVIOUS_PUBLISHED_VERSION_PROPERTY;
import static org.xwiki.contrib.changerequest.internal.storage.FileChangeXClassInitializer.PREVIOUS_VERSION_PROPERTY;
import static org.xwiki.contrib.changerequest.internal.storage.FileChangeXClassInitializer.REFERENCE_LOCALE_PROPERTY;
import static org.xwiki.contrib.changerequest.internal.storage.FileChangeXClassInitializer.REFERENCE_PROPERTY;
import static org.xwiki.contrib.changerequest.internal.storage.FileChangeXClassInitializer.TYPE_PROPERTY;
import static org.xwiki.contrib.changerequest.internal.storage.FileChangeXClassInitializer.VERSION_PROPERTY;

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
    private static final String ATTACHMENT_EXTENSION = "xml";

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private DocumentReferenceResolver<ChangeRequest> changeRequestDocumentReferenceResolver;

    @Inject
    private DocumentRevisionProvider documentRevisionProvider;

    @Inject
    private UserReferenceResolver<String> stringUserReferenceResolver;

    @Inject
    private UserReferenceConverter userReferenceConverter;

    @Inject
    @Named("uid")
    private EntityReferenceSerializer<String> uidReferenceSerializer;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private FileChangeVersionManager fileChangeVersionManager;

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
        return String.format("%s.%s", id, ATTACHMENT_EXTENSION);
    }

    private String getIdFromFilename(String filename)
    {
        return filename.substring(0, filename.length() - ATTACHMENT_EXTENSION.length() - 1);
    }

    @Override
    public void save(FileChange fileChange) throws ChangeRequestException
    {
        if (!fileChange.isSaved()) {
            XWikiContext context = this.contextProvider.get();
            XWiki wiki = context.getWiki();
            boolean isDeletion = fileChange.getType() == FileChange.FileChangeType.DELETION;
            try {
                if (!isDeletion) {
                    XWikiDocument modifiedDocument = (XWikiDocument) fileChange.getModifiedDocument();
                    modifiedDocument.getAuthors().setOriginalMetadataAuthor(fileChange.getAuthor());
                    modifiedDocument.setContentUpdateDate(fileChange.getCreationDate());
                    modifiedDocument
                        .setRCSVersion(this.fileChangeVersionManager.getDocumentVersion(fileChange.getVersion()));
                }
                String fileChangeId = String.format("%s-%s",
                    fileChange.getVersion(),
                    Util.getHash(this.uidReferenceSerializer.serialize(fileChange.getTargetEntity())));
                fileChange.setId(fileChangeId);

                String filename = this.getFileChangeFileName(fileChangeId);
                XWikiDocument fileChangeDocument = this.getFileChangeStorageDocument(fileChange.getChangeRequest(),
                    fileChange.getTargetEntity());
                this.createFileChangeObject(fileChange, fileChangeDocument);

                fileChangeDocument.setHidden(true);

                if (fileChange.getType() != FileChange.FileChangeType.DELETION) {
                    this.createAttachment(fileChange, fileChangeDocument, filename);
                }
                fileChangeDocument.setContentDirty(true);
                wiki.saveDocument(fileChangeDocument, context);
                fileChange.setSaved(true);
            } catch (XWikiException | IOException e) {
                throw new ChangeRequestException(
                    String.format("Error while storing filechange [%s]", fileChange), e);
            }
        }
    }

    private void createAttachment(FileChange fileChange, XWikiDocument fileChangeDocument, String filename)
        throws IOException, XWikiException
    {
        XWikiContext context = this.contextProvider.get();
        XWiki wiki = context.getWiki();
        XWikiAttachment attachment = new XWikiAttachment(fileChangeDocument, filename);
        attachment.setContentStore(wiki.getDefaultAttachmentContentStore().getHint());
        XWikiAttachmentContent attachmentContent = new XWikiAttachmentContent(attachment);

        OutputStream contentOutputStream = attachmentContent.getContentOutputStream();
        ((XWikiDocument) fileChange.getModifiedDocument())
            .toXML(contentOutputStream, true, true, true, false, context);
        contentOutputStream.close();
        attachment.setAttachment_content(attachmentContent);
        attachment.setMetaDataDirty(true);
        fileChangeDocument.setAttachment(attachment);
    }

    private void createFileChangeObject(FileChange fileChange, XWikiDocument fileChangeDocument) throws XWikiException
    {
        String filename = this.getFileChangeFileName(fileChange.getId());
        XWikiContext context = this.contextProvider.get();
        int xObjectNumber = fileChangeDocument.createXObject(FILECHANGE_XCLASS, context);
        BaseObject fileChangeObject = fileChangeDocument.getXObject(FILECHANGE_XCLASS, xObjectNumber);
        // In case of deletion, the filename is still use as file change ID.
        fileChangeObject.set(FILENAME_PROPERTY, filename, context);
        fileChangeObject.set(VERSION_PROPERTY, fileChange.getVersion(), context);
        fileChangeObject.set(PREVIOUS_VERSION_PROPERTY, fileChange.getPreviousVersion(), context);
        DocumentReference documentReferenceWithLocale = fileChange.getTargetEntity();
        fileChangeObject.set(REFERENCE_PROPERTY, this.entityReferenceSerializer.serialize(documentReferenceWithLocale),
            context);
        fileChangeObject.set(PREVIOUS_PUBLISHED_VERSION_PROPERTY, fileChange.getPreviousPublishedVersion(), context);
        fileChangeObject.set(PREVIOUS_PUBLISHED_VERSION_DATE_PROPERTY,
            fileChange.getPreviousPublishedVersionDate(), context);
        fileChangeObject.set(CREATION_DATE_PROPERTY, fileChange.getCreationDate(), context);
        fileChangeObject.set(AUTHOR_PROPERTY, this.userReferenceConverter.convert(fileChange.getAuthor()), context);
        Locale locale = documentReferenceWithLocale.getLocale();
        if (locale == null) {
            locale = Locale.ROOT;
        }
        fileChangeObject.set(REFERENCE_LOCALE_PROPERTY, locale, context);
        fileChangeObject.set(TYPE_PROPERTY, fileChange.getType().name().toLowerCase(), context);
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
                List<BaseObject> fileChangeObjects = changeRequestDocument.getXObjects(FILECHANGE_XCLASS);
                for (BaseObject fileChangeObject : fileChangeObjects) {
                    FileChange fileChange = this.createFileChangeFromXObject(fileChangeObject, changeRequest);

                    if (fileChange.getType() == FileChange.FileChangeType.DELETION
                        || this.loadDocumentFromAttachment(fileChange, changeRequestDocument)) {
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

    private boolean loadDocumentFromAttachment(FileChange fileChange, XWikiDocument changeRequestDocument)
        throws XWikiException
    {
        boolean result = false;
        String filename = getFileChangeFileName(fileChange.getId());
        XWikiAttachment attachment = changeRequestDocument.getAttachment(filename);
        if (attachment != null) {
            XWikiDocument document = new XWikiDocument(null);
            document.fromXML(attachment.getContentInputStream(contextProvider.get()));
            // The isNew flag is not saved in the XML, so ensure to flag it properly.
            if (fileChange.getType() != FileChange.FileChangeType.CREATION) {
                document.setNew(false);
            }
            fileChange
                .setModifiedDocument(document);
            result = true;
        } else {
            logger.warn("Cannot find attachment for filechange with filename [{}]. "
                + "This filechange will be ignored.", filename);
        }
        return result;
    }

    private FileChange createFileChangeFromXObject(BaseObject fileChangeObject, ChangeRequest changeRequest)
    {
        String typeString = fileChangeObject.getStringValue(TYPE_PROPERTY);
        FileChange.FileChangeType type = FileChange.FileChangeType.valueOf(typeString.toUpperCase());
        FileChange fileChange = new FileChange(changeRequest, type);
        String filename = fileChangeObject.getStringValue(FILENAME_PROPERTY);
        String previousVersion = fileChangeObject.getStringValue(PREVIOUS_VERSION_PROPERTY);
        String previousPublishedVersion =
            fileChangeObject.getStringValue(PREVIOUS_PUBLISHED_VERSION_PROPERTY);
        Date previousPublishedVersionDate = fileChangeObject.getDateValue(PREVIOUS_PUBLISHED_VERSION_DATE_PROPERTY);
        String version = fileChangeObject.getStringValue(VERSION_PROPERTY);
        String authorString = fileChangeObject.getStringValue(AUTHOR_PROPERTY);
        UserReference author = this.stringUserReferenceResolver.resolve(authorString);
        Date creationDate = fileChangeObject.getDateValue(CREATION_DATE_PROPERTY);
        DocumentReference documentReference =
            this.documentReferenceResolver.resolve(fileChangeObject.getStringValue(REFERENCE_PROPERTY));
        String localeString = fileChangeObject.getStringValue(REFERENCE_LOCALE_PROPERTY);
        Locale locale = LocaleUtils.toLocale(localeString);
        documentReference = new DocumentReference(documentReference, locale);

        return fileChange
            .setId(this.getIdFromFilename(filename))
            .setTargetEntity(documentReference)
            .setPreviousVersion(previousVersion)
            .setPreviousPublishedVersion(previousPublishedVersion, previousPublishedVersionDate)
            .setVersion(version)
            .setCreationDate(creationDate)
            .setAuthor(author)
            .setSaved(true);
    }

    @Override
    public void merge(FileChange fileChange) throws ChangeRequestException
    {
        switch (fileChange.getType())
        {
            case EDITION:
                this.mergeEdition(fileChange);
                break;

            case DELETION:
                this.mergeDeletion(fileChange);
                break;

            case CREATION:
                this.mergeCreation(fileChange);
                break;

            default:
                throw new ChangeRequestException("Unknown file change type: " + fileChange.getType());
        }
    }

    @Override
    public void rebase(FileChange fileChange) throws ChangeRequestException
    {
        FileChange clone;
        XWikiContext context = this.contextProvider.get();
        try {
            XWikiDocument document = context.getWiki().getDocument(fileChange.getTargetEntity(), context);
            if (fileChange.getType() == FileChange.FileChangeType.CREATION && !document.isNew()) {
                clone = fileChange.cloneWithType(FileChange.FileChangeType.EDITION);
            } else {
                clone = fileChange.clone();
            }
            if (!document.isNew()) {
                clone.setPreviousPublishedVersion(document.getVersion(), document.getDate());
            }
            clone.setPreviousVersion(fileChange.getVersion())
                .setVersion(this.fileChangeVersionManager.getNextFileChangeVersion(fileChange.getVersion(), false));
            this.save(clone);
        } catch (XWikiException e) {
            throw new ChangeRequestException(
                String.format("Error while trying to access published document from [%s] to get its version: [%s]",
                    fileChange.getTargetEntity(),
                    ExceptionUtils.getRootCauseMessage(e)));
        }
    }

    private String getMergeSaveMessage()
    {
        // FIXME: Merge message should be translatable
        return "Merge changes from **change request**";
    }

    private void mergeCreation(FileChange fileChange) throws ChangeRequestException
    {
        XWikiContext context = contextProvider.get();
        XWiki wiki = context.getWiki();
        XWikiDocument modifiedDoc = (XWikiDocument) fileChange.getModifiedDocument();
        try {
            wiki.saveDocument(modifiedDoc, getMergeSaveMessage(), context);
        } catch (XWikiException e) {
            throw new ChangeRequestException(
                String.format("Error while saving the new document [%s]", fileChange), e);
        }
    }

    private void mergeEdition(FileChange fileChange) throws ChangeRequestException
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
                throw new ChangeRequestException(
                    String.format("Cannot merge the file change [%s] since it has conflicts.", fileChange));
            }
            if (mergeDocumentResult.isModified()) {
                XWikiDocument document = (XWikiDocument) mergeDocumentResult.getMergeResult();
                // FIXME: When merging the context user should be set with a ghost user depending on the config.
                wiki.saveDocument(document, getMergeSaveMessage(), context);
            }
        } catch (XWikiException e) {
            throw new ChangeRequestException(
                String.format("Error while merging the file change [%s]", fileChange), e);
        }
    }

    private void mergeDeletion(FileChange fileChange) throws ChangeRequestException
    {
        DocumentReference targetEntity = fileChange.getTargetEntity();
        XWikiContext context = this.contextProvider.get();
        XWiki wiki = context.getWiki();
        try {
            XWikiDocument document = wiki.getDocument(targetEntity, context);
            wiki.deleteDocument(document, context);
        } catch (XWikiException e) {
            throw new ChangeRequestException(String.format("Error while deleting [%s]", targetEntity), e);
        }
    }

    private XWikiDocument getDocumentFromFileChange(FileChange fileChange, DocumentVersion version)
        throws ChangeRequestException
    {
        XWikiContext context = contextProvider.get();
        XWikiDocument result;
        try {
            switch (version) {
                case OLD:
                    // TODO: we'll need a fallback if the source version has been deleted for some reason.
                    result = this.documentRevisionProvider.getRevision(fileChange.getTargetEntity(),
                        fileChange.getPreviousPublishedVersion());
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
        XWikiDocument result = getDocumentFromFileChange(fileChange, DocumentVersion.OLD);
        ChangeRequestStatus status = fileChange.getChangeRequest().getStatus();

        // if the CR is closed or merged, we don't really care if the previous version is not exactly the same.
        if (status != ChangeRequestStatus.MERGED
            && status != ChangeRequestStatus.CLOSED
            && !result.getDate().equals(fileChange.getPreviousPublishedVersionDate())) {
            throw new ChangeRequestException("The previous version of the document has been removed, "
                + "comparison is not possible.");
        } else {
            return result;
        }
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
