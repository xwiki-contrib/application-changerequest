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

import java.util.List;

import javax.annotation.Priority;

import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.FileChangeSavingChecker;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link FileChangeSavingCheckersLoader}.
 *
 * @version $Id$
 */
@ComponentTest
class FileChangeSavingCheckersLoaderTest
{
    @InjectMockComponents
    private FileChangeSavingCheckersLoader loader;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @BeforeComponent
    void setup() throws Exception
    {
        this.componentManager.registerComponent(ComponentManager.class, "context", this.componentManager);
    }

    @Priority(10)
    class Checker1 implements FileChangeSavingChecker
    {
        @Override
        public SavingCheckerResult canChangeOnDocumentBeAdded(ChangeRequest changeRequest,
            DocumentReference documentReference, FileChange.FileChangeType changeType)
        {
            return null;
        }

        @Override
        public SavingCheckerResult canChangeRequestBeCreatedWith(FileChange fileChange)
        {
            return null;
        }
    }

    @Priority(9)
    class Checker2 implements FileChangeSavingChecker
    {
        @Override
        public SavingCheckerResult canChangeOnDocumentBeAdded(ChangeRequest changeRequest,
            DocumentReference documentReference, FileChange.FileChangeType changeType)
        {
            return null;
        }

        @Override
        public SavingCheckerResult canChangeRequestBeCreatedWith(FileChange fileChange)
        {
            return null;
        }
    }

    class Checker3 implements FileChangeSavingChecker
    {
        @Override
        public SavingCheckerResult canChangeOnDocumentBeAdded(ChangeRequest changeRequest,
            DocumentReference documentReference, FileChange.FileChangeType changeType)
        {
            return null;
        }

        @Override
        public SavingCheckerResult canChangeRequestBeCreatedWith(FileChange fileChange)
        {
            return null;
        }
    }

    @Test
    void getCheckers() throws Exception
    {
        Checker1 checker1 = new Checker1();
        Checker2 checker2 = new Checker2();
        Checker3 checker3 = new Checker3();

        componentManager.registerComponent(FileChangeSavingChecker.class, "checker1", checker1);
        componentManager.registerComponent(FileChangeSavingChecker.class, "checker2", checker2);
        componentManager.registerComponent(FileChangeSavingChecker.class, "checker3", checker3);

        List<FileChangeSavingChecker> checkers = this.loader.getCheckers();
        assertEquals(List.of(checker2, checker1, checker3), checkers);
    }
}