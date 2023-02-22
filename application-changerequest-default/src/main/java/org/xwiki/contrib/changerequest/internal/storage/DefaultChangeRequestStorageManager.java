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
package org.xwiki.contrib.changerequest.internal.storage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.changerequest.ApproversManager;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestConfiguration;
import org.xwiki.contrib.changerequest.ChangeRequestReview;
import org.xwiki.contrib.changerequest.ChangeRequestRightsManager;
import org.xwiki.contrib.changerequest.ChangeRequestStatus;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.discussions.ChangeRequestDiscussionService;
import org.xwiki.contrib.changerequest.events.ChangeRequestCreatedEvent;
import org.xwiki.contrib.changerequest.events.ChangeRequestMergeFailedEvent;
import org.xwiki.contrib.changerequest.events.ChangeRequestMergedEvent;
import org.xwiki.contrib.changerequest.events.ChangeRequestMergingEvent;
import org.xwiki.contrib.changerequest.events.ChangeRequestStatusChangedEvent;
import org.xwiki.contrib.changerequest.events.SplitBeginChangeRequestEvent;
import org.xwiki.contrib.changerequest.events.SplitEndChangeRequestEvent;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.internal.cache.ChangeRequestStorageCacheManager;
import org.xwiki.contrib.changerequest.storage.ChangeRequestIDGenerator;
import org.xwiki.contrib.changerequest.storage.ChangeRequestStorageManager;
import org.xwiki.contrib.changerequest.storage.FileChangeStorageManager;
import org.xwiki.contrib.changerequest.storage.ReviewStorageManager;
import org.xwiki.job.JobException;
import org.xwiki.job.JobExecutor;
import org.xwiki.model.document.DocumentAuthors;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.refactoring.job.EntityRequest;
import org.xwiki.refactoring.job.RefactoringJobs;
import org.xwiki.refactoring.script.RequestFactory;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.user.UserReferenceSerializer;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.xwiki.contrib.changerequest.internal.storage.ChangeRequestXClassInitializer.AUTHORS_FIELD;
import static org.xwiki.contrib.changerequest.internal.storage.ChangeRequestXClassInitializer.CHANGED_DOCUMENTS_FIELD;
import static org.xwiki.contrib.changerequest.internal.storage.ChangeRequestXClassInitializer.CHANGE_REQUEST_XCLASS;
import static org.xwiki.contrib.changerequest.internal.storage.ChangeRequestXClassInitializer.STALE_DATE_FIELD;
import static org.xwiki.contrib.changerequest.internal.storage.ChangeRequestXClassInitializer.STATUS_FIELD;

/**
 * Default implementation of {@link ChangeRequestStorageManager}.
 * The change request are stored as XWiki documents located on a dedicated space.
 *
 * @version $Id$
 * @since 0.1
 */
@Component
@Singleton
public class DefaultChangeRequestStorageManager implements ChangeRequestStorageManager
{
    private static final String REFERENCE = "reference";

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private FileChangeStorageManager fileChangeStorageManager;

    @Inject
    private DocumentReferenceResolver<ChangeRequest> changeRequestDocumentReferenceResolver;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> localEntityReferenceSerializer;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private UserReferenceSerializer<String> userReferenceSerializer;

    @Inject
    private ChangeRequestIDGenerator defaultIDGenerator;

    @Inject
    private QueryManager queryManager;

    @Inject
    private ChangeRequestConfiguration configuration;

    @Inject
    private ReviewStorageManager reviewStorageManager;

    @Inject
    private ChangeRequestDiscussionService discussionService;

    @Inject
    private ObservationManager observationManager;

    @Inject
    private RequestFactory refactoringRequestFactory;

    @Inject
    private JobExecutor jobExecutor;

    @Inject
    private ChangeRequestRightsManager changeRequestRightsManager;

    @Inject
    private ChangeRequestStorageCacheManager changeRequestStorageCacheManager;

    @Inject
    @Named("document")
    private UserReferenceResolver<DocumentReference> userReferenceResolver;

    @Inject
    private ApproversManager<ChangeRequest> approversManager;

    @Inject
    private ApproversManager<FileChange> fileChangeApproversManager;

    @Inject
    @Named("count")
    private QueryFilter countQueryFilter;

    @Inject
    @Named("context")
    private ComponentManager componentManager;

    @Inject
    private Logger logger;

    private ChangeRequestIDGenerator getIdGenerator()
    {
        ChangeRequestIDGenerator result = this.defaultIDGenerator;
        String idGeneratorHint = this.configuration.getIdGeneratorHint();
        if (!StringUtils.isBlank(idGeneratorHint)
            && this.componentManager.hasComponent(ChangeRequestIDGenerator.class, idGeneratorHint)) {
            try {
                result = this.componentManager.getInstance(ChangeRequestIDGenerator.class, idGeneratorHint);
            } catch (ComponentLookupException e) {
                this.logger.error("Error while loading ChangeRequestIDGenerator component with hint [{}]: [{}]",
                    idGeneratorHint,
                    ExceptionUtils.getRootCauseMessage(e));
                this.logger.debug("Full stack trace of component loading error: ", e);
            }
        } else if (!StringUtils.isBlank(idGeneratorHint)) {
            this.logger.warn("Cannot find ChangeRequestIDGenerator component with hint [{}], it will fallback on "
                + "default implementation.", idGeneratorHint);
        }
        return result;
    }

    @Override
    public void save(ChangeRequest changeRequest) throws ChangeRequestException
    {
        XWikiContext context = this.contextProvider.get();
        XWiki wiki = context.getWiki();
        if (changeRequest.getId() == null) {
            changeRequest.setId(this.getIdGenerator().generateId(changeRequest));
        }
        DocumentReference reference = this.changeRequestDocumentReferenceResolver.resolve(changeRequest);
        try {
            XWikiDocument document = wiki.getDocument(reference, context);
            document.setTitle(changeRequest.getTitle());
            document.setContent(changeRequest.getDescription());
            DocumentAuthors authors = document.getAuthors();
            if (document.isNew()) {
                authors.setCreator(changeRequest.getCreator());
            }
            authors.setOriginalMetadataAuthor(this.userReferenceResolver.resolve(context.getUserReference()));
            BaseObject xObject = document.getXObject(CHANGE_REQUEST_XCLASS, 0, true, context);
            xObject.set(STATUS_FIELD, changeRequest.getStatus().name().toLowerCase(Locale.ROOT), context);

            List<String> serializedReferences = changeRequest.getModifiedDocuments().stream()
                .map(target -> this.localEntityReferenceSerializer.serialize(target))
                .collect(Collectors.toList());
            xObject.set(CHANGED_DOCUMENTS_FIELD, serializedReferences, context);

            List<String> serializedAuthors = changeRequest.getAuthors().stream()
                .map(target -> this.userReferenceSerializer.serialize(target))
                .collect(Collectors.toList());

            xObject.set(AUTHORS_FIELD, serializedAuthors, context);
            xObject.set(STALE_DATE_FIELD, null, context);

            for (FileChange fileChange : changeRequest.getAllFileChanges()) {
                this.fileChangeStorageManager.save(fileChange);
            }
            wiki.saveDocument(document, context);
        } catch (XWikiException e) {
            throw new ChangeRequestException(
                String.format("Error while saving the change request [%s]", changeRequest), e);
        }
        this.changeRequestStorageCacheManager.invalidate(changeRequest.getId());
    }

    @Override
    public void saveStaleDate(ChangeRequest changeRequest) throws ChangeRequestException
    {
        XWikiContext context = this.contextProvider.get();
        XWiki wiki = context.getWiki();
        if (changeRequest.getId() == null) {
            throw new ChangeRequestException("The stale date can only be saved for existing change requests.");
        } else {
            DocumentReference reference = this.changeRequestDocumentReferenceResolver.resolve(changeRequest);
            try {
                XWikiDocument document = wiki.getDocument(reference, context);
                BaseObject xObject = document.getXObject(CHANGE_REQUEST_XCLASS, 0, true, context);
                xObject.set(STALE_DATE_FIELD, changeRequest.getStaleDate(), context);
                wiki.saveDocument(document, context);
            } catch (XWikiException e) {
                throw new ChangeRequestException("Error while saving the change request stale date", e);
            }
        }
    }

    @Override
    public Optional<ChangeRequest> load(String changeRequestId) throws ChangeRequestException
    {
        Optional<ChangeRequest> result = this.changeRequestStorageCacheManager.getChangeRequest(changeRequestId);

        if (result.isEmpty()) {
            ChangeRequest changeRequest = new ChangeRequest();
            changeRequest.setId(changeRequestId);
            DocumentReference reference = this.changeRequestDocumentReferenceResolver.resolve(changeRequest);
            XWikiContext context = this.contextProvider.get();
            WikiReference currentWiki = context.getWikiReference();
            XWiki wiki = context.getWiki();
            try {
                XWikiDocument document = wiki.getDocument(reference, context);
                BaseObject xObject = document.getXObject(CHANGE_REQUEST_XCLASS);
                if (!document.isNew() && xObject != null) {
                    ChangeRequestStatus status = ChangeRequestStatus.valueOf(
                        xObject.getStringValue(STATUS_FIELD).toUpperCase());
                    Date staleDate = xObject.getDateValue(STALE_DATE_FIELD);
                    changeRequest
                        .setTitle(document.getTitle())
                        .setDescription(document.getContent())
                        .setCreator(document.getAuthors().getCreator())
                        .setStatus(status)
                        .setCreationDate(document.getCreationDate())
                        .setStaleDate(staleDate);
                    List<String> changedDocuments = xObject.getListValue(CHANGED_DOCUMENTS_FIELD);

                    for (String changedDocument : changedDocuments) {
                        DocumentReference changedDocumentReference =
                            this.documentReferenceResolver.resolve(changedDocument, currentWiki);
                        List<FileChange> fileChanges =
                            this.fileChangeStorageManager.load(changeRequest, changedDocumentReference);
                        for (FileChange fileChange : fileChanges) {
                            changeRequest.addFileChange(fileChange);
                        }
                    }

                    this.reviewStorageManager.load(changeRequest);
                    result = Optional.of(changeRequest);
                    this.changeRequestStorageCacheManager.cacheChangeRequest(changeRequest);
                }
            } catch (XWikiException e) {
                throw new ChangeRequestException(
                    String.format("Error while trying to load change request of id [%s]", changeRequestId), e);
            }
        }
        return result;
    }

    @Override
    public void merge(ChangeRequest changeRequest) throws ChangeRequestException
    {
        this.observationManager.notify(new ChangeRequestMergingEvent(), changeRequest.getId(), changeRequest);
        // We immediately save the merge status to avoid having the listeners to consider this change request
        // when computing status changes.
        ChangeRequestStatus oldStatus = changeRequest.getStatus();
        changeRequest.setStatus(ChangeRequestStatus.MERGED);
        this.save(changeRequest);
        this.observationManager.notify(new ChangeRequestStatusChangedEvent(), changeRequest.getId(),
            new ChangeRequestStatus[] {oldStatus, ChangeRequestStatus.MERGED});

        try {
            Set<DocumentReference> documentReferences = changeRequest.getFileChanges().keySet();
            for (DocumentReference documentReference : documentReferences) {
                Optional<FileChange> optionalFileChange = changeRequest.getLatestFileChangeFor(documentReference);
                if (optionalFileChange.isPresent()) {
                    this.fileChangeStorageManager.merge(optionalFileChange.get());
                }
            }
            this.observationManager.notify(new ChangeRequestMergedEvent(), changeRequest.getId(), changeRequest);
        } catch (ChangeRequestException e) {
            // in case of error we reset the status
            changeRequest.setStatus(oldStatus);
            this.save(changeRequest);
            this.observationManager.notify(new ChangeRequestStatusChangedEvent(), changeRequest.getId(),
                new ChangeRequestStatus[] {ChangeRequestStatus.MERGED, oldStatus});
            this.observationManager.notify(new ChangeRequestMergeFailedEvent(), changeRequest.getId(), changeRequest);
        }
    }

    @Override
    public List<DocumentReference> getOpenChangeRequestMatchingName(String title) throws ChangeRequestException
    {
        String statement = String.format(", BaseObject as obj , StringProperty as obj_status where "
            + "doc.fullName like :reference and obj_status.value in %s and "
            + "doc.fullName=obj.name and obj.className='%s' and obj_status.id.id=obj.id and obj_status.id.name='%s'",
            getInOpenStatusesStatement(), this.entityReferenceSerializer.serialize(CHANGE_REQUEST_XCLASS),
            STATUS_FIELD);
        SpaceReference changeRequestSpaceLocation = this.configuration.getChangeRequestSpaceLocation();
        try {
            Query query = this.queryManager.createQuery(statement, Query.HQL);
            query.bindValue(REFERENCE, String.format("%s.%%%s%%",
                this.localEntityReferenceSerializer.serialize(changeRequestSpaceLocation), title));
            List<String> changeRequestDocuments = query.execute();
            return changeRequestDocuments.stream()
                .map(this.documentReferenceResolver::resolve).collect(Collectors.toList());
        } catch (QueryException e) {
            throw new ChangeRequestException(
                String.format("Error while looking for change requests with title [%s]", title), e);
        }
    }

    @Override
    public List<ChangeRequest> findChangeRequestTargeting(DocumentReference documentReference)
        throws ChangeRequestException
    {
        List<ChangeRequest> result = new ArrayList<>();
        List<DocumentReference> changeRequestReferences = this.findChangeRequestReferenceTargeting(documentReference);
        for (DocumentReference crReference : changeRequestReferences) {
            this.load(crReference.getLastSpaceReference().getName()).ifPresent(result::add);
        }
        return result;
    }

    @Override
    public List<DocumentReference> findChangeRequestReferenceTargeting(DocumentReference documentReference)
        throws ChangeRequestException
    {
        List<DocumentReference> result = new ArrayList<>();
        String statement = String.format("from doc.object(%s) as obj where :reference member of obj.%s",
            this.entityReferenceSerializer.serialize(CHANGE_REQUEST_XCLASS), CHANGED_DOCUMENTS_FIELD);
        try {
            Query query = this.queryManager.createQuery(statement, Query.XWQL);
            query.bindValue(REFERENCE, this.localEntityReferenceSerializer.serialize(documentReference));
            List<String> changeRequestDocuments = query.execute();
            for (String changeRequestDocument : changeRequestDocuments) {
                DocumentReference crReference = this.documentReferenceResolver.resolve(changeRequestDocument);
                result.add(crReference);
            }
        } catch (QueryException e) {
            throw new ChangeRequestException(
                String.format("Error while trying to get change request for document [%s]", documentReference), e);
        }
        return result;
    }

    @Override
    public List<ChangeRequest> findChangeRequestTargeting(SpaceReference spaceReference)
        throws ChangeRequestException
    {
        List<ChangeRequest> result = new ArrayList<>();
        String statement = String.format(", BaseObject as obj, DBStringListProperty as prop join prop.list list "
            + "where obj.name=doc.fullName and obj.className='%s' and obj.id=prop.id.id and "
            + "prop.id.name='%s' and list like :reference order by doc.creationDate desc",
            this.entityReferenceSerializer.serialize(CHANGE_REQUEST_XCLASS), CHANGED_DOCUMENTS_FIELD);
        try {
            Query query = this.queryManager.createQuery(statement, Query.HQL);
            query.bindValue(REFERENCE,
                String.format("%%%s%%", this.localEntityReferenceSerializer.serialize(spaceReference)));
            List<String> changeRequestDocuments = query.execute();
            for (String changeRequestDocument : changeRequestDocuments) {
                DocumentReference crReference = this.documentReferenceResolver.resolve(changeRequestDocument);
                this.load(crReference.getLastSpaceReference().getName()).ifPresent(result::add);
            }
        } catch (QueryException e) {
            throw new ChangeRequestException(
                String.format("Error while trying to get change request for space [%s]", spaceReference), e);
        }

        return result;
    }

    private String getInOpenStatusesStatement()
    {
        List<ChangeRequestStatus> openStatuses =
            Arrays.stream(ChangeRequestStatus.values()).filter(ChangeRequestStatus::isOpen)
                .collect(Collectors.toList());

        StringBuilder statusStatement = new StringBuilder("(");
        Iterator<ChangeRequestStatus> iterator = openStatuses.iterator();
        char apos = '\'';
        while (iterator.hasNext()) {
            ChangeRequestStatus status = iterator.next();
            statusStatement.append(apos);
            statusStatement.append(status.name().toLowerCase());
            statusStatement.append(apos);
            if (iterator.hasNext()) {
                statusStatement.append(",");
            }
        }
        statusStatement.append(")");
        return statusStatement.toString();
    }

    @Override
    public List<ChangeRequest> findOpenChangeRequestsByDate(Date limitDate, boolean considerCreationDate)
        throws ChangeRequestException
    {
        String columnDate = (considerCreationDate) ? "creationDate" : "date";
        String statement = String.format(", BaseObject as obj , StringProperty as obj_status where "
            + "doc.%s < :limitDate and obj_status.value in %s and "
            + "doc.fullName=obj.name and obj.className='%s' and obj_status.id.id=obj.id and obj_status.id.name='%s'",
            columnDate, getInOpenStatusesStatement(), this.entityReferenceSerializer.serialize(CHANGE_REQUEST_XCLASS),
            STATUS_FIELD);

        return this.findChangeRequestWithStatementAndLimitDate(statement, limitDate);
    }

    @Override
    public List<ChangeRequest> findChangeRequestsStaledBefore(Date limitDate) throws ChangeRequestException
    {
        String statement = String.format(", BaseObject as obj , StringProperty as obj_status, "
                + "DateProperty as obj_staled where "
                + "obj_staled.value < :limitDate and obj_status.value in %s and "
                + "doc.fullName=obj.name and obj.className='%s' "
                + "and obj_status.id.id=obj.id and obj_status.id.name='%s' "
                + "and obj_staled.id.id=obj.id and obj_staled.id.name='%s'",
            getInOpenStatusesStatement(), this.entityReferenceSerializer.serialize(CHANGE_REQUEST_XCLASS),
            STATUS_FIELD, STALE_DATE_FIELD);

        return this.findChangeRequestWithStatementAndLimitDate(statement, limitDate);
    }

    private List<ChangeRequest> findChangeRequestWithStatementAndLimitDate(String statement, Date limitDate)
        throws ChangeRequestException
    {
        List<ChangeRequest> result = new ArrayList<>();
        try {
            Query query = this.queryManager.createQuery(statement, Query.HQL);
            query.bindValue("limitDate", limitDate);
            List<String> changeRequestDocuments = query.execute();
            for (String changeRequestDocument : changeRequestDocuments) {
                DocumentReference crReference = this.documentReferenceResolver.resolve(changeRequestDocument);
                this.load(crReference.getLastSpaceReference().getName()).ifPresent(result::add);
            }
        } catch (QueryException e) {
            throw new ChangeRequestException(
                String.format("Error while querying change requests with statement [%s] and limitDate [%s]",
                    statement, limitDate), e);
        }

        return result;
    }

    @Override
    public List<ChangeRequest> split(ChangeRequest changeRequest) throws ChangeRequestException
    {
        List<ChangeRequest> result = new ArrayList<>();

        // If the CR only contains a single document, the split shouldn't have any effect.
        if (changeRequest.getModifiedDocuments().size() > 1) {
            this.observationManager.notify(new SplitBeginChangeRequestEvent(), changeRequest.getId(), changeRequest);

            // Perform the actual split
            result.addAll(this.performFileChangeSplit(changeRequest));

            // Handle the reviews
            for (ChangeRequest splittedChangeRequest : result) {
                for (ChangeRequestReview review : changeRequest.getReviews()) {
                    ChangeRequestReview clonedReview = review.cloneWithChangeRequest(splittedChangeRequest);

                    // we consider reviews as outdated for splitted change requests
                    // and we keep same id to avoid having to perform a mapping old/new reviews in discussions
                    clonedReview.setValid(false);
                    clonedReview.setId(review.getId());

                    splittedChangeRequest.addReview(clonedReview);
                    this.reviewStorageManager.save(clonedReview);
                }
            }

            // Handle the approvers
            this.handleApproversInSplittedCR(changeRequest, result);

            // Handle discussions last to not break the CR in case of problem there.
            this.discussionService.moveDiscussions(changeRequest, result);
            DocumentReference changeRequestDocument =
                this.changeRequestDocumentReferenceResolver.resolve(changeRequest);
            EntityRequest deleteRequest =
                this.refactoringRequestFactory.createDeleteRequest(Collections.singletonList(changeRequestDocument));
            deleteRequest.setDeep(true);
            deleteRequest.setCheckAuthorRights(false);
            deleteRequest.setCheckRights(false);
            try {
                this.jobExecutor.execute(RefactoringJobs.DELETE, deleteRequest);
            } catch (JobException e) {
                throw new ChangeRequestException(
                    String.format("Error while performing deletion of change request document [%s]",
                        changeRequestDocument),
                    e);
            }
            this.observationManager.notify(new SplitEndChangeRequestEvent(), changeRequest.getId(), result);
        }
        return result;
    }

    private List<ChangeRequest> performFileChangeSplit(ChangeRequest changeRequest) throws ChangeRequestException
    {
        List<ChangeRequest> result = new ArrayList<>();
        Map<DocumentReference, Deque<FileChange>> fileChanges = changeRequest.getFileChanges();

        // Split the change request and create the new ones with appropriate file changes
        // and handle rights right away
        for (Map.Entry<DocumentReference, Deque<FileChange>> entry : fileChanges.entrySet()) {
            ChangeRequest splittedChangeRequest = changeRequest.cloneWithoutFileChanges();

            for (FileChange fileChange : entry.getValue()) {
                FileChange clonedFileChange = fileChange
                    .cloneWithChangeRequestAndType(splittedChangeRequest, fileChange.getType());
                splittedChangeRequest.addFileChange(clonedFileChange);
            }

            this.save(splittedChangeRequest);

            this.changeRequestRightsManager.copyAllButViewRights(changeRequest, splittedChangeRequest);
            this.changeRequestRightsManager.copyViewRights(splittedChangeRequest, entry.getKey());

            this.observationManager.notify(new ChangeRequestCreatedEvent(),
                splittedChangeRequest.getId(), splittedChangeRequest);
            result.add(splittedChangeRequest);
        }
        return result;
    }

    private void handleApproversInSplittedCR(ChangeRequest changeRequest, List<ChangeRequest> result)
        throws ChangeRequestException
    {
        // Set back the list of approvers
        if (this.approversManager.wasManuallyEdited(changeRequest)) {
            Set<DocumentReference> groupsApprovers = this.approversManager.getGroupsApprovers(changeRequest);
            Set<UserReference> usersApprovers = this.approversManager.getAllApprovers(changeRequest, false);

            for (ChangeRequest splittedChangeRequest : result) {
                this.approversManager.setGroupsApprovers(groupsApprovers, splittedChangeRequest);
                this.approversManager.setUsersApprovers(usersApprovers, splittedChangeRequest);
            }
        } else {
            for (ChangeRequest splittedChangeRequest : result) {
                // Each change request contains filechanges for a single document reference, so we can get it like that
                FileChange fileChange = splittedChangeRequest.getLastFileChanges().get(0);
                Set<DocumentReference> groupsApprovers =
                    this.fileChangeApproversManager.getGroupsApprovers(fileChange);
                Set<UserReference> usersApprovers = this.fileChangeApproversManager.getAllApprovers(fileChange, false);
                this.approversManager.setGroupsApprovers(groupsApprovers, splittedChangeRequest);
                this.approversManager.setUsersApprovers(usersApprovers, splittedChangeRequest);
            }
        }
    }

    private String getAllChangeRequestQueryStatement(boolean onlyOpen)
    {
        String statement;
        if (onlyOpen) {
            statement = String.format(", BaseObject as obj , StringProperty as obj_status "
                    + "where obj_status.value in %s and "
                    + "doc.fullName=obj.name and obj.className='%s' "
                    + "and obj_status.id.id=obj.id and obj_status.id.name='%s' ",
                getInOpenStatusesStatement(), this.entityReferenceSerializer.serialize(CHANGE_REQUEST_XCLASS),
                STATUS_FIELD);
        } else {
            statement = String.format(", BaseObject as obj , StringProperty as obj_status, "
                    + "where doc.fullName=obj.name and obj.className='%s' ",
                this.entityReferenceSerializer.serialize(CHANGE_REQUEST_XCLASS));
        }
        return statement;
    }

    @Override
    public long countChangeRequests(boolean onlyOpen) throws ChangeRequestException
    {
        String statement = getAllChangeRequestQueryStatement(onlyOpen);
        try {
            List<Long> result = this.queryManager.createQuery(statement, Query.HQL)
                .addFilter(this.countQueryFilter)
                .execute();
            return result.get(0);
        } catch (QueryException e) {
            throw new ChangeRequestException("Error while counting the change request", e);
        }
    }

    @Override
    public List<DocumentReference> getChangeRequestsReferences(boolean onlyOpen, int offset, int limit)
        throws ChangeRequestException
    {
        String statement = getAllChangeRequestQueryStatement(onlyOpen);
        try {
            List<String> crDocuments = this.queryManager.createQuery(statement, Query.HQL)
                .setOffset(offset)
                .setLimit(limit)
                .execute();
            List<DocumentReference> result = new ArrayList<>();
            for (String crDocument : crDocuments) {
                result.add(this.documentReferenceResolver.resolve(crDocument));
            }
            return result;
        } catch (QueryException e) {
            throw new ChangeRequestException(
                String.format("Error while getting change request [onlyOpen: %s] [offset: %s] [limit: %s]",
                    onlyOpen, offset, limit), e);
        }
    }

    @Override
    public List<ChangeRequest> getChangeRequests(boolean onlyOpen, int offset, int limit)
        throws ChangeRequestException
    {
        List<DocumentReference> changeRequestsReferences = this.getChangeRequestsReferences(onlyOpen, offset, limit);
        List<ChangeRequest> result = new ArrayList<>();

        for (DocumentReference changeRequestsReference : changeRequestsReferences) {
            this.load(changeRequestsReference.getLastSpaceReference().getName()).ifPresent(result::add);
        }
        return result;
    }
}
