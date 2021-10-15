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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.discussions.ChangeRequestDiscussionException;
import org.xwiki.contrib.changerequest.discussions.ChangeRequestDiscussionService;
import org.xwiki.contrib.changerequest.discussions.references.AbstractChangeRequestDiscussionContextReference;
import org.xwiki.contrib.changerequest.discussions.references.ChangeRequestFileDiffReference;
import org.xwiki.contrib.changerequest.discussions.references.ChangeRequestLineDiffReference;
import org.xwiki.contrib.changerequest.discussions.references.ChangeRequestReference;
import org.xwiki.contrib.changerequest.discussions.references.ChangeRequestReviewsReference;
import org.xwiki.contrib.discussions.DiscussionContextService;
import org.xwiki.contrib.discussions.DiscussionService;
import org.xwiki.contrib.discussions.DiscussionStoreConfigurationParameters;
import org.xwiki.contrib.discussions.domain.Discussion;
import org.xwiki.contrib.discussions.domain.DiscussionContext;
import org.xwiki.contrib.discussions.domain.references.DiscussionContextEntityReference;
import org.xwiki.localization.ContextualLocalizationManager;

/**
 * Default implementation of {@link ChangeRequestDiscussionService}.
 *
 * @version $Id$
 * @since 0.6
 */
@Component
@Singleton
public class DefaultChangeRequestDiscussionService implements ChangeRequestDiscussionService
{
    private static final String DISCUSSION_CONTEXT_TRANSLATION_PREFIX = "changerequest.discussion.context.";
    private static final String DISCUSSION_TRANSLATION_PREFIX = "changerequest.discussion.";

    @Inject
    private DiscussionContextService discussionContextService;

    @Inject
    private DiscussionService discussionService;

    @Inject
    private ContextualLocalizationManager localizationManager;

    @Inject
    private ChangeRequestDiscussionReferenceUtils discussionReferenceUtils;

    private <T extends AbstractChangeRequestDiscussionContextReference> String getTitleTranslation(String prefix,
        T reference)
    {
        String translationKey = String.format("%s%s.title", prefix, reference.getType().name().toLowerCase());
        List<Object> parameters = this.discussionReferenceUtils.getTranslationParameters(reference);

        return this.localizationManager.getTranslationPlain(translationKey, parameters.toArray());
    }

    private <T extends AbstractChangeRequestDiscussionContextReference> String getDescriptionTranslation(String prefix,
        T reference)
    {
        String translationKey = String.format("%s%s.description", prefix, reference.getType().name().toLowerCase());
        List<Object> parameters = new ArrayList<>();
        parameters.add(reference.getChangeRequestId());
        parameters.addAll(this.discussionReferenceUtils.getTranslationParameters(reference));

        return this.localizationManager.getTranslationPlain(translationKey, parameters.toArray());
    }

    private <T extends AbstractChangeRequestDiscussionContextReference> DiscussionContextEntityReference
        createContextEntityReferenceFor(T reference)
    {
        String entityReference;
        if (!StringUtils.isEmpty(reference.getReference())) {
            entityReference = String.format("%s_%s", reference.getChangeRequestId(), reference.getReference());
        } else {
            entityReference = reference.getChangeRequestId();
        }
        return new DiscussionContextEntityReference(
            String.format("changerequest-%s", reference.getType().name().toLowerCase()),
            entityReference
        );
    }

    private <T extends AbstractChangeRequestDiscussionContextReference> DiscussionStoreConfigurationParameters
        createDiscussionStoreConfigurationParametersFor(T reference)
    {
        DiscussionStoreConfigurationParameters configurationParameters = new DiscussionStoreConfigurationParameters();
        configurationParameters.put(DefaultChangeRequestDiscussionStoreConfiguration.CHANGE_REQUEST_ID_PARAMETER_KEY,
            reference.getChangeRequestId());
        return configurationParameters;
    }

    private <T extends AbstractChangeRequestDiscussionContextReference> DiscussionContext getOrCreateContextFor(
        T reference)
        throws ChangeRequestDiscussionException
    {
        DiscussionContextEntityReference contextEntityReference = this.createContextEntityReferenceFor(reference);
        Optional<DiscussionContext> discussionContext = this.discussionContextService.getOrCreate(
            ChangeRequestDiscussionService.APPLICATION_HINT,
            getTitleTranslation(DISCUSSION_CONTEXT_TRANSLATION_PREFIX, reference),
            getDescriptionTranslation(DISCUSSION_CONTEXT_TRANSLATION_PREFIX, reference),
            contextEntityReference,
            createDiscussionStoreConfigurationParametersFor(reference));
        if (discussionContext.isPresent()) {
            return discussionContext.get();
        } else {
            throw new ChangeRequestDiscussionException(
                String.format("Error while getting or creating discussion context for reference [%s]", reference));
        }
    }

    @Override
    public <T extends AbstractChangeRequestDiscussionContextReference> Discussion getOrCreateDiscussionFor(T reference)
        throws ChangeRequestDiscussionException
    {
        List<DiscussionContext> contextList = new ArrayList<>();

        contextList.add(this.getOrCreateContextFor(new ChangeRequestReference(reference.getChangeRequestId())));
        switch (reference.getType()) {
            case CHANGE_REQUEST_COMMENT:
            case REVIEWS:
            case FILE_DIFF:
                contextList.add(this.getOrCreateContextFor(reference));
                break;

            case REVIEW:
                contextList.add(this.getOrCreateContextFor(reference));
                contextList.add(this.getOrCreateContextFor(
                    new ChangeRequestReviewsReference(reference.getChangeRequestId())));
                break;

            case LINE_DIFF:
                contextList.add(this.getOrCreateContextFor(reference));
                ChangeRequestLineDiffReference lineDiffReference = (ChangeRequestLineDiffReference) reference;
                contextList.add(this.getOrCreateContextFor(
                    new ChangeRequestFileDiffReference(lineDiffReference.getFileChangeId(),
                        lineDiffReference.getChangeRequestId())));
                break;

            case CHANGE_REQUEST:
            default:
                break;
        }

        Optional<Discussion> discussionOptional = this.discussionService.getOrCreate(
            ChangeRequestDiscussionService.APPLICATION_HINT,
            getTitleTranslation(DISCUSSION_TRANSLATION_PREFIX, reference),
            getDescriptionTranslation(DISCUSSION_TRANSLATION_PREFIX, reference),
            contextList.stream().map(DiscussionContext::getReference).collect(Collectors.toList()),
            this.createDiscussionStoreConfigurationParametersFor(reference)
        );
        if (discussionOptional.isPresent()) {
            return discussionOptional.get();
        } else {
            throw new ChangeRequestDiscussionException(
                String.format("Error while getting or creating discussion for reference [%s]", reference));
        }
    }



    @Override
    public <T extends AbstractChangeRequestDiscussionContextReference> List<Discussion> getDiscussionsFrom(T reference)
        throws ChangeRequestDiscussionException
    {
        DiscussionContext discussionContext = this.getOrCreateContextFor(reference);
        return this.discussionService
            .findByDiscussionContexts(Collections.singletonList(discussionContext.getReference()));
    }

    @Override
    public AbstractChangeRequestDiscussionContextReference getReferenceFrom(Discussion discussion)
        throws ChangeRequestDiscussionException
    {
        List<DiscussionContext> discussionContexts =
            this.discussionContextService.findByDiscussionReference(discussion.getReference());
        AbstractChangeRequestDiscussionContextReference reference = null;

        for (DiscussionContext discussionContext : discussionContexts) {
            reference = this.discussionReferenceUtils.computeReferenceFromContext(discussionContext, reference);
        }

        if (reference != null) {
            return reference;
        } else {
            throw new ChangeRequestDiscussionException(
                String.format("Error while computing reference for contexts [%s] from discussion [%s]",
                    discussionContexts, discussion));
        }
    }
}
