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

import javax.inject.Named;

import org.junit.jupiter.api.Test;
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
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceSerializer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ChangeRequestAutoWatchHandler}.
 *
 * @version $Id$
 * @since 0.13
 */
@ComponentTest
class ChangeRequestAutoWatchHandlerTest
{
    @InjectMockComponents
    private ChangeRequestAutoWatchHandler autoWatchHandler;

    @MockComponent
    private WatchedEntitiesConfiguration watchedEntitiesConfiguration;

    @MockComponent
    private WatchedEntityFactory watchedEntityFactory;

    @MockComponent
    private WatchedEntitiesManager watchedEntitiesManager;

    @MockComponent
    private DocumentReferenceResolver<ChangeRequest> changeRequestDocumentReferenceResolver;

    @MockComponent
    @Named("document")
    private UserReferenceSerializer<DocumentReference> userReferenceSerializer;

    @Test
    void shouldCreateWatchedEntity()
    {
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        UserReference creator = mock(UserReference.class);
        when(changeRequest.getCreator()).thenReturn(creator);

        DocumentReference userDoc = mock(DocumentReference.class);
        when(this.userReferenceSerializer.serialize(creator)).thenReturn(userDoc);

        when(this.watchedEntitiesConfiguration.getAutomaticWatchMode(userDoc)).thenReturn(null);
        assertFalse(this.autoWatchHandler.shouldCreateWatchedEntity(changeRequest));

        when(this.watchedEntitiesConfiguration.getAutomaticWatchMode(userDoc)).thenReturn(AutomaticWatchMode.ALL);
        assertTrue(this.autoWatchHandler.shouldCreateWatchedEntity(changeRequest));

        when(this.watchedEntitiesConfiguration.getAutomaticWatchMode(userDoc)).thenReturn(AutomaticWatchMode.MAJOR);
        assertTrue(this.autoWatchHandler.shouldCreateWatchedEntity(changeRequest));

        when(this.watchedEntitiesConfiguration.getAutomaticWatchMode(userDoc)).thenReturn(AutomaticWatchMode.NONE);
        assertFalse(this.autoWatchHandler.shouldCreateWatchedEntity(changeRequest));

        when(this.watchedEntitiesConfiguration.getAutomaticWatchMode(userDoc)).thenReturn(AutomaticWatchMode.NEW);
        assertTrue(this.autoWatchHandler.shouldCreateWatchedEntity(changeRequest));
    }

    @Test
    void watchChangeRequest() throws ChangeRequestException, NotificationException
    {
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        UserReference creator = mock(UserReference.class);
        when(changeRequest.getCreator()).thenReturn(creator);

        DocumentReference userDoc = mock(DocumentReference.class);
        when(this.userReferenceSerializer.serialize(creator)).thenReturn(userDoc);

        DocumentReference changeRequestDoc = mock(DocumentReference.class);
        when(this.changeRequestDocumentReferenceResolver.resolve(changeRequest)).thenReturn(changeRequestDoc);

        WatchedLocationReference watchedLocationReference = mock(WatchedLocationReference.class);
        when(this.watchedEntityFactory.createWatchedLocationReference(changeRequestDoc))
            .thenReturn(watchedLocationReference);

        this.autoWatchHandler.watchChangeRequest(changeRequest);
        verify(this.watchedEntitiesManager).watchEntity(watchedLocationReference, userDoc);
    }
}
