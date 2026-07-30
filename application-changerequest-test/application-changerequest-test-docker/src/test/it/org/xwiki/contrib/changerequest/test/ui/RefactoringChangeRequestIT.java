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

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.xwiki.ckeditor.test.po.CKEditor;
import org.xwiki.contrib.changerequest.test.po.ChangeRequestLiveDataElement;
import org.xwiki.contrib.changerequest.test.po.ChangeRequestPage;
import org.xwiki.contrib.changerequest.test.po.ChangeRequestSaveModal;
import org.xwiki.contrib.changerequest.test.po.ExtendedCreatePage;
import org.xwiki.contrib.changerequest.test.po.ExtendedDeleteConfirmationPage;
import org.xwiki.contrib.changerequest.test.po.ExtendedEditPage;
import org.xwiki.contrib.changerequest.test.po.ExtendedViewPage;
import org.xwiki.contrib.changerequest.test.po.discussion.DiscussionEditor;
import org.xwiki.contrib.changerequest.test.po.discussion.MessageElement;
import org.xwiki.contrib.changerequest.test.po.discussion.ReviewDiscussion;
import org.xwiki.contrib.changerequest.test.po.filechanges.ChangeRequestEntityDiff;
import org.xwiki.contrib.changerequest.test.po.filechanges.FileChangesPane;
import org.xwiki.contrib.changerequest.test.po.filechanges.LineChange;
import org.xwiki.contrib.changerequest.test.po.reviews.ReviewContainer;
import org.xwiki.contrib.changerequest.test.po.reviews.ReviewElement;
import org.xwiki.contrib.changerequest.test.po.reviews.ReviewsPane;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.CopyOrRenameOrDeleteStatusPage;
import org.xwiki.test.ui.po.RenamePage;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@UITest(
    properties = {
        "xwikiDbHbmCommonExtraMappings=notification-filter-preferences.hbm.xml"
    },
    extraJARs = {
        // It's currently not possible to install a JAR contributing a Hibernate mapping file as an Extension. Thus
        // we need to provide the JAR inside WEB-INF/lib. See https://jira.xwiki.org/browse/XWIKI-8271
        "org.xwiki.platform:xwiki-platform-notifications-filters-default",
        // The Solr store is not ready yet to be installed as extension
        "org.xwiki.platform:xwiki-platform-eventstream-store-solr"
    },
    resolveExtraJARs = true
)
public class RefactoringChangeRequestIT
{
    private static final String TEST_USER_PREFIX = "RefactoringChangeRequestIT";

    private static final String CR_USER = TEST_USER_PREFIX + "CRUser";

    @BeforeAll
    void beforeAll(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
        setup.createUser(CR_USER, CR_USER, null, "editor", "Wysiwyg", "usertype", "Advanced");
        setup.setGlobalRights("", CR_USER, "crapprove", true);
    }

    @Test
    void refactoringSpace(TestUtils testUtils, TestReference testReference)
    {
        testUtils.loginAsSuperAdmin();
        DocumentReference mainPage = new DocumentReference(testReference);
        DocumentReference subPage = new DocumentReference("WebHome", new SpaceReference("SubPage",
            testReference.getLastSpaceReference()));
        DocumentReference subPageTerminal = new DocumentReference("SubPageTerminal",
            testReference.getLastSpaceReference());

        testUtils.createPage(mainPage, "Main space page.");
        testUtils.createPage(subPage, "A sub page.");
        testUtils.createPage(subPageTerminal, "The terminal sub page.");

        testUtils.login(CR_USER, CR_USER);

        // Create CR1 containing changes for main page and merging it
        testUtils.gotoPage(mainPage);
        ExtendedViewPage extendedViewPage = new ExtendedViewPage();
        ExtendedEditPage<CKEditor> extendedEditPage = extendedViewPage.clickStandardEdit(true);
        extendedEditPage.getWrappedEditor().getRichTextArea().setContent("Main space page.\nNew changes.");
        ChangeRequestSaveModal changeRequestSaveModal = extendedEditPage.clickSaveAsChangeRequest();
        changeRequestSaveModal.setChangeRequestTitle("Refactoring_CR1");
        ChangeRequestPage changeRequestPage = changeRequestSaveModal.clickSave();
        ReviewContainer reviewContainer = changeRequestPage.clickReviewButton();
        reviewContainer.selectApprove();
        changeRequestPage = reviewContainer.save();
        changeRequestPage.clickMergeButton();

        // Create CR2 containing changes for main page, sub page and terminal page and a new page to create, reviewing
        // it and keeping it open.
        testUtils.gotoPage(mainPage);
        extendedViewPage = new ExtendedViewPage();
        extendedEditPage = extendedViewPage.clickStandardEdit(true);
        extendedEditPage.getWrappedEditor().getRichTextArea().setContent("Main space page.\n Some other changes.");
        changeRequestSaveModal = extendedEditPage.clickSaveAsChangeRequest();
        changeRequestSaveModal.setChangeRequestTitle("Refactoring_CR2");
        changeRequestPage = changeRequestSaveModal.clickSave();

        testUtils.gotoPage(subPage);
        extendedViewPage = new ExtendedViewPage();
        extendedEditPage = extendedViewPage.clickStandardEdit(true);
        extendedEditPage.getWrappedEditor().getRichTextArea().setContent("Sub page.\n Some other changes.");
        changeRequestSaveModal = extendedEditPage.clickSaveAsChangeRequest();
        changeRequestSaveModal.openAddChangesToExistingChangeRequestCollapse();
        changeRequestSaveModal.selectExistingChangeRequest("Refactoring_CR2").select();
        changeRequestPage = changeRequestSaveModal.clickSave();

        testUtils.gotoPage(subPageTerminal);
        extendedViewPage = new ExtendedViewPage();
        extendedEditPage = extendedViewPage.clickStandardEdit(true);
        extendedEditPage.getWrappedEditor().getRichTextArea().setContent("Sub terminal page.\n Some other changes.");
        changeRequestSaveModal = extendedEditPage.clickSaveAsChangeRequest();
        changeRequestSaveModal.openAddChangesToExistingChangeRequestCollapse();
        changeRequestSaveModal.selectExistingChangeRequest("Refactoring_CR2").select();
        changeRequestPage = changeRequestSaveModal.clickSave();

        testUtils.gotoPage(mainPage);
        extendedViewPage = new ExtendedViewPage();
        ExtendedCreatePage extendedCreatePage = extendedViewPage.clickStandardCreate();
        extendedCreatePage.getDocumentPicker().setTitle("MyNewPage");
        extendedEditPage = extendedCreatePage.clickChangeRequestCreateButton(true);
        extendedEditPage.getWrappedEditor().getRichTextArea().setContent("My new page content");
        changeRequestSaveModal = extendedEditPage.clickSaveAsChangeRequest();
        changeRequestSaveModal.openAddChangesToExistingChangeRequestCollapse();
        changeRequestSaveModal.selectExistingChangeRequest("Refactoring_CR2").select();
        changeRequestPage = changeRequestSaveModal.clickSave();

        DocumentReference createdPageReference = new DocumentReference("WebHome", new SpaceReference("MyNewPage",
            testReference.getLastSpaceReference()));

        reviewContainer = changeRequestPage.clickReviewButton();
        reviewContainer.selectApprove();
        reviewContainer.setComment("My review: it's all good.");
        changeRequestPage = reviewContainer.save();

        FileChangesPane fileChangesPane = changeRequestPage.openFileChanges();
        assertTrue(fileChangesPane.getListOfChangedFilesReferences().contains(
            createdPageReference.getLocalDocumentReference().toString()),
            "List of changed files: [" + fileChangesPane.getListOfChangedFilesReferences() +"] "
                + "doesn't contain [" + createdPageReference.getLocalDocumentReference() +"]");
        ChangeRequestEntityDiff contentEntityDiff =
            fileChangesPane.getEntityDiff(mainPage.getLocalDocumentReference().toString(),
                "Page properties");
        DiscussionEditor diffMessageEditor = contentEntityDiff.clickAddingDiffComment("Content", 1, LineChange.ADDED);
        diffMessageEditor.setContent("My diff message.");
        diffMessageEditor.clickSave();
        changeRequestPage.waitForTimelineRefresh();
        assertEquals("Ready for publication", changeRequestPage.getStatusLabel());


        // CR3 request deletion of terminal subpage and modification on sub page, closing it.
        testUtils.gotoPage(subPageTerminal);
        extendedViewPage = new ExtendedViewPage();
        ExtendedDeleteConfirmationPage extendedDeleteConfirmationPage = extendedViewPage.clickRequestForDeletion();
        changeRequestSaveModal = extendedDeleteConfirmationPage.clickChangeRequestDelete();
        changeRequestSaveModal.setChangeRequestTitle("Refactoring_CR3");
        changeRequestSaveModal.clickSave();

        testUtils.gotoPage(subPage);
        extendedViewPage = new ExtendedViewPage();
        extendedEditPage = extendedViewPage.clickStandardEdit(true);
        extendedEditPage.getWrappedEditor().getRichTextArea().setContent("Sub page.\n Some other changes.");
        changeRequestSaveModal = extendedEditPage.clickSaveAsChangeRequest();
        changeRequestSaveModal.openAddChangesToExistingChangeRequestCollapse();
        changeRequestSaveModal.selectExistingChangeRequest("Refactoring_CR3").select();
        changeRequestPage = changeRequestSaveModal.clickSave();
        changeRequestPage.clickClose();

        testUtils.loginAsSuperAdmin();
        testUtils.gotoPage(mainPage);
        extendedViewPage = new ExtendedViewPage();
        ChangeRequestLiveDataElement changeRequestLiveDataElement = extendedViewPage.openChangeRequestTab();
        // CR1 and 2
        assertEquals(2, changeRequestLiveDataElement.countRows());

        testUtils.gotoPage(subPage);
        extendedViewPage = new ExtendedViewPage();
        changeRequestLiveDataElement = extendedViewPage.openChangeRequestTab();
        // CR2 and 3
        assertEquals(2, changeRequestLiveDataElement.countRows());

        testUtils.gotoPage(subPageTerminal);
        extendedViewPage = new ExtendedViewPage();
        changeRequestLiveDataElement = extendedViewPage.openChangeRequestTab();
        // CR2 and 3
        assertEquals(2, changeRequestLiveDataElement.countRows());

        ViewPage viewPage = testUtils.gotoPage(mainPage);
        RenamePage renamePage = viewPage.rename();
        assertTrue(renamePage.isPreserveChildren());
        renamePage.getDocumentPicker().setTitle("RefactoredSpace");
        CopyOrRenameOrDeleteStatusPage renameStatusPage = renamePage.clickRenameButton();
        renameStatusPage = renameStatusPage.waitUntilFinished();
        assertEquals("Done.", renameStatusPage.getInfoMessage());

        SpaceReference testLastSpaceReference = testReference.getLastSpaceReference();
        SpaceReference refactoredSpaceReference =
            new SpaceReference("RefactoredSpace", testLastSpaceReference.getParent());
        DocumentReference mainPageRefactored = new DocumentReference("WebHome", refactoredSpaceReference);
        DocumentReference subPageRefactored = new DocumentReference("WebHome", new SpaceReference("SubPage",
            refactoredSpaceReference));
        DocumentReference subPageTerminalRefactored = new DocumentReference("SubPageTerminal",
            refactoredSpaceReference);
        DocumentReference createdPageReferenceRefactored = new DocumentReference("WebHome", new SpaceReference(
            "MyNewPage", refactoredSpaceReference));

        testUtils.gotoPage(subPageRefactored);
        extendedViewPage = new ExtendedViewPage();
        assertFalse(extendedViewPage.isNewDocument());
        changeRequestLiveDataElement = extendedViewPage.openChangeRequestTab();
        // CR2 and 3
        assertEquals(2, changeRequestLiveDataElement.countRows());

        testUtils.gotoPage(subPageRefactored);
        extendedViewPage = new ExtendedViewPage();
        assertFalse(extendedViewPage.isNewDocument());
        changeRequestLiveDataElement = extendedViewPage.openChangeRequestTab();
        // CR2 and 3
        assertEquals(2, changeRequestLiveDataElement.countRows());

        testUtils.gotoPage(mainPageRefactored);
        extendedViewPage = new ExtendedViewPage();
        assertFalse(extendedViewPage.isNewDocument());
        changeRequestLiveDataElement = extendedViewPage.openChangeRequestTab();
        // The merged CR is not there anymore
        assertEquals(1, changeRequestLiveDataElement.countRows());

        ChangeRequestLiveDataElement.ChangeRequestRowElement changeRequestRowElement =
            changeRequestLiveDataElement.getChangeRequests().get(0);
        assertEquals("Refactoring_CR2", changeRequestRowElement.getTitle());
        changeRequestPage = changeRequestRowElement.gotoChangeRequest();
        assertEquals("Ready for publication", changeRequestPage.getStatusLabel());

        ReviewsPane reviewsPane = changeRequestPage.openReviewsPane();
        List<ReviewElement> reviews = reviewsPane.getReviews();
        assertEquals(1, reviews.size());
        ReviewElement reviewElement = reviews.get(0);
        assertTrue(reviewElement.isApproval());
        assertFalse(reviewElement.isOutdated());
        assertTrue(reviewElement.hasDiscussion());
        ReviewDiscussion discussion = reviewElement.getDiscussion();
        List<MessageElement> messages = discussion.getMessages();
        assertEquals(1, messages.size());
        MessageElement messageElement = messages.get(0);
        assertFalse(messageElement.isOutdated());
        assertEquals("My review: it's all good.", messageElement.getContent());

        fileChangesPane = changeRequestPage.openFileChanges();
        List<String> listOfChangedFiles = fileChangesPane.getListOfChangedFilesReferences();
        assertEquals(4, listOfChangedFiles.size());
        assertTrue(listOfChangedFiles.contains(mainPageRefactored.getLocalDocumentReference().toString()),
            String.format("List of changes files [%s] didn't  contained [%s]", listOfChangedFiles,
                mainPageRefactored.getLocalDocumentReference()));
        assertTrue(listOfChangedFiles.contains(subPageRefactored.getLocalDocumentReference().toString()),
            String.format("List of changes files [%s] didn't  contained [%s]", listOfChangedFiles,
                subPageRefactored.getLocalDocumentReference()));
        assertTrue(listOfChangedFiles.contains(subPageTerminalRefactored.getLocalDocumentReference().toString()),
            String.format("List of changes files [%s] didn't  contained [%s]", listOfChangedFiles,
                subPageTerminalRefactored.getLocalDocumentReference()));
        assertTrue(listOfChangedFiles.contains(createdPageReferenceRefactored.getLocalDocumentReference().toString()),
            String.format("List of changes files [%s] didn't  contained [%s]", listOfChangedFiles,
                createdPageReferenceRefactored.getLocalDocumentReference()));

        assertTrue(fileChangesPane.isDiffOutdated(mainPageRefactored.getLocalDocumentReference().toString()));
        assertTrue(fileChangesPane.isDiffOutdated(subPageRefactored.getLocalDocumentReference().toString()));
        assertTrue(fileChangesPane.isDiffOutdated(subPageTerminalRefactored.getLocalDocumentReference().toString()));
        assertFalse(
            fileChangesPane.isDiffOutdated(createdPageReferenceRefactored.getLocalDocumentReference().toString()));
    }
}
