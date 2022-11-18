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
package org.xwiki.contrib.changerequest.internal.listeners;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ApproversManager;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.ChangeRequestManager;
import org.xwiki.contrib.changerequest.ChangeRequestReview;
import org.xwiki.contrib.changerequest.events.ApproversUpdatedEvent;
import org.xwiki.contrib.changerequest.internal.UserReferenceConverter;
import org.xwiki.contrib.changerequest.internal.storage.ChangeRequestXClassInitializer;
import org.xwiki.contrib.changerequest.storage.ChangeRequestStorageManager;
import org.xwiki.contrib.changerequest.storage.ReviewStorageManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.observation.event.Event;
import org.xwiki.user.UserException;
import org.xwiki.user.UserManager;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.user.group.GroupException;
import org.xwiki.user.group.GroupManager;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Listener in charge of synchronizing the list of approvers between change requests and published documents.
 *
 * @version $Id$
 * @since 0.14
 */
@Component
@Singleton
@Named(ApproversUpdatedListener.NAME)
public class ApproversUpdatedListener extends AbstractLocalEventListener
{
    static final String NAME = "org.xwiki.contrib.changerequest.internal.listeners.ApproversUpdatedListener";

    private static final List<Event> EVENT_LIST = Collections.singletonList(
        new ApproversUpdatedEvent()
    );

    @Inject
    private Provider<ChangeRequestStorageManager> changeRequestStorageManagerProvider;

    @Inject
    private ApproversManager<ChangeRequest> changeRequestApproversManager;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private Provider<GroupManager> groupManagerProvider;

    @Inject
    private UserReferenceConverter userReferenceConverter;

    @Inject
    private Provider<ReviewStorageManager> reviewStorageManagerProvider;

    @Inject
    @Named("current")
    private UserReferenceResolver<String> userReferenceResolver;

    @Inject
    private Provider<ChangeRequestManager> changeRequestManagerProvider;

    @Inject
    private UserManager userManager;

    @Inject
    private Logger logger;

    /**
     * Default constructor.
     */
    public ApproversUpdatedListener()
    {
        super(NAME, EVENT_LIST);
    }

    @Override
    public void processLocalEvent(Event event, Object source, Object data)
    {
        XWikiDocument sourceDoc = (XWikiDocument) source;
        BaseObject changeRequestObject = sourceDoc.getXObject(ChangeRequestXClassInitializer.CHANGE_REQUEST_XCLASS);
        if (changeRequestObject == null) {
            // Handle the synchronization of existing change requests
            Pair<Set<String>, Set<String>> approversSet = (Pair<Set<String>, Set<String>>) data;
            try {
                List<ChangeRequest> changeRequests = changeRequestStorageManagerProvider.get()
                    .findChangeRequestTargeting(sourceDoc.getDocumentReference());
                changeRequests = changeRequests.stream()
                    .filter(changeRequest -> changeRequest.getStatus().isOpen())
                    .collect(Collectors.toList());
                for (ChangeRequest changeRequest : changeRequests) {
                    this.synchronizeChangeRequest(changeRequest, approversSet);
                }
            } catch (ChangeRequestException e) {
                this.logger.error("Error while processing update of approvers of [{}] with changes [{}]",
                    sourceDoc.getDocumentReference(), data, e);
            }
        }
    }

    private void synchronizeChangeRequest(ChangeRequest changeRequest, Pair<Set<String>, Set<String>> approversSet)
        throws ChangeRequestException
    {
        if (!this.changeRequestApproversManager.wasManuallyEdited(changeRequest)) {
            Set<String> removedApprovers = new HashSet<>(approversSet.getLeft());
            removedApprovers.removeAll(approversSet.getRight());
            if (!removedApprovers.isEmpty()) {
                this.invalidateOutdatedReviews(changeRequest, removedApprovers);
            }

            this.synchronizeApprovers(changeRequest, approversSet.getRight());
            this.computeStatus(changeRequest);
        }
    }

    private void computeStatus(ChangeRequest changeRequest)
    {
        try {
            this.changeRequestManagerProvider.get().computeReadyForMergingStatus(changeRequest);
        } catch (ChangeRequestException e) {
            this.logger.error("Error while computing ready for merging status of [{}]", changeRequest, e);
        }
    }

    private void synchronizeApprovers(ChangeRequest changeRequest, Set<String> newApprovers)
    {
        Set<UserReference> usersApprovers = new LinkedHashSet<>();
        Set<DocumentReference> groupsApprovers = new LinkedHashSet<>();


        for (String newApprover : newApprovers) {
            UserReference userReference = this.userReferenceResolver.resolve(newApprover);
            try {
                if (this.userManager.exists(userReference)) {
                    usersApprovers.add(userReference);
                } else {
                    groupsApprovers.add(this.documentReferenceResolver.resolve(newApprover));
                }
            } catch (UserException e) {
                this.logger.error("Error when checking if user [{}] exists: [{}]", userReference, e);
            }
        }


        try {
            this.changeRequestApproversManager.setUsersApprovers(usersApprovers, changeRequest);
            this.changeRequestApproversManager.setGroupsApprovers(groupsApprovers, changeRequest);
        } catch (ChangeRequestException e) {
            this.logger.error("Error when adding missing approvers in change request [{}]", changeRequest.getId(), e);
        }
    }

    private void invalidateOutdatedReviews(ChangeRequest changeRequest, Set<String> removedApprovers)
        throws ChangeRequestException
    {
        Map<DocumentReference, ChangeRequestReview> changeRequestReviewMap =
            this.getMapOfValidReviews(changeRequest);

        List<ChangeRequestReview> reviewsToInvalidate =
            this.getReviewsToInvalidate(changeRequestReviewMap, removedApprovers);

        for (ChangeRequestReview review : reviewsToInvalidate) {
            review.setValid(false);
            review.setSaved(false);
            this.reviewStorageManagerProvider.get().save(review);
        }
    }

    private List<ChangeRequestReview> getReviewsToInvalidate(Map<DocumentReference, ChangeRequestReview> reviews,
        Set<String> removedApprovers)
    {
        Set<DocumentReference> removedApproversRef = removedApprovers
            .stream()
            .map(this.documentReferenceResolver::resolve)
            .collect(Collectors.toSet());

        List<ChangeRequestReview> reviewsToInvalidate = new ArrayList<>();
        for (DocumentReference removedApproverRef : removedApproversRef) {
            if (reviews.containsKey(removedApproverRef)) {
                reviewsToInvalidate.add(reviews.get(removedApproverRef));
            } else {
                try {
                    Collection<DocumentReference> members =
                        this.groupManagerProvider.get().getMembers(removedApproverRef, true);
                    if (members != null) {
                        for (DocumentReference member : members) {
                            if (reviews.containsKey(member)) {
                                reviewsToInvalidate.add(reviews.get(member));
                            }
                        }
                    }
                } catch (GroupException e) {
                    this.logger.error("Error when getting groups from [{}]", removedApproverRef, e);
                }
            }
        }
        return reviewsToInvalidate;
    }

    private Map<DocumentReference, ChangeRequestReview> getMapOfValidReviews(ChangeRequest changeRequest)
    {
        List<ChangeRequestReview> validReviews =
            changeRequest.getReviews().stream().filter(ChangeRequestReview::isValid)
                .collect(Collectors.toList());

        Map<DocumentReference, ChangeRequestReview> changeRequestReviewMap = new HashMap<>();
        for (ChangeRequestReview validReview : validReviews) {
            DocumentReference convertedAuthor;
            if (validReview.getOriginalApprover() != null) {
                convertedAuthor = this.userReferenceConverter.convert(validReview.getOriginalApprover());
            } else {
                convertedAuthor = this.userReferenceConverter.convert(validReview.getAuthor());
            }

            changeRequestReviewMap.put(convertedAuthor, validReview);
        }

        return changeRequestReviewMap;
    }
}
