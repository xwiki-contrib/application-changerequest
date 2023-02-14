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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.LRUCacheConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.storage.ChangeRequestStorageManager;
import org.xwiki.contrib.changerequest.storage.FileChangeStorageManager;
import org.xwiki.text.StringUtils;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Cache dedicated to store modified documents title to be displayed in notifications.
 *
 * @version $Id$
 * @since 1.4.5
 */
@Component(roles = ChangeRequestTitleCacheManager.class)
@Singleton
public class ChangeRequestTitleCacheManager implements Initializable, Disposable
{
    @Inject
    private CacheManager cacheManager;

    @Inject
    private Provider<ChangeRequestStorageManager> changeRequestStorageManagerProvider;

    @Inject
    private Logger logger;

    @Inject
    private Provider<FileChangeStorageManager> fileChangeStorageManagerProvider;

    @Inject
    private Provider<XWikiContext> contextProvider;

    private Cache<Map<String, String>> titleCache;

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        this.titleCache.dispose();
    }

    @Override
    public void initialize() throws InitializationException
    {
        try {
            this.titleCache =
                this.cacheManager.createNewCache(new LRUCacheConfiguration("changerequest.titles", 1000));
        } catch (CacheException e) {
            throw new InitializationException("Error while creating cache", e);
        }
    }

    /**
     * Compute and return the title for the given change request and file change.
     * If it's not already in cache, this method will put the computed title in cache.
     *
     * @param changeRequestId the identifier of the change request for which to retrieve a document title
     * @param fileChangeId the identifier of the file change for which to retrieve a document title
     * @return a title or {@code null} if there was a problem to compute it
     */
    public String getTitle(String changeRequestId, String fileChangeId)
    {
        String result;
        Map<String, String> mapTitle = this.titleCache.get(changeRequestId);
        if (mapTitle == null) {
            mapTitle = new HashMap<>();
            this.titleCache.set(changeRequestId, mapTitle);
        }
        if (!mapTitle.containsKey(fileChangeId)) {
            result = loadTitle(changeRequestId, fileChangeId);
            mapTitle.put(fileChangeId, result);
        } else {
            result = mapTitle.get(fileChangeId);
        }
        return result;
    }

    /**
     * Invalidate cache entry for the given change request and for the document related to the given filechange.
     * This method will retrieve the previous filechange concerning same doc to invalidate this previous entry.
     *
     * @param changeRequestId the identifier of the change request for which to invalidate an entry
     * @param newFileChange the filechange for which to invalidate an entry
     */
    public void invalidate(String changeRequestId, FileChange newFileChange)
    {
        this.invalidate(changeRequestId, newFileChange, null);
    }

    private void invalidate(String changeRequestId, FileChange newFileChange, String newTitle)
    {
        Map<String, String> mapTitle = this.titleCache.get(changeRequestId);
        if (mapTitle != null) {
            ChangeRequest changeRequest = newFileChange.getChangeRequest();
            if (!StringUtils.isBlank(newTitle)) {
                changeRequest.getFileChangeImmediatelyBefore(newFileChange)
                    .ifPresent(oldFileChange -> mapTitle.put(oldFileChange.getId(), newTitle));
            } else {
                changeRequest.getFileChangeImmediatelyBefore(newFileChange)
                    .ifPresent(oldFileChange -> mapTitle.remove(oldFileChange.getId()));
            }
            if (mapTitle.isEmpty()) {
                this.titleCache.remove(changeRequestId);
            }
        }
    }

    /**
     * Invalidate all entries for the given change request.
     *
     * @param changeRequestId the change request for which to invalidate entries
     */
    public void invalidate(String changeRequestId)
    {
        this.titleCache.remove(changeRequestId);
    }

    /**
     * Invalidate all entries contained in this cache.
     */
    public void invalidateAll()
    {
        this.titleCache.removeAll();
    }

    private String loadTitle(String changeRequestId, String fileChangeId)
    {
        String result = null;
        try {
            Optional<ChangeRequest> optionalChangeRequest =
                this.changeRequestStorageManagerProvider.get().load(changeRequestId);
            if (optionalChangeRequest.isPresent()) {
                ChangeRequest changeRequest = optionalChangeRequest.get();
                Optional<FileChange> optionalFileChange = changeRequest
                    .getAllFileChanges()
                    .stream()
                    .filter(fileChange -> fileChange.getId().equals(fileChangeId))
                    .findFirst();
                if (optionalFileChange.isPresent()) {
                    FileChange fileChange = optionalFileChange.get();
                    Optional<FileChange> latestFileChangeFor =
                        changeRequest.getLatestFileChangeFor(fileChange.getTargetEntity());
                    // We always want to load latest title.
                    if (latestFileChangeFor.isPresent() && !fileChange.equals(latestFileChangeFor.get())) {
                        fileChange = latestFileChangeFor.get();
                    }
                    result = this.loadTitle(fileChange);
                } else {
                    this.logger.warn("Cannot find any filechange [{}] in change request [{}] to load title",
                        fileChangeId, changeRequestId);
                }
            } else {
                this.logger.warn("Cannot find any change request [{}] to load title for filechange [{}]",
                    changeRequestId, fileChangeId);
            }
        } catch (ChangeRequestException e) {
            this.logger.error("Error while loading change request [{}] to load title for filechange [{}]: [{}]",
                changeRequestId, fileChangeId, ExceptionUtils.getRootCauseMessage(e));
            this.logger.debug("Full stack trace related to change request loading problem: ", e);
        }
        return result;
    }

    private String loadTitle(FileChange fileChange)
    {
        String result = null;
        ChangeRequest changeRequest = fileChange.getChangeRequest();
        try {
            DocumentModelBridge document;
            if (fileChange.getModifiedDocument() != null) {
                document = fileChange.getModifiedDocument();
            } else {
                document = this.fileChangeStorageManagerProvider.get().getCurrentDocumentFromFileChange(fileChange);
            }
            result = ((XWikiDocument) document).getRenderedTitle(this.contextProvider.get());
            // We invalidate previous entry by replacing its title value.
            this.invalidate(changeRequest.getId(), fileChange, result);
        } catch (ChangeRequestException e) {
            this.logger.error("Error when loading the current document from filechange [{}] to get title: [{}]",
                fileChange,
                ExceptionUtils.getRootCauseMessage(e));
            this.logger.debug("Full stack trace of the loading document error: ", e);
        }
        return result;
    }
}
