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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.discussions.ChangeRequestDiscussionService;
import org.xwiki.contrib.changerequest.discussions.references.AbstractChangeRequestDiscussionContextReference;
import org.xwiki.contrib.changerequest.discussions.references.ChangeRequestCommentReference;
import org.xwiki.contrib.changerequest.discussions.references.ChangeRequestDiscussionReferenceType;
import org.xwiki.contrib.changerequest.discussions.references.ChangeRequestFileDiffReference;
import org.xwiki.contrib.changerequest.discussions.references.ChangeRequestLineDiffReference;
import org.xwiki.contrib.changerequest.discussions.references.ChangeRequestReference;
import org.xwiki.contrib.changerequest.discussions.references.ChangeRequestReviewReference;
import org.xwiki.contrib.changerequest.discussions.references.ChangeRequestReviewsReference;
import org.xwiki.contrib.changerequest.discussions.references.difflocation.FileDiffLocation;
import org.xwiki.contrib.changerequest.discussions.references.difflocation.LineDiffLocation;
import org.xwiki.contrib.discussions.domain.DiscussionContext;
import org.xwiki.contrib.discussions.domain.references.DiscussionContextEntityReference;
import org.xwiki.contrib.discussions.domain.references.DiscussionContextReference;
import org.xwiki.localization.ContextualLocalizationManager;

/**
 * Component responsible to handle {@link AbstractChangeRequestDiscussionContextReference} and their translation to
 * {@link org.xwiki.contrib.discussions.domain.DiscussionContext}.
 *
 * @version $Id$
 * @since 0.6
 */
@Component(roles = ChangeRequestDiscussionReferenceUtils.class)
@Singleton
public class ChangeRequestDiscussionReferenceUtils
{
    static final String DISCUSSION_CONTEXT_TRANSLATION_PREFIX = "changerequest.discussion.context.";
    static final String DISCUSSION_TRANSLATION_PREFIX = "changerequest.discussion.";

    private static final String REFERENCE_TYPE_GROUP = "referenceType";
    private static final Pattern ENTITY_REFERENCE_TYPE_PATTERN =
        Pattern.compile(String.format("^changerequest-(?<%s>\\w+)$", REFERENCE_TYPE_GROUP));

    private static final String CHANGE_REQUEST_ID_GROUP = "changeRequestId";
    private static final String REFERENCE_ID_GROUP = "referenceId";

    private static final Pattern ENTITY_REFERENCE_REFERENCE_PATTERN =
        Pattern.compile(String.format("^(?<%s>[\\w-]+)(%s)(?<%s>.+)$",
            CHANGE_REQUEST_ID_GROUP, ChangeRequestDiscussionFactory.CR_ID_REF_ID_SEPARATOR, REFERENCE_ID_GROUP));

    @Inject
    private ContextualLocalizationManager localizationManager;

    private String getLineDiffRepresentation(LineDiffLocation lineDiffLocation)
    {
        return String.format("%s %s %s",
            lineDiffLocation.getDocumentPart(),
            lineDiffLocation.getLineChange().name(),
            lineDiffLocation.getLineNumber());
    }

    private List<String> getTranslationParameters(AbstractChangeRequestDiscussionContextReference reference)
    {
        List<String> parameters = new ArrayList<>();
        switch (reference.getType()) {
            case CHANGE_REQUEST:
            case CHANGE_REQUEST_COMMENT:
            case REVIEWS:
                break;

            case REVIEW:
                parameters.add(((ChangeRequestReviewReference) reference).getReviewId());
                break;

            case FILE_DIFF:
                parameters.add(((ChangeRequestFileDiffReference) reference).getFileDiffLocation().getTargetReference());
                break;

            case LINE_DIFF:
                ChangeRequestLineDiffReference lineDiffReference = (ChangeRequestLineDiffReference) reference;
                LineDiffLocation lineDiffLocation = lineDiffReference.getLineDiffLocation();
                parameters.add(getLineDiffRepresentation(lineDiffLocation));
                parameters.add(lineDiffLocation.getFileDiffLocation().getTargetReference());
                break;

            default:
                break;
        }
        return parameters;
    }

    private ChangeRequestLineDiffReference computeLineDiffReferenceFrom(String reference)
    {
        ChangeRequestLineDiffReference result = null;
        Matcher referenceMatcher = ENTITY_REFERENCE_REFERENCE_PATTERN.matcher(reference);

        if (referenceMatcher.matches()) {
            String changeRequestId = referenceMatcher.group(CHANGE_REQUEST_ID_GROUP);
            String lineDiffReference = referenceMatcher.group(REFERENCE_ID_GROUP);
            LineDiffLocation lineDiffLocation = LineDiffLocation.parse(lineDiffReference);
            result = new ChangeRequestLineDiffReference(changeRequestId, lineDiffLocation);
        }
        return result;
    }

    private AbstractChangeRequestDiscussionContextReference computeFileDiffReferenceFrom(String reference,
        AbstractChangeRequestDiscussionContextReference previousReference)
    {
        AbstractChangeRequestDiscussionContextReference result = previousReference;
        Matcher referenceMatcher = ENTITY_REFERENCE_REFERENCE_PATTERN.matcher(reference);
        if ((previousReference == null
            || previousReference.getType() == ChangeRequestDiscussionReferenceType.CHANGE_REQUEST)
            && referenceMatcher.matches()) {
            String changeRequestId = referenceMatcher.group(CHANGE_REQUEST_ID_GROUP);
            String fileDiffReference = referenceMatcher.group(REFERENCE_ID_GROUP);
            FileDiffLocation fileDiffLocation = FileDiffLocation.parse(fileDiffReference);
            result = new ChangeRequestFileDiffReference(changeRequestId, fileDiffLocation);
        }
        return result;
    }

    private AbstractChangeRequestDiscussionContextReference computeReviewDiffReferenceFrom(String reference,
        AbstractChangeRequestDiscussionContextReference previousReference)
    {
        AbstractChangeRequestDiscussionContextReference result = previousReference;
        Matcher referenceMatcher = ENTITY_REFERENCE_REFERENCE_PATTERN.matcher(reference);
        if (referenceMatcher.matches()) {
            result = new ChangeRequestReviewReference(referenceMatcher.group(REFERENCE_ID_GROUP),
                referenceMatcher.group(CHANGE_REQUEST_ID_GROUP));
        }
        return result;
    }

    private AbstractChangeRequestDiscussionContextReference computeReferenceFromType(
        ChangeRequestDiscussionReferenceType type, String reference,
        AbstractChangeRequestDiscussionContextReference previousReference)
    {
        AbstractChangeRequestDiscussionContextReference result = previousReference;
        switch (type) {
            case CHANGE_REQUEST_COMMENT:
                result = new ChangeRequestCommentReference(reference);
                break;

            case REVIEWS:
                if (previousReference == null
                    || previousReference.getType() == ChangeRequestDiscussionReferenceType.CHANGE_REQUEST) {
                    result = new ChangeRequestReviewsReference(reference);
                }
                break;

            case REVIEW:
                result = this.computeReviewDiffReferenceFrom(reference, previousReference);
                break;

            case FILE_DIFF:
                result = this.computeFileDiffReferenceFrom(reference, previousReference);
                break;

            case LINE_DIFF:
                result = this.computeLineDiffReferenceFrom(reference);
                break;

            case CHANGE_REQUEST:
            default:
                if (previousReference == null) {
                    result = new ChangeRequestReference(reference);
                }
                break;
        }
        return result;
    }

    AbstractChangeRequestDiscussionContextReference computeReferenceFromContext(
        DiscussionContext discussionContext, AbstractChangeRequestDiscussionContextReference previousReference)
    {
        AbstractChangeRequestDiscussionContextReference reference = previousReference;
        DiscussionContextReference contextReference = discussionContext.getReference();
        DiscussionContextEntityReference entityReference = discussionContext.getEntityReference();
        Matcher typeMatcher = ENTITY_REFERENCE_TYPE_PATTERN.matcher(entityReference.getType());
        if (ChangeRequestDiscussionService.APPLICATION_HINT.equals(contextReference.getApplicationHint())
            && typeMatcher.matches()) {
            ChangeRequestDiscussionReferenceType referenceType =
                ChangeRequestDiscussionReferenceType.valueOf(typeMatcher.group(REFERENCE_TYPE_GROUP).toUpperCase());
            reference = this.computeReferenceFromType(referenceType, entityReference.getReference(), previousReference);
        }
        return reference;
    }

    <T extends AbstractChangeRequestDiscussionContextReference> String getTitleTranslation(String prefix,
        T reference)
    {
        String translationKey = String.format("%s%s.title", prefix, reference.getType().name().toLowerCase());
        List<String> parameters = this.getTranslationParameters(reference);

        return this.localizationManager.getTranslationPlain(translationKey, parameters.toArray());
    }

    <T extends AbstractChangeRequestDiscussionContextReference> String getDescriptionTranslation(String prefix,
        T reference)
    {
        String translationKey = String.format("%s%s.description", prefix, reference.getType().name().toLowerCase());
        List<Object> parameters = new ArrayList<>();
        parameters.add(reference.getChangeRequestId());
        parameters.addAll(this.getTranslationParameters(reference));

        return this.localizationManager.getTranslationPlain(translationKey, parameters.toArray());
    }
}
