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
import java.util.Collections;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.container.servlet.ServletResponse;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.ChangeRequestReference;
import org.xwiki.contrib.changerequest.ChangeRequestReview;
import org.xwiki.contrib.changerequest.discussions.ChangeRequestDiscussionService;
import org.xwiki.contrib.changerequest.discussions.references.ChangeRequestReviewReference;
import org.xwiki.contrib.discussions.DiscussionContextService;
import org.xwiki.contrib.discussions.domain.Discussion;
import org.xwiki.contrib.discussions.domain.DiscussionContext;
import org.xwiki.contrib.discussions.domain.Message;
import org.xwiki.contrib.discussions.server.DiscussionMessageRequestCreator;
import org.xwiki.contrib.discussions.server.DiscussionServerException;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

/**
 * Handler in charge of creating reviews and attached review comments on a change request.
 *
 * @version $Id$
 * @since 1.4
 */
@Component
@Singleton
@Named("review")
public class ReviewChangeRequestHandler extends AbstractChangeRequestActionHandler
{
    private static final String REVIEW_CHOICE_PARAMETER = "reviewChoice";
    private static final String ORIGINAL_APPROVER_PARAMETER = "originalApprover";
    private static final String WITH_COMMENT_PARAMETER = "withComment";

    @Inject
    private DiscussionMessageRequestCreator discussionMessageRequestCreator;

    @Inject
    private UserReferenceResolver<String> stringUserReferenceResolver;

    @Inject
    private UserReferenceResolver<CurrentUserReference> currentUserReferenceUserReferenceResolver;

    @Inject
    private ChangeRequestDiscussionService changeRequestDiscussionService;

    @Inject
    private DiscussionContextService discussionContextService;

    @Override
    public void handle(ChangeRequestReference changeRequestReference) throws ChangeRequestException, IOException
    {
        ChangeRequest changeRequest = this.loadChangeRequest(changeRequestReference);
        HttpServletResponse response =
            ((ServletResponse) this.container.getResponse()).getHttpServletResponse();
        HttpServletRequest request =
            ((ServletRequest) this.container.getRequest()).getHttpServletRequest();

        UserReference currentUser = this.currentUserReferenceUserReferenceResolver
            .resolve(CurrentUserReference.INSTANCE);
        boolean isApproved = StringUtils.equals(request.getParameter(REVIEW_CHOICE_PARAMETER), "approve");
        UserReference originalApprover = null;
        if (!StringUtils.isEmpty(request.getParameter(ORIGINAL_APPROVER_PARAMETER))) {
            originalApprover =
                this.stringUserReferenceResolver.resolve(request.getParameter(ORIGINAL_APPROVER_PARAMETER));
        }

        if (this.isAuthorized(changeRequest, currentUser, originalApprover, response)) {
            ChangeRequestReview changeRequestReview =
                this.changeRequestManager.addReview(changeRequest, currentUser, isApproved, originalApprover);

            if (StringUtils.equals(request.getParameter(WITH_COMMENT_PARAMETER), "1")) {
                this.handleComment(changeRequest, changeRequestReview, request);
            }
            this.answerJSON(HttpServletResponse.SC_OK,
                Collections.singletonMap("reviewReference", changeRequestReview.getId()));
        }
    }

    private void handleComment(ChangeRequest changeRequest, ChangeRequestReview review, HttpServletRequest request)
        throws ChangeRequestException
    {
        ChangeRequestReviewReference changeRequestReviewReference =
            new ChangeRequestReviewReference(review.getId(), changeRequest.getId());

        String errorMessage = "Error when creating the review message.";
        try {
            Message message = this.discussionMessageRequestCreator.createMessage(request);
            Discussion discussion = message.getDiscussion();
            DiscussionContext reviewDiscussionContext = this.changeRequestDiscussionService
                .getOrCreateDiscussionContextFor(changeRequestReviewReference);
            this.discussionContextService.link(reviewDiscussionContext, discussion);
        } catch (DiscussionServerException e) {
            throw new ChangeRequestException(errorMessage, e);
        }
    }

    private boolean isAuthorized(ChangeRequest changeRequest, UserReference currentUser, UserReference originalApprover,
        HttpServletResponse response) throws ChangeRequestException, IOException
    {
        boolean isAuthorized;
        if (originalApprover == null || currentUser.equals(originalApprover)) {
            isAuthorized = this.changeRequestRightsManager.isAuthorizedToReview(currentUser, changeRequest);
        } else {
            isAuthorized = this.changeRequestRightsManager
                .isAuthorizedToReviewOnBehalf(currentUser, changeRequest, originalApprover);
        }

        if (!isAuthorized) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing authorization to perform a review.");
        }
        return isAuthorized;
    }
}
