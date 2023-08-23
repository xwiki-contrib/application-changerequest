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

import org.junit.jupiter.api.BeforeAll;
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
import org.xwiki.contrib.changerequest.test.po.reviews.ReviewContainer;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.HistoryPane;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests dedicated to merging change request.
 *
 * @version $Id$
 * @since 1.5
 */
@UITest
public class ChangeRequestMergeIT
{
    private static final String TEST_USER_PREFIX = "CRMergeTest";

    private static final String CR_USER = TEST_USER_PREFIX + "CRUser";

    @BeforeAll
    void beforeAll(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
        setup.createUser(CR_USER, CR_USER, null);
        setup.setGlobalRights("", CR_USER, "crapprove", true);
        setup.updateObject("XWiki", CR_USER, "XWiki.XWikiUsers", 0, "editor", "Wysiwyg");
    }

    @Test
    void mergeNewPage(TestUtils testUtils, TestReference testReference)
    {
        testUtils.login(CR_USER, CR_USER);
        testUtils.gotoPage(testReference);
        ExtendedViewPage extendedViewPage = new ExtendedViewPage();
        ExtendedCreatePage extendedCreatePage = extendedViewPage.clickStandardCreate();

        assertTrue(extendedCreatePage.hasChangeRequestCreateButton());
        ExtendedEditPage<CKEditor> extendedEditPage = extendedCreatePage.clickChangeRequestCreateButton(true);
        extendedEditPage.getWrappedEditor().getRichTextArea().setContent("Some content in the new page");
        ChangeRequestSaveModal changeRequestSaveModal = extendedEditPage.clickSaveAsChangeRequest();
        changeRequestSaveModal.setChangeRequestTitle("Merge new page");
        ChangeRequestPage changeRequestPage = changeRequestSaveModal.clickSave();
        assertEquals("Ready for review", changeRequestPage.getStatusLabel());
        assertTrue(changeRequestPage.isReviewButtonDisplayed());
        assertTrue(changeRequestPage.isReviewButtonEnabled());

        FileChangesPane fileChangesPane = changeRequestPage.openFileChanges();
        FilechangesLiveDataElement fileChangesListLiveData = fileChangesPane.getFileChangesListLiveData();
        assertEquals(1, fileChangesListLiveData.countRows());
        FilechangesLiveDataElement.FilechangesRowElement filechangesRowElement =
            fileChangesListLiveData.getFileChanges().get(0);
        assertEquals(ChangeType.CREATION, filechangesRowElement.getChangeType());
        assertEquals("filechange-1.1", filechangesRowElement.getVersion());

        ReviewContainer reviewContainer = changeRequestPage.clickReviewButton();
        reviewContainer.selectApprove();
        changeRequestPage = reviewContainer.save();

        assertEquals("Ready for publication", changeRequestPage.getStatusLabel());
        assertTrue(changeRequestPage.isMergeButtonDisplayed());
        assertTrue(changeRequestPage.isMergeButtonEnabled());

        changeRequestPage = changeRequestPage.clickMergeButton();
        assertEquals("Published", changeRequestPage.getStatusLabel());

        ViewPage viewPage = testUtils.gotoPage(testReference);
        assertEquals("Some content in the new page", viewPage.getContent());

        HistoryPane historyPane = viewPage.openHistoryDocExtraPane();
        assertEquals("1.1", historyPane.getCurrentVersion());
    }
}
