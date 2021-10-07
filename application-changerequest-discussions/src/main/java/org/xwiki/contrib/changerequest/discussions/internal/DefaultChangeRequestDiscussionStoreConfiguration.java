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
package org.xwiki.contrib.changerequest.discussions.internal;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.discussions.ChangeRequestDiscussionService;
import org.xwiki.contrib.changerequest.storage.ChangeRequestStorageManager;
import org.xwiki.contrib.discussions.DiscussionStoreConfigurationParameters;
import org.xwiki.contrib.discussions.domain.references.DiscussionContextEntityReference;
import org.xwiki.contrib.discussions.domain.references.DiscussionReference;
import org.xwiki.contrib.discussions.store.DiscussionStoreConfiguration;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.SpaceReference;

import com.xpn.xwiki.XWikiContext;

/**
 * Implementation of the {@link DiscussionStoreConfiguration} for change request.
 * This implementation relies on a specific store parameter to store all discussions elements on the same space than
 * a given change request.
 *
 * @version $Id$
 * @since 0.6
 */
@Component
@Named(ChangeRequestDiscussionService.APPLICATION_HINT)
@Singleton
public class DefaultChangeRequestDiscussionStoreConfiguration implements DiscussionStoreConfiguration
{
    /**
     * The store parameter key to use for specifying the change request identifier.
     */
    public static final String CHANGE_REQUEST_ID_PARAMETER_KEY = "changeRequestId";

    @Inject
    private DocumentReferenceResolver<ChangeRequest> changeRequestDocumentReferenceResolver;

    @Inject
    private ChangeRequestStorageManager changeRequestStorageManager;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private Logger logger;

    @Override
    public SpaceReference getDiscussionContextSpaceStorageLocation(DiscussionStoreConfigurationParameters parameters,
        DiscussionContextEntityReference contextEntityReference)
    {
        return new SpaceReference("DiscussionContext", this.getChangeRequestDiscussionSpaceReference(parameters));
    }

    @Override
    public SpaceReference getDiscussionSpaceStorageLocation(DiscussionStoreConfigurationParameters parameters)
    {
        return new SpaceReference("Discussion", this.getChangeRequestDiscussionSpaceReference(parameters));
    }

    @Override
    public SpaceReference getMessageSpaceStorageLocation(DiscussionStoreConfigurationParameters parameters,
        DiscussionReference discussionReference)
    {
        return new SpaceReference("Message", this.getChangeRequestDiscussionSpaceReference(parameters));
    }

    private SpaceReference getChangeRequestDiscussionSpaceReference(DiscussionStoreConfigurationParameters parameters)
    {
        Object changeRequestIdObj = parameters.get(CHANGE_REQUEST_ID_PARAMETER_KEY);
        Optional<ChangeRequest> changeRequestOptional = Optional.empty();

        String changeRequestId = null;
        if (changeRequestIdObj instanceof String[]) {
            changeRequestId = ((String[]) changeRequestIdObj)[0];
        } else if (changeRequestIdObj instanceof String) {
            changeRequestId = (String) changeRequestIdObj;
        }
        if (changeRequestId != null) {
            try {
                changeRequestOptional = this.changeRequestStorageManager.load(changeRequestId);
            } catch (ChangeRequestException e) {
                logger.warn("Error while getting value for change request id from [{}]: [{}]", changeRequestIdObj,
                    ExceptionUtils.getRootCauseMessage(e));
            }
        } else {
            logger.warn("Missing or wrong parameter for change request id: [{}]", changeRequestIdObj);
        }

        SpaceReference result;
        if (changeRequestOptional.isPresent()) {
            DocumentReference documentReference =
                this.changeRequestDocumentReferenceResolver.resolve(changeRequestOptional.get());
            result = documentReference.getLastSpaceReference();
        } else {
            result = new SpaceReference("ChangeRequest", this.contextProvider.get().getWikiReference());
        }
        return new SpaceReference("Discussions", result);
    }

}
