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
package org.xwiki.contrib.changerequest.internal.handlers;

import java.io.IOException;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.ChangeRequestManager;
import org.xwiki.contrib.changerequest.ChangeRequestReference;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.store.merge.MergeDocumentResult;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.EditForm;

/**
 * Handler for adding changes to an existing change request.
 *
 * @version $Id$
 * @since 0.3
 */
@Component
@Named("addchanges")
@Singleton
public class AddChangesChangeRequestHandler extends AbstractChangeRequestActionHandler
{
    @Inject
    private ChangeRequestManager changeRequestManager;

    @Inject
    private UserReferenceResolver<CurrentUserReference> userReferenceResolver;

    @Inject
    private Logger logger;

    @Override
    public void handle(ChangeRequestReference changeRequestReference) throws ChangeRequestException, IOException
    {
        HttpServletRequest request = this.prepareRequest();
        EditForm editForm = this.prepareForm(request);
        XWikiDocument modifiedDocument = this.prepareDocument(request, editForm);
        DocumentReference documentReference = modifiedDocument.getDocumentReferenceWithLocale();
        ChangeRequest changeRequest = this.loadChangeRequest(changeRequestReference);

        if (changeRequest != null) {
            String previousVersion = request.getParameter("previousVersion");

            UserReference currentUser = this.userReferenceResolver.resolve(CurrentUserReference.INSTANCE);
            FileChange fileChange = new FileChange(changeRequest);
            fileChange
                .setAuthor(currentUser)
                .setTargetEntity(documentReference)
                // FIXME: the version here is not necessarily the right one.
                .setSourceVersion(previousVersion)
                .setModifiedDocument(modifiedDocument);

            Optional<MergeDocumentResult> optionalMergeDocumentResult =
                this.changeRequestManager.mergeDocumentChanges(modifiedDocument, previousVersion, changeRequest);
            boolean withConflict = false;
            if (optionalMergeDocumentResult.isPresent()) {
                MergeDocumentResult mergeDocumentResult = optionalMergeDocumentResult.get();
                withConflict = mergeDocumentResult.hasConflicts();
                if (withConflict) {
                    // TODO: handle conflict answer
                    logger.error("Conflict found.");
                } else {
                    fileChange.setModifiedDocument(mergeDocumentResult.getMergeResult());
                }
            }
            if (!withConflict) {
                changeRequest.addFileChange(fileChange);
                this.storageManager.save(changeRequest);
                this.redirectToChangeRequest(changeRequest);
            }
        }
    }
}
