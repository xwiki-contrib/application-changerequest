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

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.ChangeRequestMergeDocumentResult;
import org.xwiki.contrib.changerequest.ChangeRequestMergeManager;
import org.xwiki.contrib.changerequest.ConflictResolutionChoice;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.diff.Conflict;
import org.xwiki.diff.ConflictDecision;
import org.xwiki.diff.internal.DefaultConflictDecision;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;
import org.xwiki.store.merge.MergeDocumentResult;

/**
 * Script service dedicated to merge operation in change request.
 *
 * @version $Id$
 * @since 0.11
 */
@Component
@Singleton
@Named("changerequest.merge")
public class ChangeRequestMergeScriptService implements ScriptService
{
    @Inject
    private ChangeRequestMergeManager changeRequestMergeManager;

    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    /**
     * Perform a merge without saving between the changes of the change request related to the given reference, and the
     * published document with same reference, and returns the merge result.
     *
     * @param changeRequest the change request for which to find changes.
     * @param documentReference the document reference for which to perform a merge.
     * @return a {@link Optional#empty()} if no change for the given reference can be found or if an error occurs
     *         during the merge, else an optional containing the {@link MergeDocumentResult}.
     * @throws ChangeRequestException in case of problem for loading information.
     * @since 0.4
     */
    public Optional<ChangeRequestMergeDocumentResult> getMergeDocumentResult(ChangeRequest changeRequest,
        DocumentReference documentReference) throws ChangeRequestException
    {
        Optional<ChangeRequestMergeDocumentResult> result = Optional.empty();
        Optional<FileChange> optionalFileChange = changeRequest.getLatestFileChangeFor(documentReference);
        if (optionalFileChange.isPresent()) {
            ChangeRequestMergeDocumentResult changeRequestMergeDocumentResult =
                this.changeRequestMergeManager.getMergeDocumentResult(optionalFileChange.get());
            result = Optional.of(changeRequestMergeDocumentResult);
        }
        return result;
    }

    /**
     * Allow to create a {@link ConflictDecision} based on the given parameters.
     *
     * @param mergeDocumentResult the merge result for which to create a conflict decision.
     * @param conflictReference the reference of the conflict for which to create the decision.
     * @param decisionType the decision taken for fixing the conflict.
     * @param customResolution a custom resolution input. Note that if this parameter is given, then the decisionType
     *                         will be set to custom.
     * @return an {@link Optional#empty()} if no conflict matches the given reference in the merge result, else returns
     *          a {@link ConflictDecision} with the appropriate information to be used in
     *          {@link #fixConflicts(ChangeRequest, DocumentReference, ConflictResolutionChoice, List)}.
     * @since 0.4
     */
    public Optional<ConflictDecision<?>> createConflictDecision(MergeDocumentResult mergeDocumentResult,
        String conflictReference, ConflictDecision.DecisionType decisionType, List<Object> customResolution)
    {
        Optional<ConflictDecision<?>> result = Optional.empty();
        Conflict<?> concernedConflict = null;
        for (Conflict<?> conflict : mergeDocumentResult.getConflicts()) {
            if (StringUtils.equals(conflictReference, conflict.getReference())) {
                concernedConflict = conflict;
                break;
            }
        }
        if (concernedConflict != null) {
            ConflictDecision<Object> decision = new DefaultConflictDecision<>(concernedConflict);
            decision.setType(decisionType);
            if (customResolution != null && !customResolution.isEmpty()) {
                decision.setCustom(customResolution);
            }
            result = Optional.of(decision);
        }

        return result;
    }

    /**
     * Fix conflicts related to the given {@link MergeDocumentResult} by applying the given decision.
     *
     * @param changeRequest the change request for which to fix the conflicts.
     * @param documentReference the document reference for which to perform a merge.
     * @param resolutionChoice the global choice to make.
     * @param customDecisions the specific decisions in case the resolution choice was
     *          {@link ConflictResolutionChoice#CUSTOM}.
     * @return {@code true} if the conflicts were properly fixed, {@code false} if any problem occurs preventing to fix
     *          the conflict.
     * @throws ChangeRequestException in case of problem for applying decisions.
     * @since 0.4
     */
    public boolean fixConflicts(ChangeRequest changeRequest, DocumentReference documentReference,
        ConflictResolutionChoice resolutionChoice, List<ConflictDecision<?>> customDecisions)
        throws ChangeRequestException
    {
        boolean result = false;

        if (this.canFixConflict(changeRequest, documentReference)) {
            Optional<FileChange> optionalFileChange = changeRequest.getLatestFileChangeFor(documentReference);
            if (optionalFileChange.isPresent()) {
                result = this.changeRequestMergeManager
                    .mergeWithConflictDecision(optionalFileChange.get(), resolutionChoice, customDecisions);
            }
        }

        return result;
    }

    /**
     * Check if the current user can fix a conflict related to the given document reference in the given change request.
     * @param changeRequest the change request concerned by a conflict.
     * @param documentReference the reference of the document concerned by the conflict.
     * @return {@code true} if the current user is authorized to fix the conflict.
     */
    private boolean canFixConflict(ChangeRequest changeRequest, DocumentReference documentReference)
        throws ChangeRequestException
    {
        Optional<FileChange> optionalFileChange = changeRequest.getLatestFileChangeFor(documentReference);
        if (optionalFileChange.isPresent()) {
            ChangeRequestAuthorizationScriptService authorizationScriptService = null;
            try {
                authorizationScriptService = this.componentManagerProvider.get()
                    .getInstance(ScriptService.class, "changerequest.authorization");
                return authorizationScriptService.isAuthorizedToEdit(changeRequest);
            } catch (ComponentLookupException e) {
                throw new ChangeRequestException("Error when trying to access authorization script service.", e);
            }
        }
        return false;
    }

    /**
     * Check if the given change request has any conflict.
     *
     * @param changeRequest the change request for which to check if it has conflict.
     * @return {@code true} if at least one conflict has been found in a document.
     * @throws ChangeRequestException in case of problem when checking presence of conflict.
     * @since 0.13
     */
    @Unstable
    public boolean hasConflict(ChangeRequest changeRequest) throws ChangeRequestException
    {
        return this.changeRequestMergeManager.hasConflict(changeRequest);
    }
}
