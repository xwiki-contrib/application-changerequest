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
package org.xwiki.contrib.changerequest.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.filters.watch.AutomaticWatchMode;
import org.xwiki.notifications.filters.watch.WatchedEntitiesConfiguration;
import org.xwiki.notifications.filters.watch.WatchedEntitiesManager;
import org.xwiki.notifications.filters.watch.WatchedEntityFactory;
import org.xwiki.notifications.filters.watch.WatchedLocationReference;
import org.xwiki.user.GuestUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceSerializer;

/**
 * This components aims at handling the creation of a watched entity for newly created change request.
 * This is basically the same feature as automatically watching newly created documents, but applied on change request
 * docs.
 * See it used in {@link org.xwiki.contrib.changerequest.internal.listeners.ChangeRequestCreatedEventListener}.
 *
 * @version $Id$
 * @since 0.13
 */
@Component(roles = ChangeRequestAutoWatchHandler.class)
@Singleton
public class ChangeRequestAutoWatchHandler
{
    @Inject
    private WatchedEntitiesConfiguration watchedEntitiesConfiguration;

    @Inject
    private WatchedEntityFactory watchedEntityFactory;

    @Inject
    private WatchedEntitiesManager watchedEntitiesManager;

    @Inject
    private DocumentReferenceResolver<ChangeRequest> changeRequestDocumentReferenceResolver;

    @Inject
    @Named("document")
    private UserReferenceSerializer<DocumentReference> userReferenceSerializer;

    /**
     * Check if the given change request should be automatically watched by the given user.
     *
     * @param changeRequest the change request for which to watch for changes.
     * @param userReference the user for whom to check if they should watch the change request.
     * @return {@code true} if the creator of the change request has any autowatch value set.
     */
    public boolean shouldCreateWatchedEntity(ChangeRequest changeRequest, UserReference userReference)
    {
        boolean result = false;
        if (userReference != GuestUserReference.INSTANCE) {
            DocumentReference userDoc = this.userReferenceSerializer.serialize(userReference);
            AutomaticWatchMode automaticWatchMode = this.watchedEntitiesConfiguration.getAutomaticWatchMode(userDoc);
            result = automaticWatchMode == AutomaticWatchMode.ALL
                || automaticWatchMode == AutomaticWatchMode.MAJOR
                || automaticWatchMode == AutomaticWatchMode.NEW;
        }
        return result;
    }

    /**
     * Create and register a new watch entity for the creator to automatically receive notifications on the change
     * request.
     *
     * @param changeRequest the change request for which to create a new watch entity.
     * @param userReference the user who should watch the change request.
     * @throws ChangeRequestException in case of problem when saving the watch entity.
     */
    public void watchChangeRequest(ChangeRequest changeRequest, UserReference userReference)
        throws ChangeRequestException
    {
        DocumentReference userDoc = this.userReferenceSerializer.serialize(userReference);
        DocumentReference changeRequestDoc = this.changeRequestDocumentReferenceResolver.resolve(changeRequest);
        WatchedLocationReference watchedLocationReference =
            this.watchedEntityFactory.createWatchedLocationReference(changeRequestDoc);
        try {
            this.watchedEntitiesManager.watchEntity(watchedLocationReference, userDoc);
        } catch (NotificationException e) {
            throw new ChangeRequestException(
                String.format("Error when trying to automatically watch newly created change request [%s]",
                    changeRequest.getId()), e);
        }
    }
}
