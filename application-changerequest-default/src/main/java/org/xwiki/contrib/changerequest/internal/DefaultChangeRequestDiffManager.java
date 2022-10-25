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

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ChangeRequestDiffManager;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.internal.cache.DiffCacheManager;
import org.xwiki.contrib.changerequest.storage.FileChangeStorageManager;
import org.xwiki.diff.DiffException;
import org.xwiki.diff.xml.XMLDiffConfiguration;
import org.xwiki.diff.xml.XMLDiffManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.store.TemporaryAttachmentException;
import org.xwiki.store.TemporaryAttachmentSessionsManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Default implementation of {@link ChangeRequestDiffManager}.
 *
 * @version $Id$
 * @since 1.3
 */
@Component
@Singleton
public class DefaultChangeRequestDiffManager implements ChangeRequestDiffManager
{
    @Inject
    private FileChangeStorageManager fileChangeStorageManager;

    @Inject
    private Provider<XWikiContext> contextProvider;

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
                        result = this.getHtmlDiff(previousDoc, modifiedDoc);
                        this.temporaryAttachmentSessionsManagerProvider.get()
                            .removeUploadedAttachments(modifiedDoc.getDocumentReference());
                    }
                    break;

                case CREATION:
                    modifiedDoc =
                        (XWikiDocument) this.fileChangeStorageManager.getModifiedDocumentFromFileChange(fileChange);
                    result = this.getHtmlDiff(null, modifiedDoc);
                    break;

                case DELETION:
                    previousDocumentFromFileChange =
                        this.fileChangeStorageManager.getPreviousDocumentFromFileChange(fileChange);
                    if (previousDocumentFromFileChange.isEmpty()) {
                        result = null;
                    } else {
                        previousDoc = (XWikiDocument) previousDocumentFromFileChange.get();
                        result = this.getHtmlDiff(previousDoc, null);
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

    private String getRenderedContent(XWikiDocument document) throws XWikiException
    {
        XWikiContext context = contextProvider.get();
        XWikiDocument currentDoc = context.getDoc();
        String result = "";
        if (document != null) {
            context.setDoc(document);
            try {
                // Note that we render the content in restricted mode to avoid any security issue:
                // we cannot guarantee here that the provided changes are safe
                result = document.displayDocument(Syntax.HTML_5_0, true, contextProvider.get());
            } finally {
                context.setDoc(currentDoc);
            }
        }
        return result;
    }

    private String getHtmlDiff(XWikiDocument previousDoc, XWikiDocument nextDoc) throws ChangeRequestException
    {
        try {
            // Note that it's important here to keep on the same line the calls of both rendering content:
            // in case of stacktraces because of missing script rights we don't want to have different line numbers for
            // previousDoc and for nextDoc as it would produce an insertion in the diff.
            return this.xmlDiffManager.diff(getRenderedContent(previousDoc), getRenderedContent(nextDoc),
                this.xmlDiffConfigurationProvider.get());
        } catch (XWikiException e) {
            throw new ChangeRequestException("Error while computing the rendered content for diff.", e);
        } catch (DiffException e) {
            throw new ChangeRequestException("Error while computing the diff", e);
        }
    }
}
