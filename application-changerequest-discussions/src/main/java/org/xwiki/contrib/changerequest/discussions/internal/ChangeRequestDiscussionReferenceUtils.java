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
import org.xwiki.contrib.discussions.domain.DiscussionContext;
import org.xwiki.contrib.discussions.domain.references.DiscussionContextEntityReference;
import org.xwiki.contrib.discussions.domain.references.DiscussionContextReference;
import org.xwiki.store.merge.MergeDocumentResult;

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
    private static final String REFERENCE_TYPE_GROUP = "referenceType";
    private static final Pattern ENTITY_REFERENCE_TYPE_PATTERN =
        Pattern.compile(String.format("^changerequest-(?<%s>\\w+)$", REFERENCE_TYPE_GROUP));

    private static final String CHANGE_REQUEST_ID_GROUP = "changeRequestId";
    private static final String REFERENCE_ID_GROUP = "referenceId";

    private static final Pattern ENTITY_REFERENCE_REFERENCE_PATTERN =
        Pattern.compile(String.format("^(?<%s>[\\w-]+)_(?<%s>.+)$", CHANGE_REQUEST_ID_GROUP, REFERENCE_ID_GROUP));

    private String getLineDiffRepresentation(ChangeRequestLineDiffReference lineDiffReference)
    {
        return String.format("%s %s %s",
            lineDiffReference.getDocumentPart(),
            lineDiffReference.getLineChange().name(),
            lineDiffReference.getLineNumber());
    }

    List<Object> getTranslationParameters(AbstractChangeRequestDiscussionContextReference reference)
    {
        List<Object> parameters = new ArrayList<>();
        switch (reference.getType()) {
            case CHANGE_REQUEST:
            case CHANGE_REQUEST_COMMENT:
            case REVIEWS:
                break;

            case REVIEW:
                parameters.add(((ChangeRequestReviewReference) reference).getReviewId());
                break;

            case FILE_DIFF:
                parameters.add(((ChangeRequestFileDiffReference) reference).getFileChangeId());
                break;

            case LINE_DIFF:
                ChangeRequestLineDiffReference lineDiffReference = (ChangeRequestLineDiffReference) reference;
                parameters.add(getLineDiffRepresentation(lineDiffReference));
                parameters.add(lineDiffReference.getFileChangeId());
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
            Matcher lineDiffReferenceMatcher =
                ChangeRequestLineDiffReference.REFERENCE_PATTERN.matcher(lineDiffReference);
            if (lineDiffReferenceMatcher.matches()) {
                String fileChangeId = lineDiffReferenceMatcher.group("fileChangeId");
                MergeDocumentResult.DocumentPart documentPart =
                    MergeDocumentResult.DocumentPart.valueOf(
                        lineDiffReferenceMatcher.group("documentPart").toUpperCase());
                long lineNumber = Long.parseLong(lineDiffReferenceMatcher.group("lineNumber"));
                ChangeRequestLineDiffReference.LineChange lineChange =
                    ChangeRequestLineDiffReference.LineChange.valueOf(
                        lineDiffReferenceMatcher.group("lineChange"));
                result = new ChangeRequestLineDiffReference(fileChangeId, changeRequestId, documentPart,
                    lineNumber, lineChange);
            }
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
            result = new ChangeRequestFileDiffReference(referenceMatcher.group(REFERENCE_ID_GROUP),
                referenceMatcher.group(CHANGE_REQUEST_ID_GROUP));
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
}
