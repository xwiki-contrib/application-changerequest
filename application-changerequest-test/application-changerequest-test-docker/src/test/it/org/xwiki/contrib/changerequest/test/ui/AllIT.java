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
package org.xwiki.contrib.changerequest.test.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.xwiki.test.docker.junit5.UITest;

/**
 * All the UI tests for Change Request application.
 *
 * @version $Id$
 * @since 0.5
 */
@UITest
public class AllIT
{
    @Nested
    @DisplayName("Change Request Creations")
    class NestedChangeRequestCreationIT extends ChangeRequestCreationIT
    {
    }

    @Nested
    @DisplayName("Change Request Rights")
    class NestedChangeRequestRightsIT extends ChangeRequestRightsIT
    {
    }

    @Nested
    @DisplayName("Change Request Rebase and Conflicts handling ")
    class NestedChangeRequestConflictsIT extends ChangeRequestConflictsIT
    {
    }

    @Nested
    @DisplayName("Delegate Approvers mechanism")
    class NestedDelegateApproversIT extends DelegateApproversIT
    {
    }
    
    @Nested
    @DisplayName("Split Change Request feature")
    class NestedSplitChangeRequestIT extends SplitChangeRequestIT
    {
    }

    @Nested
    @DisplayName("Minimum Approvers mechanism")
    class NestedMinimumApproversIT extends MinimumApproversIT
    {
    }

    @Nested
    @DisplayName("Edition of existing change request")
    class NestedChangeRequestEditionIT extends ChangeRequestEditionIT
    {
    }

    @Nested
    @DisplayName("Merging change request")
    class NestedChangeRequestMergeIT extends ChangeRequestMergeIT
    {
    }

    @Nested
    @DisplayName("Add changes to existing change request")
    class NestedChangeRequestAddChangesIT extends ChangeRequestAddChangesIT
    {
    }

    @Nested
    @DisplayName("Manipulate discussions in change request")
    class NestedChangeRequestDiscussionIT extends ChangeRequestDiscussionIT
    {
    }

    @Nested
    @DisplayName("Test Stale Change Request feature")
    class NestedStaleChangeRequestIT extends StaleChangeRequestIT
    {
    }

    @Nested
    @DisplayName("Test cancellation of a save")
    class NestedChangeRequestCancelSaveIT extends ChangeRequestCancelSaveIT
    {
    }
}
