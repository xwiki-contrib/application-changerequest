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
package org.xwiki.contrib.changerequest.script;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.ChangeRequestMergeDocumentResult;
import org.xwiki.contrib.changerequest.ChangeRequestMergeManager;
import org.xwiki.contrib.changerequest.ConflictResolutionChoice;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.diff.Chunk;
import org.xwiki.diff.Conflict;
import org.xwiki.diff.ConflictDecision;
import org.xwiki.diff.Delta;
import org.xwiki.diff.internal.DefaultConflictDecision;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.store.merge.MergeDocumentResult;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ChangeRequestMergeScriptService}.
 *
 * @version $Id$
 * @since 0.11
 */
@ComponentTest
class ChangeRequestMergeScriptServiceTest
{
    @InjectMockComponents
    private ChangeRequestMergeScriptService scriptService;

    @MockComponent
    private ChangeRequestMergeManager changeRequestMergeManager;

    @MockComponent
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @Test
    void getMergeDocumentResult() throws ChangeRequestException
    {
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        DocumentReference documentReference = mock(DocumentReference.class);

        when(changeRequest.getLatestFileChangeFor(documentReference)).thenReturn(Optional.empty());
        assertEquals(Optional.empty(), this.scriptService.getMergeDocumentResult(changeRequest, documentReference));

        FileChange fileChange = mock(FileChange.class);
        when(changeRequest.getLatestFileChangeFor(documentReference)).thenReturn(Optional.of(fileChange));
        ChangeRequestMergeDocumentResult expected = mock(ChangeRequestMergeDocumentResult.class);
        when(this.changeRequestMergeManager.getMergeDocumentResult(fileChange)).thenReturn(expected);
        assertEquals(Optional.of(expected),
            this.scriptService.getMergeDocumentResult(changeRequest, documentReference));
    }

    @Test
    void createConflictDecision()
    {
        MergeDocumentResult mergeDocumentResult = mock(MergeDocumentResult.class);
        String conflictReference = "a reference";
        ConflictDecision.DecisionType decisionType = ConflictDecision.DecisionType.CURRENT;
        List<Object> customResolution = mock(List.class);

        Conflict<Object> expectedConflict = mock(Conflict.class);
        when(mergeDocumentResult.getConflicts()).thenReturn(Arrays.asList(
            mock(Conflict.class),
            mock(Conflict.class),
            mock(Conflict.class)
        ));
        assertEquals(Optional.empty(),
            this.scriptService.createConflictDecision(mergeDocumentResult, conflictReference, null, null));

        when(mergeDocumentResult.getConflicts()).thenReturn(Arrays.asList(
            mock(Conflict.class),
            mock(Conflict.class),
            expectedConflict,
            mock(Conflict.class)
        ));
        when(expectedConflict.getReference()).thenReturn(conflictReference);
        ConflictDecision<Object> expected = new DefaultConflictDecision<>(expectedConflict);
        Delta<Object> deltaCurrent = mock(Delta.class);
        when(expectedConflict.getDeltaCurrent()).thenReturn(deltaCurrent);
        Chunk<Object> currentChunk = mock(Chunk.class);
        when(deltaCurrent.getNext()).thenReturn(currentChunk);
        expected.setType(decisionType);
        assertEquals(Optional.of(expected),
            this.scriptService.createConflictDecision(mergeDocumentResult, conflictReference, decisionType, null));

        expected.setCustom(customResolution);
        assertEquals(Optional.of(expected),
            this.scriptService.createConflictDecision(mergeDocumentResult, conflictReference, decisionType,
                customResolution));
    }

    @Test
    void fixConflicts(MockitoComponentManager mockitoComponentManager) throws Exception
    {
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        DocumentReference documentReference = mock(DocumentReference.class);
        FileChange fileChange = mock(FileChange.class);
        when(this.componentManagerProvider.get()).thenReturn(mockitoComponentManager);
        ChangeRequestAuthorizationScriptService authorizationScriptService =
            mock(ChangeRequestAuthorizationScriptService.class);
        mockitoComponentManager.registerComponent(ScriptService.class, "changerequest.authorization",
            authorizationScriptService);
        when(authorizationScriptService.isAuthorizedToEdit(changeRequest)).thenReturn(false);
        when(changeRequest.getLatestFileChangeFor(documentReference)).thenReturn(Optional.of(fileChange));
        assertFalse(this.scriptService
            .fixConflicts(changeRequest, documentReference, ConflictResolutionChoice.CHANGE_REQUEST_VERSION, null));
        verify(this.changeRequestMergeManager, never()).mergeWithConflictDecision(any(), any(), any());

        when(authorizationScriptService.isAuthorizedToEdit(changeRequest)).thenReturn(true);
        List<ConflictDecision<?>> customDecision = mock(List.class);
        when(this.changeRequestMergeManager
            .mergeWithConflictDecision(fileChange, ConflictResolutionChoice.CHANGE_REQUEST_VERSION, customDecision))
            .thenReturn(true);
        assertTrue(this.scriptService.fixConflicts(changeRequest, documentReference,
            ConflictResolutionChoice.CHANGE_REQUEST_VERSION, customDecision));
        verify(authorizationScriptService, times(2)).isAuthorizedToEdit(changeRequest);
    }
}
