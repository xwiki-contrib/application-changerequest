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
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestManager;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.storage.ChangeRequestStorageManager;
import org.xwiki.contrib.changerequest.storage.FileChangeStorageManager;
import org.xwiki.store.merge.MergeDocumentResult;
import org.xwiki.store.merge.MergeManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.merge.MergeConfiguration;

/**
 * Default implementation of {@link ChangeRequestManager}.
 *
 * @version $Id$
 * @since 0.1-SNAPSHOT
 */
@Component
@Singleton
public class DefaultChangeRequestManager implements ChangeRequestManager
{
    @Inject
    private ChangeRequestStorageManager changeRequestStorageManager;

    @Inject
    private FileChangeStorageManager fileChangeStorageManager;

    @Inject
    private MergeManager mergeManager;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Override
    public Optional<ChangeRequest> getChangeRequest(String id)
    {
        return Optional.empty();
    }

    @Override
    public boolean hasConflicts(FileChange fileChange) throws ChangeRequestException
    {
        XWikiContext context = this.contextProvider.get();
        DocumentModelBridge modifiedDoc =
            this.fileChangeStorageManager.getModifiedDocumentFromFileChange(fileChange);
        DocumentModelBridge previousDoc =
            this.fileChangeStorageManager.getPreviousDocumentFromFileChange(fileChange);
        DocumentModelBridge originalDoc =
            this.fileChangeStorageManager.getCurrentDocumentFromFileChange(fileChange);

        MergeConfiguration mergeConfiguration = new MergeConfiguration();

        // We need the reference of the user and the document in the config to retrieve
        // the conflict decision in the MergeManager.
        mergeConfiguration.setUserReference(context.getUserReference());
        mergeConfiguration.setConcernedDocument(modifiedDoc.getDocumentReference());

        // The modified doc is actually the one we should save, so it's ok to modify it directly
        // and better for performance.
        mergeConfiguration.setProvidedVersionsModifiables(true);

        MergeDocumentResult mergeDocumentResult =
            mergeManager.mergeDocument(previousDoc, originalDoc, modifiedDoc, mergeConfiguration);
        return mergeDocumentResult.hasConflicts();
    }
}
