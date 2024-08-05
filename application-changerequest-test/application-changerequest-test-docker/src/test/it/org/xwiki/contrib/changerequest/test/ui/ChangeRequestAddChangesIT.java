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
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.ckeditor.test.po.CKEditor;
import org.xwiki.contrib.changerequest.test.po.ChangeRequestPage;
import org.xwiki.contrib.changerequest.test.po.ChangeRequestSaveModal;
import org.xwiki.contrib.changerequest.test.po.ExtendedCreatePage;
import org.xwiki.contrib.changerequest.test.po.ExtendedEditPage;
import org.xwiki.contrib.changerequest.test.po.ExtendedViewPage;
import org.xwiki.contrib.changerequest.test.po.filechanges.ChangeType;
import org.xwiki.contrib.changerequest.test.po.filechanges.FileChangesPane;
import org.xwiki.contrib.changerequest.test.po.filechanges.FilechangesLiveDataElement;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.editor.WikiEditPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests related to adding changes to an existing change request.
 *
 * @version $Id$
 * @since 1.5
 */
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
public class ChangeRequestAddChangesIT
{
    private static final String TEST_USER_PREFIX = "CRAddChangesIT";

    private static final String CR_USER = TEST_USER_PREFIX + "CRUser";

    @BeforeAll
    void beforeAll(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
        setup.createUser(CR_USER, CR_USER, null, "editor", "Wysiwyg", "usertype", "Advanced");
        setup.setGlobalRights("", CR_USER, "crapprove", true);
    }

    /**
     * Test that requesting the same page creation in same change request should lead to a conflict.
     */
    @Test
    @Order(1)
    void createSamePage(TestUtils testUtils, TestReference testReference)
    {
        String serializedReference = testReference.getLocalDocumentReference().toString();
        testUtils.login(CR_USER, CR_USER);
        testUtils.gotoPage(testReference);
        ExtendedViewPage extendedViewPage = new ExtendedViewPage();
        ExtendedCreatePage extendedCreatePage = extendedViewPage.clickStandardCreate();

        assertTrue(extendedCreatePage.hasChangeRequestCreateButton());
        ExtendedEditPage<CKEditor> extendedEditPage = extendedCreatePage.clickChangeRequestCreateButton(true);
        extendedEditPage.getWrappedEditor().getRichTextArea().setContent("Some content in the new page");
        // FIXME: We should be able to do that from the PO
        WikiEditPage wikiEditPage = new WikiEditPage();
        wikiEditPage.setTitle("A new title");
        ChangeRequestSaveModal changeRequestSaveModal = extendedEditPage.clickSaveAsChangeRequest();
        changeRequestSaveModal.setChangeRequestTitle(testReference.getLastSpaceReference().getName());
        ChangeRequestPage changeRequestPage = changeRequestSaveModal.clickSave();

        FileChangesPane fileChangesPane = changeRequestPage.openFileChanges();
        List<FilechangesLiveDataElement.FilechangesRowElement> fileChanges =
            fileChangesPane.getFileChangesListLiveData().getFileChanges();
        assertEquals(1, fileChanges.size());
        FilechangesLiveDataElement.FilechangesRowElement filechangesRowElement = fileChanges.get(0);

        assertEquals(ChangeType.CREATION, filechangesRowElement.getChangeType());
        assertEquals("filechange-1.1", filechangesRowElement.getVersion());
        assertEquals(serializedReference, filechangesRowElement.getReference());
        assertEquals("A new title", filechangesRowElement.getTitle());

        testUtils.gotoPage(testReference);
        extendedViewPage = new ExtendedViewPage();
        extendedCreatePage = extendedViewPage.clickStandardCreate();

        assertTrue(extendedCreatePage.hasChangeRequestCreateButton());
        extendedEditPage = extendedCreatePage.clickChangeRequestCreateButton(true);
        extendedEditPage.getWrappedEditor().getRichTextArea().setContent("Some other content");
        changeRequestSaveModal = extendedEditPage.clickSaveAsChangeRequest();
        changeRequestSaveModal.openAddChangesToExistingChangeRequestCollapse();
        changeRequestSaveModal.selectExistingChangeRequest(testReference.getLastSpaceReference().getName());
        changeRequestSaveModal.clickSaveExpectFailure(false);
        changeRequestSaveModal = new ChangeRequestSaveModal();
        assertTrue(changeRequestSaveModal.hasErrorDisplayed());
        assertEquals("This Change Request cannot be saved. "
            + "Try creating a new Change Request or add your changes to another Change Request. "
            + "The following errors were encountered: Conflict found in the changes.",
            changeRequestSaveModal.getOtherErrorMessage());

        // Close the modal to prevent issues with other tests.
        changeRequestSaveModal.close();
        extendedEditPage.clickCancel();
    }

    /**
     * Test that performing edition of a page in same change request should lead to a conflict.
     */
    @Test
    @Order(2)
    void editSamePage(TestUtils testUtils, TestReference testReference)
    {
        testUtils.loginAsSuperAdmin();
        testUtils.createPage(testReference, "Some content");
        String serializedReference = testReference.getLocalDocumentReference().toString();
        testUtils.login(CR_USER, CR_USER);
        testUtils.gotoPage(testReference);
        ExtendedViewPage extendedViewPage = new ExtendedViewPage();
        extendedViewPage.editWYSIWYG();
        CKEditor editor = new CKEditor("content");
        ExtendedEditPage<CKEditor> extendedEditPage = new ExtendedEditPage<>(editor);

        extendedEditPage.getWrappedEditor().getRichTextArea().setContent("Some content in the new page");
        ChangeRequestSaveModal changeRequestSaveModal = extendedEditPage.clickSaveAsChangeRequest();
        changeRequestSaveModal.setChangeRequestTitle(testReference.getLastSpaceReference().getName());
        ChangeRequestPage changeRequestPage = changeRequestSaveModal.clickSave();

        FileChangesPane fileChangesPane = changeRequestPage.openFileChanges();
        List<FilechangesLiveDataElement.FilechangesRowElement> fileChanges =
            fileChangesPane.getFileChangesListLiveData().getFileChanges();
        assertEquals(1, fileChanges.size());
        FilechangesLiveDataElement.FilechangesRowElement filechangesRowElement = fileChanges.get(0);

        assertEquals(ChangeType.EDITION, filechangesRowElement.getChangeType());
        assertEquals("filechange-2.1", filechangesRowElement.getVersion());
        assertEquals(serializedReference, filechangesRowElement.getReference());

        testUtils.gotoPage(testReference);
        extendedViewPage = new ExtendedViewPage();
        extendedViewPage.editWYSIWYG();
        editor = new CKEditor("content");
        extendedEditPage = new ExtendedEditPage<>(editor);

        extendedEditPage.getWrappedEditor().getRichTextArea().setContent("Some other content");
        changeRequestSaveModal = extendedEditPage.clickSaveAsChangeRequest();
        changeRequestSaveModal.openAddChangesToExistingChangeRequestCollapse();
        changeRequestSaveModal.selectExistingChangeRequest(testReference.getLastSpaceReference().getName());
        changeRequestSaveModal.clickSaveExpectFailure(false);
        assertTrue(changeRequestSaveModal.hasErrorDisplayed());
        assertEquals("This Change Request cannot be saved. "
            + "Try creating a new Change Request or add your changes to another Change Request. "
            + "The following errors were encountered: Conflict found in the changes.",
            changeRequestSaveModal.getOtherErrorMessage());

        // Close the modal to prevent issues with other tests.
        changeRequestSaveModal.close();
        extendedEditPage.clickCancel();
    }
}
