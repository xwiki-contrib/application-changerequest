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
package org.xwiki.contrib.changerequest.internal.remote;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestReview;
import org.xwiki.contrib.changerequest.ChangeRequestStatus;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.events.ApproversUpdatedEvent;
import org.xwiki.contrib.changerequest.events.ChangeRequestCreatedEvent;
import org.xwiki.contrib.changerequest.events.ChangeRequestFileChangeAddedEvent;
import org.xwiki.contrib.changerequest.events.ChangeRequestMergeFailedEvent;
import org.xwiki.contrib.changerequest.events.ChangeRequestMergedEvent;
import org.xwiki.contrib.changerequest.events.ChangeRequestMergingEvent;
import org.xwiki.contrib.changerequest.events.ChangeRequestRebasedEvent;
import org.xwiki.contrib.changerequest.events.ChangeRequestReviewAddedEvent;
import org.xwiki.contrib.changerequest.events.ChangeRequestStatusChangedEvent;
import org.xwiki.contrib.changerequest.events.ChangeRequestUpdatedEvent;
import org.xwiki.contrib.changerequest.events.ChangeRequestUpdatedFileChangeEvent;
import org.xwiki.contrib.changerequest.events.ChangeRequestUpdatingFileChangeEvent;
import org.xwiki.contrib.changerequest.events.FileChangeDocumentSavedEvent;
import org.xwiki.contrib.changerequest.events.FileChangeDocumentSavingEvent;
import org.xwiki.contrib.changerequest.events.FileChangeRebasedEvent;
import org.xwiki.contrib.changerequest.events.SplitBeginChangeRequestEvent;
import org.xwiki.contrib.changerequest.events.SplitEndChangeRequestEvent;
import org.xwiki.contrib.changerequest.events.StaleChangeRequestEvent;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.remote.LocalEventData;
import org.xwiki.observation.remote.RemoteEventData;
import org.xwiki.observation.remote.converter.AbstractEventConverter;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Component in charge of performing conversion of events for cluster support.
 *
 * @version $Id$
 * @since 1.5
 */
@Component
@Singleton
@Named("changerequest")
public class ChangeRequestEventsConverter extends AbstractEventConverter
{
    // List of events where the source is the change request identifier, and the data the corresponding change request.
    private static final List<Class<?>> CR_DATA_EVENTS = List.of(
        ChangeRequestCreatedEvent.class,
        ChangeRequestMergedEvent.class,
        ChangeRequestMergeFailedEvent.class,
        ChangeRequestRebasedEvent.class,
        ChangeRequestUpdatedEvent.class,
        StaleChangeRequestEvent.class
    );

    // List of events where the source is the change request identifier, and the data a file change.
    private static final List<Class<?>> FILE_CHANGE_DATA_EVENTS = List.of(
        ChangeRequestFileChangeAddedEvent.class,
        FileChangeRebasedEvent.class
    );

    // List of events for which we don't convert the data (those are BeginEvent we need to comply with their respective
    // EndEvent).
    private static final List<Class<?>> NO_DATA_EVENTS = List.of(
        ChangeRequestMergingEvent.class,
        ChangeRequestUpdatingFileChangeEvent.class,
        SplitBeginChangeRequestEvent.class
    );

    @Inject
    private Provider<ChangeRequestEventsConverterHelper> changeRequestEventsConverterHelperProvider;

    @Inject
    private Provider<XWikiDocumentEventConverterSerializer> xWikiDocumentEventConverterSerializerProvider;

    @Inject
    private Logger logger;

    @Override
    public boolean toRemote(LocalEventData localEvent, RemoteEventData remoteEvent)
    {
        boolean result = false;
        if (CR_DATA_EVENTS.contains(localEvent.getEvent().getClass())
             || NO_DATA_EVENTS.contains(localEvent.getEvent().getClass())) {
            this.copyEventAndSource(localEvent, remoteEvent);
            result = true;
        } else if (FILE_CHANGE_DATA_EVENTS.contains(localEvent.getEvent().getClass())) {
            this.copyEventAndSource(localEvent, remoteEvent);
            // the data is a filechange we set the data as the identifier.
            remoteEvent.setData(((FileChange) localEvent.getData()).getId());
            result = true;
        } else {
            result = this.toRemoteSpecificEvents(localEvent, remoteEvent);
        }
        return result;
    }

    private boolean toRemoteSpecificEvents(LocalEventData localEvent, RemoteEventData remoteEvent)
    {
        boolean result = false;
        if (localEvent.getEvent() instanceof ApproversUpdatedEvent) {
            // fill the remote event
            remoteEvent.setEvent((Serializable) localEvent.getEvent());
            // the source is an XWikiDocument
            remoteEvent.setSource(this.xWikiDocumentEventConverterSerializerProvider.get()
                .serializeXWikiDocument((XWikiDocument) localEvent.getSource()));
            // the data is already serializable
            remoteEvent.setData((Serializable) localEvent.getData());
            result = true;
        } else if (localEvent.getEvent() instanceof ChangeRequestUpdatedFileChangeEvent) {
            result = this.toRemoteUpdatedFileChangeEvent(localEvent, remoteEvent);
        } else if (localEvent.getEvent() instanceof ChangeRequestReviewAddedEvent) {
            this.copyEventAndSource(localEvent, remoteEvent);
            ChangeRequestReview review = (ChangeRequestReview) localEvent.getData();
            // We set the identifier of the review as data of the remote event
            remoteEvent.setData(review.getId());
            result = true;
        } else if (localEvent.getEvent() instanceof ChangeRequestStatusChangedEvent) {
            result = this.toRemoteChangeRequestStatusChangedEvent(localEvent, remoteEvent);
        } else if (localEvent.getEvent() instanceof FileChangeDocumentSavedEvent) {
            result = this.toRemoteFileChangeDocumentSavedEvent(localEvent, remoteEvent);
        } else if (localEvent.getEvent() instanceof SplitEndChangeRequestEvent) {
            this.copyEventAndSource(localEvent, remoteEvent);
            List<ChangeRequest> data = (List<ChangeRequest>) localEvent.getData();
            remoteEvent.setData(new ArrayList<>(data.stream().map(ChangeRequest::getId).collect(Collectors.toList())));
            result = true;
        } else if (localEvent.getEvent() instanceof FileChangeDocumentSavingEvent) {
            remoteEvent.setEvent((FileChangeDocumentSavingEvent) localEvent.getEvent());
            result = true;
        }
        return result;
    }

    private boolean toRemoteChangeRequestStatusChangedEvent(LocalEventData localEvent, RemoteEventData remoteEvent)
    {
        this.copyEventAndSource(localEvent, remoteEvent);
        ChangeRequestStatus[] statuses = (ChangeRequestStatus[]) localEvent.getData();
        List<String> data = Arrays.stream(statuses).map(ChangeRequestStatus::name).collect(Collectors.toList());
        remoteEvent.setData(new ArrayList<>(data));
        return true;
    }

    private boolean toRemoteUpdatedFileChangeEvent(LocalEventData localEvent, RemoteEventData remoteEvent)
    {
        this.copyEventAndSource(localEvent, remoteEvent);
        // if the data is a filechange we set the data as the identifier.
        // if it's not a filechange, then it's a full change request, and we'll retrieve it from the source.
        if (localEvent.getData() instanceof FileChange) {
            remoteEvent.setData(((FileChange) localEvent.getData()).getId());
        }
        return true;
    }

    private boolean toRemoteFileChangeDocumentSavedEvent(LocalEventData localEvent, RemoteEventData remoteEvent)
    {
        // fill the remote event
        remoteEvent.setEvent((Serializable) localEvent.getEvent());
        // The source is the filechange, we need both the change request id and the filechange id to retrieve it
        FileChange fileChange = (FileChange) localEvent.getSource();
        remoteEvent.setSource(Pair.of(fileChange.getChangeRequest().getId(), fileChange.getId()));
        // the data is an XWikiDocument
        remoteEvent.setData(this.xWikiDocumentEventConverterSerializerProvider.get()
            .serializeXWikiDocument((XWikiDocument) localEvent.getData()));
        return true;
    }

    private void copyEventAndSource(LocalEventData localEvent, RemoteEventData remoteEvent)
    {
        remoteEvent.setEvent((Serializable) localEvent.getEvent());
        remoteEvent.setSource((Serializable) localEvent.getSource());
    }

    @Override
    public boolean fromRemote(RemoteEventData remoteEvent, LocalEventData localEvent)
    {
        boolean result = false;
        try {
            result = this.handleFromRemoteEvent(remoteEvent, localEvent);
        } catch (ChangeRequestEventsConverterException e) {
            this.logger.error(e.getMessage());
            if (e.getCause() != null) {
                this.logger.debug("Full root cause: ", e);
            }
            // Ensure to not send the event in case of error.
            localEvent.setEvent(null);
        } catch (XWikiException e) {
            this.logger.error("Error while trying to unserialize document from remote event [{}]: [{}]",
                remoteEvent, ExceptionUtils.getRootCauseMessage(e));
            this.logger.debug("Full stack trace of the error to unserialize document: ", e);
            // Ensure to not send the event in case of error.
            localEvent.setEvent(null);
        }
        return result;
    }

    private boolean handleFromRemoteEvent(RemoteEventData remoteEvent, LocalEventData localEvent)
        throws XWikiException, ChangeRequestEventsConverterException
    {
        boolean result = false;
        if (CR_DATA_EVENTS.contains(remoteEvent.getEvent().getClass())) {
            localEvent.setEvent((Event) remoteEvent.getEvent());
            String changeRequestId = (String) remoteEvent.getSource();
            localEvent.setSource(changeRequestId);
            localEvent.setData(this.changeRequestEventsConverterHelperProvider.get()
                .getChangeRequest(changeRequestId, remoteEvent));
            result = true;
        } else if (FILE_CHANGE_DATA_EVENTS.contains(remoteEvent.getEvent().getClass())) {
            localEvent.setEvent((Event) remoteEvent.getEvent());
            String changeRequestId = (String) remoteEvent.getSource();
            localEvent.setSource(changeRequestId);
            localEvent.setData(this.changeRequestEventsConverterHelperProvider.get()
                .getFileChange(changeRequestId, remoteEvent.getData(), remoteEvent));
            result = true;
        } else if (NO_DATA_EVENTS.contains(remoteEvent.getEvent().getClass())) {
            localEvent.setEvent((Event) remoteEvent.getEvent());
            String changeRequestId = (String) remoteEvent.getSource();
            localEvent.setSource(changeRequestId);
            result = true;
        } else {
            result = this.handleFromRemoteSpecificEvents(remoteEvent, localEvent);
        }
        return result;
    }

    private boolean handleFromRemoteSpecificEvents(RemoteEventData remoteEvent, LocalEventData localEvent)
        throws ChangeRequestEventsConverterException, XWikiException
    {
        boolean result = false;
        if (remoteEvent.getEvent() instanceof ApproversUpdatedEvent) {
            result = this.fromRemoteApproversUpdatedEvent(remoteEvent, localEvent);
        } else if (remoteEvent.getEvent() instanceof ChangeRequestUpdatedFileChangeEvent) {
            result = this.fromRemoteFileChangeUpdatedEvent(remoteEvent, localEvent);
        } else if (remoteEvent.getEvent() instanceof ChangeRequestReviewAddedEvent) {
            result = this.fromRemoteReviewAddedEvent(remoteEvent, localEvent);
        } else if (remoteEvent.getEvent() instanceof ChangeRequestStatusChangedEvent) {
            result = this.fromRemoteStatusChangedEvent(remoteEvent, localEvent);
        } else if (remoteEvent.getEvent() instanceof FileChangeDocumentSavedEvent) {
            result = this.fromRemoteFileChangeDocumentSavedEvent(remoteEvent, localEvent);
        } else if (remoteEvent.getEvent() instanceof SplitEndChangeRequestEvent) {
            result = this.fromRemoteSplitEndChangeRequestEvent(remoteEvent, localEvent);
        } else if (remoteEvent.getEvent() instanceof FileChangeDocumentSavingEvent) {
            localEvent.setEvent((FileChangeDocumentSavingEvent) remoteEvent.getEvent());
            result = true;
        }
        return result;
    }

    private boolean fromRemoteApproversUpdatedEvent(RemoteEventData remoteEvent, LocalEventData localEvent)
        throws XWikiException
    {
        boolean result = false;
        localEvent.setEvent((ApproversUpdatedEvent) remoteEvent.getEvent());
        localEvent.setData(remoteEvent.getData());
        localEvent.setSource(this.xWikiDocumentEventConverterSerializerProvider.get()
            .unserializeDocument(remoteEvent.getSource()));
        result = true;
        return result;
    }

    private boolean fromRemoteSplitEndChangeRequestEvent(RemoteEventData remoteEvent, LocalEventData localEvent)
        throws ChangeRequestEventsConverterException
    {
        localEvent.setEvent((SplitEndChangeRequestEvent) remoteEvent.getEvent());
        localEvent.setSource(remoteEvent.getSource());
        List<ChangeRequest> data = new ArrayList<>();
        List<String> remoteData = (List<String>) remoteEvent.getData();
        for (String changeRequestId : remoteData) {
            data.add(this.changeRequestEventsConverterHelperProvider.get()
                .getChangeRequest(changeRequestId, remoteEvent));
        }
        localEvent.setData(data);
        return true;
    }

    private boolean fromRemoteFileChangeDocumentSavedEvent(RemoteEventData remoteEvent, LocalEventData localEvent)
        throws ChangeRequestEventsConverterException, XWikiException
    {
        localEvent.setEvent((FileChangeDocumentSavedEvent) remoteEvent.getEvent());
        Pair<String, String> source = (Pair<String, String>) remoteEvent.getSource();
        String changeRequestId = source.getLeft();
        String fileChangeId = source.getRight();
        FileChange fileChange = this.changeRequestEventsConverterHelperProvider.get()
            .getFileChange(changeRequestId, fileChangeId, remoteEvent);
        localEvent.setSource(fileChange);
        localEvent.setData(this.xWikiDocumentEventConverterSerializerProvider.get()
            .unserializeDocument(remoteEvent.getData()));
        return true;
    }

    private boolean fromRemoteReviewAddedEvent(RemoteEventData remoteEvent, LocalEventData localEvent)
        throws ChangeRequestEventsConverterException
    {
        localEvent.setEvent((ChangeRequestReviewAddedEvent) remoteEvent.getEvent());

        String changeRequestId = (String) remoteEvent.getSource();
        localEvent.setSource(changeRequestId);

        ChangeRequest changeRequest = this.changeRequestEventsConverterHelperProvider.get()
            .getChangeRequest(changeRequestId, remoteEvent);
        String reviewId = (String) remoteEvent.getData();
        Optional<ChangeRequestReview> reviewOptional = changeRequest.getReviews().stream()
            .filter(changeRequestReview -> changeRequestReview.getId().equals(reviewId))
            .findFirst();
        if (reviewOptional.isPresent()) {
            localEvent.setData(reviewOptional.get());
            return true;
        } else {
            throw new ChangeRequestEventsConverterException(
                String.format("Cannot find review with id [%s] in the change request reviews to properly convert"
                    + " remote event [%s]", reviewId, remoteEvent));
        }
    }

    private boolean fromRemoteFileChangeUpdatedEvent(RemoteEventData remoteEvent, LocalEventData localEvent)
        throws ChangeRequestEventsConverterException
    {
        localEvent.setEvent((ChangeRequestUpdatedFileChangeEvent) remoteEvent.getEvent());

        String changeRequestId = (String) remoteEvent.getSource();
        localEvent.setSource(changeRequestId);
        if (remoteEvent.getData() != null) {
            localEvent.setData(this.changeRequestEventsConverterHelperProvider.get()
                .getFileChange(changeRequestId, remoteEvent.getData(), remoteEvent));
        } else {
            localEvent.setData(this.changeRequestEventsConverterHelperProvider.get()
                .getChangeRequest(changeRequestId, remoteEvent));
        }
        return true;
    }

    private boolean fromRemoteStatusChangedEvent(RemoteEventData remoteEvent, LocalEventData localEvent)
    {
        localEvent.setEvent((ChangeRequestStatusChangedEvent) remoteEvent.getEvent());

        String changeRequestId = (String) remoteEvent.getSource();
        localEvent.setSource(changeRequestId);

        List<String> statuses = (List<String>) remoteEvent.getData();
        ChangeRequestStatus[] data = statuses.stream()
            .map(ChangeRequestStatus::valueOf)
            .toArray(ChangeRequestStatus[]::new);
        localEvent.setData(data);
        return true;
    }
}
