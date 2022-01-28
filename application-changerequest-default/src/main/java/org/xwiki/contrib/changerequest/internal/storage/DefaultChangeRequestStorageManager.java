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
import java.util.Collections;
import java.util.Deque;
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

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestConfiguration;
import org.xwiki.contrib.changerequest.ChangeRequestReview;
import org.xwiki.contrib.changerequest.ChangeRequestRightsManager;
import org.xwiki.contrib.changerequest.ChangeRequestStatus;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.discussions.ChangeRequestDiscussionService;
import org.xwiki.contrib.changerequest.events.ChangeRequestMergedEvent;
import org.xwiki.contrib.changerequest.events.ChangeRequestMergingEvent;
import org.xwiki.contrib.changerequest.events.ChangeRequestStatusChangedEvent;
import org.xwiki.contrib.changerequest.events.SplittedChangeRequestEvent;
import org.xwiki.contrib.changerequest.internal.UserReferenceConverter;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.internal.id.ChangeRequestIDGenerator;
import org.xwiki.contrib.changerequest.storage.ChangeRequestStorageManager;
import org.xwiki.contrib.changerequest.storage.FileChangeStorageManager;
import org.xwiki.contrib.changerequest.storage.ReviewStorageManager;
import org.xwiki.job.JobException;
import org.xwiki.job.JobExecutor;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.refactoring.job.EntityRequest;
import org.xwiki.refactoring.job.RefactoringJobs;
import org.xwiki.refactoring.script.RequestFactory;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.user.UserReferenceSerializer;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.xwiki.contrib.changerequest.internal.storage.ChangeRequestXClassInitializer.CHANGED_DOCUMENTS_FIELD;
import static org.xwiki.contrib.changerequest.internal.storage.ChangeRequestXClassInitializer.CHANGE_REQUEST_XCLASS;
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
    private UserReferenceConverter userReferenceConverter;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private FileChangeStorageManager fileChangeStorageManager;

    @Inject
    private DocumentReferenceResolver<ChangeRequest> changeRequestDocumentReferenceResolver;

    @Inject
    @Named("document")
    private UserReferenceResolver<DocumentReference> userReferenceResolver;

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
    @Named("title")
    private ChangeRequestIDGenerator idGenerator;

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

    @Override
    public void save(ChangeRequest changeRequest) throws ChangeRequestException
    {
        XWikiContext context = this.contextProvider.get();
        XWiki wiki = context.getWiki();
        if (changeRequest.getId() == null) {
            changeRequest.setId(this.idGenerator.generateId(changeRequest));
        }
        DocumentReference reference = this.changeRequestDocumentReferenceResolver.resolve(changeRequest);
        try {
            XWikiDocument document = wiki.getDocument(reference, context);
            document.setTitle(changeRequest.getTitle());
            document.setContent(changeRequest.getDescription());
            document.setContentAuthorReference(this.userReferenceConverter.convert(changeRequest.getCreator()));
            BaseObject xObject = document.getXObject(CHANGE_REQUEST_XCLASS, 0, true, context);
            xObject.set(STATUS_FIELD, changeRequest.getStatus().name().toLowerCase(Locale.ROOT), context);

            List<String> serializedReferences = changeRequest.getModifiedDocuments().stream()
                .map(target -> this.localEntityReferenceSerializer.serialize(target))
                .collect(Collectors.toList());
            xObject.set(CHANGED_DOCUMENTS_FIELD, serializedReferences, context);

            List<String> serializedAuthors = changeRequest.getAuthors().stream()
                .map(target -> this.userReferenceSerializer.serialize(target))
                .collect(Collectors.toList());

            xObject.set("authors", serializedAuthors, context);

            wiki.saveDocument(document, context);
            for (FileChange fileChange : changeRequest.getAllFileChanges()) {
                this.fileChangeStorageManager.save(fileChange);
            }
        } catch (XWikiException e) {
            throw new ChangeRequestException(
                String.format("Error while saving the change request [%s]", changeRequest), e);
        }
    }

    @Override
    public Optional<ChangeRequest> load(String changeRequestId) throws ChangeRequestException
    {
        Optional<ChangeRequest> result = Optional.empty();
        ChangeRequest changeRequest = new ChangeRequest();
        changeRequest.setId(changeRequestId);
        DocumentReference reference = this.changeRequestDocumentReferenceResolver.resolve(changeRequest);
        XWikiContext context = this.contextProvider.get();
        XWiki wiki = context.getWiki();
        try {
            XWikiDocument document = wiki.getDocument(reference, context);
            BaseObject xObject = document.getXObject(CHANGE_REQUEST_XCLASS);
            if (!document.isNew() && xObject != null) {
                ChangeRequestStatus status = ChangeRequestStatus.valueOf(
                    xObject.getStringValue(STATUS_FIELD).toUpperCase());
                changeRequest
                    .setTitle(document.getTitle())
                    .setDescription(document.getContent())
                    .setCreator(this.userReferenceResolver.resolve(document.getContentAuthorReference()))
                    .setStatus(status)
                    .setCreationDate(document.getCreationDate());
                List<String> changedDocuments = xObject.getListValue(CHANGED_DOCUMENTS_FIELD);

                for (String changedDocument : changedDocuments) {
                    DocumentReference changedDocumentReference =
                        this.documentReferenceResolver.resolve(changedDocument);
                    List<FileChange> fileChanges =
                        this.fileChangeStorageManager.load(changeRequest, changedDocumentReference);
                    for (FileChange fileChange : fileChanges) {
                        changeRequest.addFileChange(fileChange);
                    }
                }

                this.reviewStorageManager.load(changeRequest);
                result = Optional.of(changeRequest);
            }
        } catch (XWikiException e) {
            throw new ChangeRequestException(
                String.format("Error while trying to load change request of id [%s]", changeRequestId), e);
        }
        return result;
    }

    @Override
    public void merge(ChangeRequest changeRequest) throws ChangeRequestException
    {
        this.observationManager.notify(new ChangeRequestMergingEvent(), changeRequest.getId(), changeRequest);
        Set<DocumentReference> documentReferences = changeRequest.getFileChanges().keySet();
        for (DocumentReference documentReference : documentReferences) {
            Optional<FileChange> optionalFileChange = changeRequest.getLatestFileChangeFor(documentReference);
            if (optionalFileChange.isPresent()) {
                this.fileChangeStorageManager.merge(optionalFileChange.get());
            }
        }

        ChangeRequestStatus oldStatus = changeRequest.getStatus();
        changeRequest.setStatus(ChangeRequestStatus.MERGED);
        this.observationManager.notify(new ChangeRequestStatusChangedEvent(), changeRequest.getId(),
            new ChangeRequestStatus[] {oldStatus, ChangeRequestStatus.MERGED});
        this.save(changeRequest);
        this.observationManager.notify(new ChangeRequestMergedEvent(), changeRequest.getId(), changeRequest);
    }

    @Override
    public List<DocumentReference> getChangeRequestMatchingName(String title) throws ChangeRequestException
    {
        String statement = String.format("from doc.object(%s) as cr where doc.fullName like :reference",
            this.entityReferenceSerializer.serialize(CHANGE_REQUEST_XCLASS));
        SpaceReference changeRequestSpaceLocation = this.configuration.getChangeRequestSpaceLocation();
        try {
            Query query = this.queryManager.createQuery(statement, Query.XWQL);
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
        String statement = String.format("from doc.object(%s) as obj where :reference member of obj.%s",
            this.entityReferenceSerializer.serialize(CHANGE_REQUEST_XCLASS), CHANGED_DOCUMENTS_FIELD);
        try {
            Query query = this.queryManager.createQuery(statement, Query.XWQL);
            query.bindValue(REFERENCE, this.localEntityReferenceSerializer.serialize(documentReference));
            List<String> changeRequestDocuments = query.execute();
            for (String changeRequestDocument : changeRequestDocuments) {
                DocumentReference crReference = this.documentReferenceResolver.resolve(changeRequestDocument);
                this.load(crReference.getLastSpaceReference().getName()).ifPresent(result::add);
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

    @Override
    public List<ChangeRequest> split(ChangeRequest changeRequest) throws ChangeRequestException
    {
        List<ChangeRequest> result = new ArrayList<>();
        Map<DocumentReference, Deque<FileChange>> fileChanges = changeRequest.getFileChanges();

        for (Map.Entry<DocumentReference, Deque<FileChange>> entry : fileChanges.entrySet()) {
            ChangeRequest splittedChangeRequest = changeRequest.cloneWithoutFileChanges();

            for (FileChange fileChange : entry.getValue()) {
                FileChange clonedFileChange = fileChange
                    .cloneWithChangeRequestAndType(splittedChangeRequest, fileChange.getType());
                splittedChangeRequest.addFileChange(clonedFileChange);
            }

            this.save(splittedChangeRequest);
            for (ChangeRequestReview review : changeRequest.getReviews()) {
                ChangeRequestReview clonedReview = review.cloneWithChangeRequest(splittedChangeRequest);

                // we consider reviews as outdated for splitted change requests
                // and we keep same id to avoid having to perform a mapping old/new reviews
                clonedReview
                    .setValid(false)
                    .setId(review.getId());

                splittedChangeRequest.addReview(review);
                this.reviewStorageManager.save(clonedReview);
            }
            this.changeRequestRightsManager.copyAllButViewRights(changeRequest, splittedChangeRequest);
            this.changeRequestRightsManager.copyViewRights(splittedChangeRequest, entry.getKey());
            result.add(splittedChangeRequest);
        }

        this.discussionService.moveDiscussions(changeRequest, result);

        DocumentReference changeRequestDocument = this.changeRequestDocumentReferenceResolver.resolve(changeRequest);
        EntityRequest deleteRequest =
            this.refactoringRequestFactory.createDeleteRequest(Collections.singletonList(changeRequestDocument));
        deleteRequest.setDeep(true);
        deleteRequest.setCheckAuthorRights(false);
        deleteRequest.setCheckRights(false);
        try {
            this.jobExecutor.execute(RefactoringJobs.DELETE, deleteRequest);
        } catch (JobException e) {
            throw new ChangeRequestException(
                String.format("Error while performing deletion of change request document [%s]", changeRequestDocument),
                e);
        }
        // TODO: put placeholder with redirect

        this.observationManager.notify(new SplittedChangeRequestEvent(), changeRequest.getId(), result);

        return result;
    }
}
