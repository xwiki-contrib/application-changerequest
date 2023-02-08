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
package org.xwiki.contrib.changerequest.internal.diff;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.contrib.changerequest.ChangeRequestDiffManager;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.internal.cache.DiffCacheManager;
import org.xwiki.contrib.changerequest.storage.FileChangeStorageManager;
import org.xwiki.diff.DiffException;
import org.xwiki.diff.xml.XMLDiffConfiguration;
import org.xwiki.diff.xml.XMLDiffManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.store.TemporaryAttachmentException;
import org.xwiki.store.TemporaryAttachmentSessionsManager;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 *  Abstract implementation of {@link ChangeRequestDiffManager}.
 *  This class has been made abstract in order to allow providing different implementations of
 *  {@link #getRenderedContent(XWikiDocument, FileChange)} for more or less secure rendering.
 *
 * @version $Id$
 * @since 1.3
 */
public abstract class AbstractChangeRequestDiffManager implements ChangeRequestDiffManager
{
    @Inject
    private FileChangeStorageManager fileChangeStorageManager;

    @Inject
    @Named("html/unified")
    private XMLDiffManager xmlDiffManager;

    @Inject
    @Named("html")
    private Provider<XMLDiffConfiguration> xmlDiffConfigurationProvider;

    @Inject
    private Provider<TemporaryAttachmentSessionsManager> temporaryAttachmentSessionsManagerProvider;

    @Inject
    private DiffCacheManager diffCacheManager;

    @Inject
    private Logger logger;

    @Override
    public String getHtmlDiff(FileChange fileChange) throws ChangeRequestException
    {
        String result = "";
        Optional<String> renderedDiff = this.diffCacheManager.getRenderedDiff(fileChange);
        if (renderedDiff.isPresent()) {
            result = renderedDiff.get();
        } else {
            XWikiDocument modifiedDoc;
            Optional<DocumentModelBridge> previousDocumentFromFileChange;
            XWikiDocument previousDoc;
            switch (fileChange.getType()) {
                case EDITION:
                    modifiedDoc =
                        (XWikiDocument) this.fileChangeStorageManager.getModifiedDocumentFromFileChange(fileChange);
                    previousDocumentFromFileChange =
                        this.fileChangeStorageManager.getPreviousDocumentFromFileChange(fileChange);
                    if (previousDocumentFromFileChange.isEmpty()) {
                        result = null;
                    } else {
                        previousDoc = (XWikiDocument) previousDocumentFromFileChange.get();
                        this.handleAttachments(modifiedDoc);
                        result = this.getHtmlDiff(previousDoc, modifiedDoc, fileChange);
                        this.temporaryAttachmentSessionsManagerProvider.get()
                            .removeUploadedAttachments(modifiedDoc.getDocumentReference());
                    }
                    break;

                case CREATION:
                    modifiedDoc =
                        (XWikiDocument) this.fileChangeStorageManager.getModifiedDocumentFromFileChange(fileChange);
                    result = this.getHtmlDiff(null, modifiedDoc, fileChange);
                    break;

                case DELETION:
                    previousDocumentFromFileChange =
                        this.fileChangeStorageManager.getPreviousDocumentFromFileChange(fileChange);
                    if (previousDocumentFromFileChange.isEmpty()) {
                        result = null;
                    } else {
                        previousDoc = (XWikiDocument) previousDocumentFromFileChange.get();
                        result = this.getHtmlDiff(previousDoc, null, fileChange);
                    }
                    break;

                case NO_CHANGE:
                default:
                    result = "";
                    break;
            }
            if (result != null) {
                this.diffCacheManager.setRenderedDiff(fileChange, result);
            }
        }
        return result;
    }

    private void handleAttachments(XWikiDocument modifiedDoc)
    {
        TemporaryAttachmentSessionsManager temporaryAttachmentSessionsManager =
            this.temporaryAttachmentSessionsManagerProvider.get();
        DocumentReference reference = modifiedDoc.getDocumentReference();
        for (XWikiAttachment attachment : modifiedDoc.getAttachmentList()) {
            XWikiAttachment clonedAttachment = attachment.clone();
            // Ensure to not delete the file related to the attachment when it's removed from temporary attachments
            clonedAttachment.getAttachment_content().setContentDirty(false);
            try {
                temporaryAttachmentSessionsManager.temporarilyAttach(clonedAttachment, reference);
            } catch (TemporaryAttachmentException e) {
                this.logger.error("Error while temporary attaching attachment [{}] to document [{}]: [{}]",
                    clonedAttachment.getFilename(),
                    reference,
                    ExceptionUtils.getRootCauseMessage(e));
            }
        }
    }

    private String getHtmlDiff(XWikiDocument previousDoc, XWikiDocument nextDoc, FileChange fileChange)
        throws ChangeRequestException
    {
        try {
            // Note that it's important here to keep on the same line the calls of both rendering content:
            // in case of stacktraces because of missing script rights we don't want to have different line numbers for
            // previousDoc and for nextDoc as it would produce an insertion in the diff.
            return this.xmlDiffManager.diff(getRenderedContent(previousDoc, fileChange),
                getRenderedContent(nextDoc, fileChange),
                this.xmlDiffConfigurationProvider.get());
        } catch (XWikiException e) {
            throw new ChangeRequestException("Error while computing the rendered content for diff.", e);
        } catch (DiffException e) {
            throw new ChangeRequestException("Error while computing the diff", e);
        }
    }

    /**
     * Render the content for the given document: various implementation might be provided depending if the content
     * should be rendered with restricted flag or not.
     *
     * @param document the document to be rendered
     * @param fileChange the filechange which triggers this rendering as it might provide information
     * @return the string corresponding to the rendered content
     * @throws XWikiException in case of problem during the rendering
     */
    protected abstract String getRenderedContent(XWikiDocument document, FileChange fileChange) throws XWikiException;
}
