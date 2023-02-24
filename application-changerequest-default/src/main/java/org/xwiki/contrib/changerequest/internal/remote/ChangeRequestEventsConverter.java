package org.xwiki.contrib.changerequest.internal.remote;

import java.io.Serializable;
import java.util.Map;
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
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.events.ApproversUpdatedEvent;
import org.xwiki.contrib.changerequest.events.ChangeRequestCreatedEvent;
import org.xwiki.contrib.changerequest.events.ChangeRequestFileChangeAddedEvent;
import org.xwiki.contrib.changerequest.events.ChangeRequestUpdatedFileChangeEvent;
import org.xwiki.contrib.changerequest.internal.cache.ChangeRequestStorageCacheManager;
import org.xwiki.contrib.changerequest.storage.ChangeRequestStorageManager;
import org.xwiki.observation.remote.LocalEventData;
import org.xwiki.observation.remote.RemoteEventData;
import org.xwiki.observation.remote.converter.AbstractEventConverter;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

@Component
@Singleton
@Named("changerequest")
public class ChangeRequestEventsConverter extends AbstractEventConverter
{
    @Inject
    private Provider<ChangeRequestStorageCacheManager> changeRequestCacheManager;

    @Inject
    private Provider<ChangeRequestStorageManager> changeRequestStorageManagerProvider;

    @Inject
    private Provider<XWikiDocumentEventConverterSerializer> xWikiDocumentEventConverterSerializerProvider;

    @Inject
    private Logger logger;

    @Override
    public boolean toRemote(LocalEventData localEvent, RemoteEventData remoteEvent)
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
        } else if (localEvent.getEvent() instanceof ChangeRequestCreatedEvent) {
            // fill the remote event
            remoteEvent.setEvent((Serializable) localEvent.getEvent());
            // The source is a string so it's serializable
            remoteEvent.setSource((Serializable) localEvent.getSource());
            // We don't set any data
            result = true;
        } else if (localEvent.getEvent() instanceof ChangeRequestFileChangeAddedEvent) {
            // fill the remote event
            remoteEvent.setEvent((Serializable) localEvent.getEvent());
            // The source is a string so it's serializable
            remoteEvent.setSource((Serializable) localEvent.getSource());
            // the data is a filechange we set the data as the identifier.
            remoteEvent.setData(((FileChange) localEvent.getData()).getId());
            result = true;
        } else if (localEvent.getEvent() instanceof ChangeRequestUpdatedFileChangeEvent) {
            // fill the remote event
            remoteEvent.setEvent((Serializable) localEvent.getEvent());
            // The source is a string so it's serializable
            remoteEvent.setSource((Serializable) localEvent.getSource());
            // if the data is a filechange we set the data as the identifier.
            // if it's not a filechange, then it's a full change request, and we'll retrieve it from the source.
            if (localEvent.getData() instanceof FileChange) {
                remoteEvent.setData(((FileChange) localEvent.getData()).getId());
            }
            result = true;
        }

        // FIXME: Handle:
        // FileChangeRebasedEvent
        // ChangeRequestUpdatingFileChangeEvent

        return result;
    }

    @Override
    public boolean fromRemote(RemoteEventData remoteEvent, LocalEventData localEvent)
    {
        boolean result = false;
        if (remoteEvent.getEvent() instanceof ApproversUpdatedEvent) {
            localEvent.setEvent((ApproversUpdatedEvent) remoteEvent.getEvent());
            try {
                localEvent.setSource(this.xWikiDocumentEventConverterSerializerProvider.get()
                    .unserializeDocument(remoteEvent.getSource()));
            } catch (XWikiException e) {
                this.logger.error("Error while trying to unserialize document from remote event [{}]: [{}]",
                    remoteEvent, ExceptionUtils.getRootCauseMessage(e));
                this.logger.debug("Full stack trace of the error to unserialize document: ", e);
            }
            localEvent.setData(remoteEvent.getData());
            result = true;
        } else if (remoteEvent.getEvent() instanceof ChangeRequestCreatedEvent) {
            localEvent.setEvent((ChangeRequestCreatedEvent) remoteEvent.getEvent());
            String changeRequestId = (String) remoteEvent.getSource();
            localEvent.setSource(changeRequestId);
            localEvent.setData(getDataFromRemote(changeRequestId, null, remoteEvent, false));
            result = true;
        } else if (remoteEvent.getEvent() instanceof ChangeRequestFileChangeAddedEvent) {
            localEvent.setEvent((ChangeRequestFileChangeAddedEvent) remoteEvent.getEvent());
            String changeRequestId = (String) remoteEvent.getSource();
            localEvent.setSource(changeRequestId);
            localEvent.setData(getDataFromRemote(changeRequestId, remoteEvent.getData(), remoteEvent, false));
            result = true;
        } else if (remoteEvent.getEvent() instanceof ChangeRequestUpdatedFileChangeEvent) {
            localEvent.setEvent((ChangeRequestUpdatedFileChangeEvent) remoteEvent.getEvent());

            String changeRequestId = (String) remoteEvent.getSource();
            localEvent.setSource(changeRequestId);
            localEvent.setData(getDataFromRemote(changeRequestId, remoteEvent.getData(), remoteEvent, true));
            result = true;
        }
        return result;
    }

    private Object getDataFromRemote(String changeRequestId, Serializable fileChangeId, RemoteEventData remoteEvent,
        boolean fallback)
    {
        Object result = null;
        // We'll need to reload the change request from DB so first invalidate the cache value for it.
        this.changeRequestCacheManager.get().invalidate(changeRequestId);

        try {
            Optional<ChangeRequest> optionalChangeRequest =
                this.changeRequestStorageManagerProvider.get().load(changeRequestId);
            if (optionalChangeRequest.isPresent()) {
                if (fileChangeId != null) {
                    Optional<FileChange> optionalFileChange =
                        optionalChangeRequest.get().getFileChangeById((String)fileChangeId);
                    if (optionalFileChange.isPresent()) {
                        result = optionalFileChange.get();
                    } else if (fallback) {
                        this.logger.error("Cannot find file change [{}] from change request [{}] to convert event"
                                + " [{}]. Falling back on the change request as data of the event.", fileChangeId,
                            changeRequestId, remoteEvent);
                        result = optionalChangeRequest.get();
                    } else {
                        this.logger.error("Cannot find file change [{}] from change request [{}] to convert event"
                                + " [{}].", fileChangeId,
                            changeRequestId, remoteEvent);
                    }
                } else {
                    result = optionalChangeRequest.get();
                }
            } else {
                this.logger.error("Cannot find change request [{}] to convert event [{}]",
                    changeRequestId, remoteEvent);
            }
        } catch (ChangeRequestException e) {
            this.logger.error("Error when loading change request [{}] to convert event [{}]: [{}]",
                changeRequestId, remoteEvent, ExceptionUtils.getRootCauseMessage(e));
            this.logger.debug("Full stack trace of the loading error:", e);
        }
        return result;
    }
}
