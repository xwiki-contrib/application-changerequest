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
     * Check if the given user has autowatch enabled.
     *
     * @param userReference the user for whom to check if they should watch the change request.
     * @return {@code true} if given user has any autowatch value set.
     */
    public boolean hasAutoWatchEnabled(UserReference userReference)
    {
        AutomaticWatchMode automaticWatchMode = this.getAutomaticWatchMode(userReference);
        return automaticWatchMode == AutomaticWatchMode.ALL
            || automaticWatchMode == AutomaticWatchMode.MAJOR
            || automaticWatchMode == AutomaticWatchMode.NEW;
    }

    /**
     * Retrieve the watch mode for the given user.
     * @param userReference the user for whom to retrieve the watch mode.
     * @return the watch mode for the user or {@link AutomaticWatchMode#NONE} if the user is guest.
     * @since 1.10
     */
    public AutomaticWatchMode getAutomaticWatchMode(UserReference userReference)
    {
        AutomaticWatchMode automaticWatchMode = AutomaticWatchMode.NONE;
        if (userReference != GuestUserReference.INSTANCE) {
            DocumentReference userDoc = this.userReferenceSerializer.serialize(userReference);
            automaticWatchMode = this.watchedEntitiesConfiguration.getAutomaticWatchMode(userDoc);
        }

        return automaticWatchMode;
    }

    /**
     * Create and register a new watch entity for the given user to automatically receive notifications on the change
     * request.
     *
     * @param changeRequest the change request for which to create a new watch entity.
     * @param userReference the user who should watch the change request.
     * @throws ChangeRequestException in case of problem when saving the watch entity.
     */
    public void watchChangeRequest(ChangeRequest changeRequest, UserReference userReference)
        throws ChangeRequestException
    {
        DocumentReference changeRequestDoc = this.changeRequestDocumentReferenceResolver.resolve(changeRequest);
        this.watchDocument(changeRequestDoc, userReference);
    }

    /**
     * Ensure that the given user watch the given location.
     *
     * @param documentReference the reference that the user should watch
     * @param userReference the user for whom to add the watch
     * @throws ChangeRequestException in case of problem for watching the document
     * @since 1.10
     */
    public void watchDocument(DocumentReference documentReference, UserReference userReference)
        throws ChangeRequestException
    {
        DocumentReference userDoc = this.userReferenceSerializer.serialize(userReference);
        WatchedLocationReference watchedLocationReference =
            this.watchedEntityFactory.createWatchedLocationReference(documentReference);
        try {
            this.watchedEntitiesManager.watchEntity(watchedLocationReference, userDoc);
        } catch (NotificationException e) {
            throw new ChangeRequestException(
                String.format("Error when trying to automatically watch document [%s]", documentReference), e);
        }
    }
}
