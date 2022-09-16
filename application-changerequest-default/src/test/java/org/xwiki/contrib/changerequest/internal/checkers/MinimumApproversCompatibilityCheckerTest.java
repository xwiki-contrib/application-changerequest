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
package org.xwiki.contrib.changerequest.internal.checkers;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.xwiki.contrib.changerequest.ApproversManager;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestConfiguration;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link MinimumApproversCompatibilityChecker}.
 *
 * @version $Id$
 * @since 1.2
 */
@ComponentTest
class MinimumApproversCompatibilityCheckerTest
{
    @InjectMockComponents
    private MinimumApproversCompatibilityChecker checker;

    @MockComponent
    private ChangeRequestConfiguration configuration;

    @MockComponent
    private ApproversManager<ChangeRequest> changeRequestApproversManager;

    @MockComponent
    private ApproversManager<XWikiDocument> documentApproversManager;

    @MockComponent
    private ContextualLocalizationManager contextualLocalizationManager;

    @Test
    void canChangeOnDocumentBeAddedWithDocReference() throws ChangeRequestException
    {
        when(this.configuration.getMinimumApprovers()).thenReturn(0);
        assertTrue(this.checker.canChangeOnDocumentBeAdded(null, null, null));
        verify(this.changeRequestApproversManager, never()).getAllApprovers(any(), anyBoolean());

        ChangeRequest changeRequest = mock(ChangeRequest.class);
        DocumentReference documentReference = mock(DocumentReference.class);

        when(this.configuration.getMinimumApprovers()).thenReturn(2);
        when(this.changeRequestApproversManager.getAllApprovers(changeRequest, false))
            .thenReturn(Set.of(mock(UserReference.class), mock(UserReference.class), mock(UserReference.class)));
        assertTrue(this.checker.canChangeOnDocumentBeAdded(changeRequest, documentReference, null));

        when(this.configuration.getMinimumApprovers()).thenReturn(4);
        assertFalse(this.checker.canChangeOnDocumentBeAdded(changeRequest, documentReference, null));
    }

    @Test
    void canChangeOnDocumentBeAddedWithFileChange() throws ChangeRequestException
    {
        when(this.configuration.getMinimumApprovers()).thenReturn(0);
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        FileChange fileChange = mock(FileChange.class);
        assertTrue(this.checker.canChangeOnDocumentBeAdded(changeRequest, fileChange));
        verify(this.changeRequestApproversManager, never()).getAllApprovers(any(), anyBoolean());

        when(this.configuration.getMinimumApprovers()).thenReturn(2);
        when(this.changeRequestApproversManager.getAllApprovers(changeRequest, false))
            .thenReturn(Set.of(mock(UserReference.class), mock(UserReference.class), mock(UserReference.class)));
        assertTrue(this.checker.canChangeOnDocumentBeAdded(changeRequest, fileChange));

        when(this.configuration.getMinimumApprovers()).thenReturn(4);

        DocumentReference documentReference = mock(DocumentReference.class);
        when(fileChange.getTargetEntity()).thenReturn(documentReference);
        when(changeRequest.getLatestFileChangeFor(documentReference)).thenReturn(Optional.empty());
        assertFalse(this.checker.canChangeOnDocumentBeAdded(changeRequest, fileChange));

        FileChange previousFileChange = mock(FileChange.class, "previous");
        when(changeRequest.getLatestFileChangeFor(documentReference)).thenReturn(Optional.of(previousFileChange));
        assertFalse(this.checker.canChangeOnDocumentBeAdded(changeRequest, fileChange));

        XWikiDocument previousDoc = mock(XWikiDocument.class, "previous");
        XWikiDocument currentDoc = mock(XWikiDocument.class, "current");
        when(fileChange.getModifiedDocument()).thenReturn(currentDoc);
        when(previousFileChange.getModifiedDocument()).thenReturn(previousDoc);

        when(this.documentApproversManager.getAllApprovers(previousDoc, false)).thenReturn(Collections.emptySet());
        when(this.documentApproversManager.getAllApprovers(currentDoc, false))
            .thenReturn(Collections.singleton(mock(UserReference.class)));
        assertTrue(this.checker.canChangeOnDocumentBeAdded(changeRequest, fileChange));

        when(this.documentApproversManager.getAllApprovers(previousDoc, false)).thenReturn(Set.of(
            mock(UserReference.class),
            mock(UserReference.class),
            mock(UserReference.class),
            mock(UserReference.class)
        ));
        when(this.documentApproversManager.getAllApprovers(currentDoc, false)).thenReturn(Set.of(
            mock(UserReference.class),
            mock(UserReference.class),
            mock(UserReference.class)
        ));
        assertFalse(this.checker.canChangeOnDocumentBeAdded(changeRequest, fileChange));

        when(this.documentApproversManager.getAllApprovers(currentDoc, false)).thenReturn(Set.of(
            mock(UserReference.class),
            mock(UserReference.class),
            mock(UserReference.class),
            mock(UserReference.class)
        ));
        assertTrue(this.checker.canChangeOnDocumentBeAdded(changeRequest, fileChange));

    }
}