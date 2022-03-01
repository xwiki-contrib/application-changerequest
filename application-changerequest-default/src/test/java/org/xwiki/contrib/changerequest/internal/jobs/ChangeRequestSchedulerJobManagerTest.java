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
package org.xwiki.contrib.changerequest.internal.jobs;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;

import javax.inject.Inject;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestConfiguration;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.ChangeRequestManager;
import org.xwiki.contrib.changerequest.ChangeRequestStatus;
import org.xwiki.contrib.changerequest.events.StaleChangeRequestEvent;
import org.xwiki.contrib.changerequest.internal.UserReferenceConverter;
import org.xwiki.contrib.changerequest.storage.ChangeRequestStorageManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;

import com.xpn.xwiki.XWikiContext;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ChangeRequestSchedulerJobManager}.
 *
 * @version $Id$
 * @since 0.10
 */
@ComponentTest
class ChangeRequestSchedulerJobManagerTest
{
    @InjectMockComponents
    private ChangeRequestSchedulerJobManager schedulerJobManager;

    @MockComponent
    private ChangeRequestConfiguration configuration;

    @MockComponent
    private ChangeRequestStorageManager changeRequestStorageManager;

    @MockComponent
    private ChangeRequestManager changeRequestManager;

    @MockComponent
    private ObservationManager observationManager;

    @MockComponent
    private UserReferenceConverter userReferenceConverter;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    private XWikiContext context;

    @BeforeEach
    void setup()
    {
        this.context = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(this.context);
        when(configuration.getDurationUnit()).thenReturn(ChronoUnit.DAYS);
    }

    @Test
    void notifyStaleChangeRequests() throws ChangeRequestException
    {
        when(this.configuration.getStaleChangeRequestDurationForNotifying()).thenReturn(0L);
        this.schedulerJobManager.notifyStaleChangeRequests();

        // when duration is set to 0 the feature is entirely disabled
        verify(this.changeRequestStorageManager, never()).findOpenChangeRequestsByDate(any(), anyBoolean());

        when(this.configuration.getStaleChangeRequestDurationForNotifying()).thenReturn(2L);
        when(this.configuration.useCreationDateForStaleDurations()).thenReturn(true);
        UserReference requestedSchedulerUser = mock(UserReference.class);
        when(this.configuration.getSchedulerContextUser()).thenReturn(requestedSchedulerUser);
        DocumentReference userDocReference = mock(DocumentReference.class);
        when(this.userReferenceConverter.convert(requestedSchedulerUser)).thenReturn(userDocReference);

        Date now = new Date();
        Instant beforeExpectedDate = now.toInstant().minus(2L, ChronoUnit.DAYS);
        Instant afterExpectedDate = now.toInstant().minus(1L, ChronoUnit.DAYS);

        ChangeRequest changeRequest1 = mock(ChangeRequest.class);
        ChangeRequest changeRequest2 = mock(ChangeRequest.class);
        when(this.changeRequestStorageManager.findOpenChangeRequestsByDate(any(Date.class), eq(true)))
            .thenAnswer(invocationOnMock -> {
                Date requestedDate = invocationOnMock.getArgument(0);
                Instant requestedInstant = requestedDate.toInstant();
                // if the test is fast enough, both date could be equals
                if (!requestedInstant.equals(beforeExpectedDate)) {
                    assertTrue(requestedInstant.isAfter(beforeExpectedDate),
                        String.format("%s should be after %s", requestedInstant, beforeExpectedDate));
                }
                assertTrue(requestedInstant.isBefore(afterExpectedDate));
                return Arrays.asList(changeRequest1, changeRequest2);
        });

        when(changeRequest1.getStaleDate()).thenReturn(new Date(42));
        when(changeRequest2.getId()).thenReturn("CR2");
        this.schedulerJobManager.notifyStaleChangeRequests();

        verify(this.context).setUserReference(userDocReference);
        verify(this.observationManager).notify(any(StaleChangeRequestEvent.class), eq("CR2"), eq(changeRequest2));
        verify(changeRequest1, never()).getId();
        verify(changeRequest2).setStaleDate(any(Date.class));
        verify(changeRequest1, never()).setStaleDate(any(Date.class));
        verify(this.changeRequestStorageManager).saveStaleDate(changeRequest2);
        verify(this.changeRequestStorageManager, never()).saveStaleDate(changeRequest1);
    }

    @Test
    void closeStaleChangeRequests() throws ChangeRequestException
    {
        when(this.configuration.getStaleChangeRequestDurationForClosing()).thenReturn(0L);
        this.schedulerJobManager.closeStaleChangeRequests();

        // when duration is set to 0 the feature is entirely disabled
        verify(this.changeRequestStorageManager, never()).findOpenChangeRequestsByDate(any(), anyBoolean());
        verify(this.changeRequestStorageManager, never()).findChangeRequestsStaledBefore(any());

        when(this.configuration.getStaleChangeRequestDurationForClosing()).thenReturn(5L);
        when(this.configuration.getStaleChangeRequestDurationForNotifying()).thenReturn(2L);

        UserReference requestedSchedulerUser = mock(UserReference.class);
        when(this.configuration.getSchedulerContextUser()).thenReturn(requestedSchedulerUser);
        DocumentReference userDocReference = mock(DocumentReference.class);
        when(this.userReferenceConverter.convert(requestedSchedulerUser)).thenReturn(userDocReference);

        Date now = new Date();
        Instant beforeExpectedDate = now.toInstant().minus(5L, ChronoUnit.DAYS);
        Instant afterExpectedDate = now.toInstant().minus(4L, ChronoUnit.DAYS);

        ChangeRequest changeRequest1 = mock(ChangeRequest.class);
        ChangeRequest changeRequest2 = mock(ChangeRequest.class);
        when(this.changeRequestStorageManager.findChangeRequestsStaledBefore(any(Date.class)))
            .thenAnswer(invocationOnMock -> {
                Date requestedDate = invocationOnMock.getArgument(0);
                Instant requestedInstant = requestedDate.toInstant();
                // if the test is fast enough, both date could be equals
                if (!requestedInstant.equals(beforeExpectedDate)) {
                    assertTrue(requestedInstant.isAfter(beforeExpectedDate),
                        String.format("%s should be after %s", requestedInstant, beforeExpectedDate));
                }
                assertTrue(requestedInstant.isBefore(afterExpectedDate));
                return Arrays.asList(changeRequest1, changeRequest2);
            });
        this.schedulerJobManager.closeStaleChangeRequests();

        verify(this.context).setUserReference(userDocReference);
        verify(this.changeRequestManager).updateStatus(changeRequest1, ChangeRequestStatus.STALE);
        verify(this.changeRequestManager).updateStatus(changeRequest2, ChangeRequestStatus.STALE);

        // When notification duration is set to 0, we directly close the change requests based on their inactivity
        when(this.configuration.getStaleChangeRequestDurationForNotifying()).thenReturn(0L);

        Date now2 = new Date();
        Instant beforeExpectedDate2 = now.toInstant().minus(5L, ChronoUnit.DAYS);
        Instant afterExpectedDate2 = now.toInstant().minus(4L, ChronoUnit.DAYS);

        ChangeRequest changeRequest3 = mock(ChangeRequest.class);
        ChangeRequest changeRequest4 = mock(ChangeRequest.class);
        when(this.changeRequestStorageManager.findOpenChangeRequestsByDate(any(Date.class), eq(false)))
            .thenAnswer(invocationOnMock -> {
                Date requestedDate = invocationOnMock.getArgument(0);
                Instant requestedInstant = requestedDate.toInstant();
                // if the test is fast enough, both date could be equals
                if (!requestedInstant.equals(beforeExpectedDate2)) {
                    assertTrue(requestedInstant.isAfter(beforeExpectedDate2),
                        String.format("%s should be after %s", requestedInstant, beforeExpectedDate2));
                }
                assertTrue(requestedInstant.isBefore(afterExpectedDate2));
                return Arrays.asList(changeRequest3, changeRequest4);
            });
        this.schedulerJobManager.closeStaleChangeRequests();
        verify(this.context, times(2)).setUserReference(userDocReference);
        verify(this.changeRequestManager).updateStatus(changeRequest3, ChangeRequestStatus.STALE);
        verify(this.changeRequestManager).updateStatus(changeRequest4, ChangeRequestStatus.STALE);
        verify(this.changeRequestStorageManager).findChangeRequestsStaledBefore(any());
        verify(this.changeRequestStorageManager).findOpenChangeRequestsByDate(any(), anyBoolean());
    }
}
