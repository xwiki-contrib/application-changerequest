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
import org.xwiki.contrib.changerequest.test.po.ChangeRequestConflictModal;
import org.xwiki.contrib.changerequest.test.po.ChangeRequestPage;
import org.xwiki.contrib.changerequest.test.po.ChangeRequestSaveModal;
import org.xwiki.contrib.changerequest.test.po.ExtendedCreatePage;
import org.xwiki.contrib.changerequest.test.po.ExtendedDeleteConfirmationPage;
import org.xwiki.contrib.changerequest.test.po.ExtendedEditPage;
import org.xwiki.contrib.changerequest.test.po.ExtendedViewPage;
import org.xwiki.contrib.changerequest.test.po.FileChangesPane;
import org.xwiki.contrib.changerequest.test.po.reviews.ReviewElement;
import org.xwiki.contrib.changerequest.test.po.reviews.ReviewModal;
import org.xwiki.contrib.changerequest.test.po.reviews.ReviewsPane;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.editor.WYSIWYGEditPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests related to the rebase and conflicts handling operations.
 *
 * @version $Id$
 * @since 0.10
 */
@UITest(
    properties = {
        "xwikiDbHbmCommonExtraMappings=notification-filter-preferences.hbm.xml"
    },
    extraJARs = {
        // It's currently not possible to install a JAR contributing a Hibernate mapping file as an Extension. Thus
        // we need to provide the JAR inside WEB-INF/lib. See https://jira.xwiki.org/browse/XWIKI-8271
        "org.xwiki.platform:xwiki-platform-notifications-filters-default",
        // It's currently not possible to install a JAR contributing a Hibernate mapping file as an Extension. Thus
        // we need to provide the JAR inside WEB-INF/lib. See https://jira.xwiki.org/browse/XWIKI-8271
        "org.xwiki.platform:xwiki-platform-eventstream-store-hibernate",
        // The Solr store is not ready yet to be installed as extension
        "org.xwiki.platform:xwiki-platform-eventstream-store-solr"
    },
    resolveExtraJARs = true
)
class ChangeRequestConflictsIT
{
    private static final String CR_USER = "CRConflictTest";
    private static final String CR_APPROVER = "CRConflictApproverTest";

    @BeforeAll
    void beforeAll(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
        setup.createUser(CR_USER, CR_USER, null);
        setup.setGlobalRights("", CR_USER, "edit", false);
        setup.setGlobalRights("", CR_USER, "delete", false);

        setup.createUser(CR_APPROVER, CR_APPROVER, null);
        // We force the approver to use WYSIWYG editor, to avoid any problem to display the review modal.
        // This should be removed once https://jira.xwiki.org/browse/XWIKI-19281 is fixed
        setup.updateObject("XWiki", CR_APPROVER, "XWiki.XWikiUsers", 0, "editor", "Wysiwyg");

        setup.setGlobalRights("", CR_APPROVER, "edit", false);
        setup.setGlobalRights("", CR_APPROVER, "crapprove", true);
    }

    @Test
    @Order(1)
    void deleteConflict(TestUtils testUtils, TestReference testReference) throws Exception
    {
        String serializedReference = testReference.getLocalDocumentReference().toString();
        testUtils.createPage(testReference, "Some content");
        testUtils.login(CR_USER, CR_USER);
        testUtils.gotoPage(testReference);
        ExtendedViewPage extendedViewPage = new ExtendedViewPage();

        assertFalse(extendedViewPage.hasStandardDeleteMenuEntry());
        assertTrue(extendedViewPage.hasRequestForDeletionMenuEntry());

        ExtendedDeleteConfirmationPage extendedDeleteConfirmationPage = extendedViewPage.clickRequestForDeletion();
        assertFalse(extendedDeleteConfirmationPage.hasDeleteButton());
        assertTrue(extendedDeleteConfirmationPage.hasChangeRequestDeleteButton());

        ChangeRequestSaveModal saveModal = extendedDeleteConfirmationPage.clickChangeRequestDelete();
        saveModal.setChangeRequestTitle("CRDelete");
        ChangeRequestPage changeRequestPage = saveModal.clickSave();
        String pageURL = changeRequestPage.getPageURL();

        FileChangesPane fileChangesPane = changeRequestPage.openFileChanges();
        assertEquals(FileChangesPane.ChangeType.DELETION, fileChangesPane.getChangeType(serializedReference));
        assertFalse(fileChangesPane.isDiffOutdated(serializedReference));
        assertFalse(fileChangesPane.isConflictLabelDisplayed(serializedReference));
        assertFalse(fileChangesPane.isRefreshActionAvailable(serializedReference));
        assertFalse(fileChangesPane.isFixConflictActionAvailable(serializedReference));

        testUtils.loginAsSuperAdmin();
        testUtils.gotoPage(testReference);
        testUtils.rest().savePage(testReference, "Some new content", "New title");
        testUtils.login(CR_USER, CR_USER);
        testUtils.gotoPage(pageURL);

        changeRequestPage = new ChangeRequestPage();
        fileChangesPane = changeRequestPage.openFileChanges();
        assertEquals(FileChangesPane.ChangeType.DELETION, fileChangesPane.getChangeType(serializedReference));
        assertTrue(fileChangesPane.isDiffOutdated(serializedReference));
        assertFalse(fileChangesPane.isConflictLabelDisplayed(serializedReference));
        assertTrue(fileChangesPane.isRefreshActionAvailable(serializedReference));
        assertFalse(fileChangesPane.isFixConflictActionAvailable(serializedReference));

        changeRequestPage = fileChangesPane.clickRefresh(serializedReference);
        fileChangesPane = changeRequestPage.openFileChanges();
        assertEquals(FileChangesPane.ChangeType.DELETION, fileChangesPane.getChangeType(serializedReference));
        assertFalse(fileChangesPane.isDiffOutdated(serializedReference));
        assertFalse(fileChangesPane.isConflictLabelDisplayed(serializedReference));
        assertFalse(fileChangesPane.isRefreshActionAvailable(serializedReference));
        assertFalse(fileChangesPane.isFixConflictActionAvailable(serializedReference));

        testUtils.loginAsSuperAdmin();
        testUtils.gotoPage(testReference);
        testUtils.rest().delete(testReference);

        testUtils.login(CR_USER, CR_USER);
        testUtils.gotoPage(pageURL);
        changeRequestPage = new ChangeRequestPage();
        fileChangesPane = changeRequestPage.openFileChanges();
        assertEquals(FileChangesPane.ChangeType.DELETION, fileChangesPane.getChangeType(serializedReference));
        assertTrue(fileChangesPane.isDiffOutdated(serializedReference));
        assertFalse(fileChangesPane.isConflictLabelDisplayed(serializedReference));
        assertTrue(fileChangesPane.isRefreshActionAvailable(serializedReference));
        assertFalse(fileChangesPane.isFixConflictActionAvailable(serializedReference));

        changeRequestPage = fileChangesPane.clickRefresh(serializedReference);
        fileChangesPane = changeRequestPage.openFileChanges();
        assertEquals(FileChangesPane.ChangeType.NO_CHANGE, fileChangesPane.getChangeType(serializedReference));
        assertFalse(fileChangesPane.isDiffOutdated(serializedReference));
        assertFalse(fileChangesPane.isConflictLabelDisplayed(serializedReference));
        assertFalse(fileChangesPane.isRefreshActionAvailable(serializedReference));
        assertFalse(fileChangesPane.isFixConflictActionAvailable(serializedReference));
    }

    @Test
    @Order(2)
    void createConflictFixWithPublishedVersion(TestUtils testUtils, TestReference testReference) throws Exception
    {
        String pageName = "NestedPage";
        DocumentReference nestedTestReference =
            new DocumentReference("WebHome", new SpaceReference(pageName, testReference.getLastSpaceReference()));
        String serializedReference = nestedTestReference.getLocalDocumentReference().toString();

        testUtils.loginAsSuperAdmin();
        testUtils.createPage(testReference, "Page parent of create test");

        testUtils.login(CR_USER, CR_USER);
        testUtils.gotoPage(testReference);
        ExtendedViewPage extendedViewPage = new ExtendedViewPage();
        assertFalse(extendedViewPage.hasStandardCreate());
        assertTrue(extendedViewPage.hasChangeRequestCreate());

        ExtendedCreatePage extendedCreatePage = extendedViewPage.clickChangeRequestCreate();
        assertFalse(extendedCreatePage.hasStandardCreateButton());
        assertTrue(extendedCreatePage.hasChangeRequestCreateButton());

        extendedCreatePage.getDocumentPicker().setTitle(pageName);

        ExtendedEditPage<WYSIWYGEditPage> extendedEditPage = extendedCreatePage.clickChangeRequestCreateButton();
        extendedEditPage.getEditor().setContent("Some content on the new page");
        assertFalse(extendedEditPage.hasStandardSaveButton());
        assertTrue(extendedEditPage.hasSaveAsChangeRequestButton());
        ChangeRequestSaveModal changeRequestSaveModal = extendedEditPage.clickSaveAsChangeRequest();
        changeRequestSaveModal.setChangeRequestTitle("CreateKeepPublishedTest");
        ChangeRequestPage changeRequestPage = changeRequestSaveModal.clickSave();

        String crUrl = changeRequestPage.getPageURL();

        FileChangesPane fileChangesPane = changeRequestPage.openFileChanges();
        assertEquals(FileChangesPane.ChangeType.CREATION, fileChangesPane.getChangeType(serializedReference));
        assertFalse(fileChangesPane.isDiffOutdated(serializedReference));
        assertFalse(fileChangesPane.isConflictLabelDisplayed(serializedReference));
        assertFalse(fileChangesPane.isRefreshActionAvailable(serializedReference));
        assertFalse(fileChangesPane.isFixConflictActionAvailable(serializedReference));

        // Create an approval review to checks it's invalidated after the conflict is fixed
        testUtils.login(CR_APPROVER, CR_APPROVER);
        testUtils.gotoPage(crUrl);
        changeRequestPage = new ChangeRequestPage();
        assertTrue(changeRequestPage.isReviewButtonDisplayed());
        assertTrue(changeRequestPage.isReviewButtonEnabled());
        ReviewModal reviewModal = changeRequestPage.clickReviewButton();
        reviewModal.selectApprove();
        reviewModal.save();
        changeRequestPage = new ChangeRequestPage();
        ReviewsPane reviewsPane = changeRequestPage.openReviewsPane();
        List<ReviewElement> reviews = reviewsPane.getReviews();
        assertEquals(1, reviews.size());
        ReviewElement reviewElement = reviews.get(0);
        assertTrue(reviewElement.isApproval());
        assertFalse(reviewElement.isOutdated());

        testUtils.loginAsSuperAdmin();
        testUtils.createPage(nestedTestReference, "Some new content");

        testUtils.login(CR_USER, CR_USER);
        testUtils.gotoPage(crUrl);
        changeRequestPage = new ChangeRequestPage();

        fileChangesPane = changeRequestPage.openFileChanges();
        assertEquals(FileChangesPane.ChangeType.CREATION, fileChangesPane.getChangeType(serializedReference));
        assertTrue(fileChangesPane.isDiffOutdated(serializedReference));
        assertTrue(fileChangesPane.isConflictLabelDisplayed(serializedReference));
        assertFalse(fileChangesPane.isRefreshActionAvailable(serializedReference));
        assertTrue(fileChangesPane.isFixConflictActionAvailable(serializedReference));

        ChangeRequestConflictModal changeRequestConflictModal = fileChangesPane.clickFixConflict(serializedReference);
        assertTrue(changeRequestConflictModal
            .isOptionAvailable(ChangeRequestConflictModal.ResolutionChoice.KEEP_CHANGE_REQUEST));
        assertTrue(changeRequestConflictModal
            .isOptionAvailable(ChangeRequestConflictModal.ResolutionChoice.KEEP_PUBLISHED));
        assertFalse(changeRequestConflictModal
            .isOptionAvailable(ChangeRequestConflictModal.ResolutionChoice.CUSTOM));

        changeRequestConflictModal =
            changeRequestConflictModal.makeChoice(ChangeRequestConflictModal.ResolutionChoice.KEEP_PUBLISHED);
        changeRequestPage = changeRequestConflictModal.submitCurrentChoice();

        fileChangesPane = changeRequestPage.openFileChanges();
        assertEquals(FileChangesPane.ChangeType.NO_CHANGE, fileChangesPane.getChangeType(serializedReference));
        assertFalse(fileChangesPane.isDiffOutdated(serializedReference));
        assertFalse(fileChangesPane.isConflictLabelDisplayed(serializedReference));
        assertFalse(fileChangesPane.isRefreshActionAvailable(serializedReference));
        assertFalse(fileChangesPane.isFixConflictActionAvailable(serializedReference));

        // check that the review is now outdated
        reviewsPane = changeRequestPage.openReviewsPane();
        reviews = reviewsPane.getReviews();
        assertEquals(1, reviews.size());
        reviewElement = reviews.get(0);
        assertTrue(reviewElement.isApproval());
        assertTrue(reviewElement.isOutdated());
    }

    @Test
    @Order(2)
    void createConflictFixWithChangeRequestVersion(TestUtils testUtils, TestReference testReference) throws Exception
    {
        String pageName = "NestedPage";
        DocumentReference nestedTestReference =
            new DocumentReference("WebHome", new SpaceReference(pageName, testReference.getLastSpaceReference()));
        String serializedReference = nestedTestReference.getLocalDocumentReference().toString();

        testUtils.loginAsSuperAdmin();
        testUtils.createPage(testReference, "Page parent of create test");

        testUtils.login(CR_USER, CR_USER);
        testUtils.gotoPage(testReference);
        ExtendedViewPage extendedViewPage = new ExtendedViewPage();
        assertFalse(extendedViewPage.hasStandardCreate());
        assertTrue(extendedViewPage.hasChangeRequestCreate());

        ExtendedCreatePage extendedCreatePage = extendedViewPage.clickChangeRequestCreate();
        assertFalse(extendedCreatePage.hasStandardCreateButton());
        assertTrue(extendedCreatePage.hasChangeRequestCreateButton());

        extendedCreatePage.getDocumentPicker().setTitle(pageName);

        ExtendedEditPage<WYSIWYGEditPage> extendedEditPage = extendedCreatePage.clickChangeRequestCreateButton();
        extendedEditPage.getEditor().setContent("Some content on the new page");
        assertFalse(extendedEditPage.hasStandardSaveButton());
        assertTrue(extendedEditPage.hasSaveAsChangeRequestButton());
        ChangeRequestSaveModal changeRequestSaveModal = extendedEditPage.clickSaveAsChangeRequest();
        changeRequestSaveModal.setChangeRequestTitle("CreateKeepCRTest");
        ChangeRequestPage changeRequestPage = changeRequestSaveModal.clickSave();

        String crUrl = changeRequestPage.getPageURL();

        FileChangesPane fileChangesPane = changeRequestPage.openFileChanges();
        assertEquals(FileChangesPane.ChangeType.CREATION, fileChangesPane.getChangeType(serializedReference));
        assertFalse(fileChangesPane.isDiffOutdated(serializedReference));
        assertFalse(fileChangesPane.isConflictLabelDisplayed(serializedReference));
        assertFalse(fileChangesPane.isRefreshActionAvailable(serializedReference));
        assertFalse(fileChangesPane.isFixConflictActionAvailable(serializedReference));

        testUtils.loginAsSuperAdmin();
        testUtils.createPage(nestedTestReference, "Some new content");

        testUtils.login(CR_USER, CR_USER);
        testUtils.gotoPage(crUrl);
        changeRequestPage = new ChangeRequestPage();

        fileChangesPane = changeRequestPage.openFileChanges();
        assertEquals(FileChangesPane.ChangeType.CREATION, fileChangesPane.getChangeType(serializedReference));
        assertTrue(fileChangesPane.isDiffOutdated(serializedReference));
        assertTrue(fileChangesPane.isConflictLabelDisplayed(serializedReference));
        assertFalse(fileChangesPane.isRefreshActionAvailable(serializedReference));
        assertTrue(fileChangesPane.isFixConflictActionAvailable(serializedReference));

        ChangeRequestConflictModal changeRequestConflictModal = fileChangesPane.clickFixConflict(serializedReference);
        assertTrue(changeRequestConflictModal
            .isOptionAvailable(ChangeRequestConflictModal.ResolutionChoice.KEEP_CHANGE_REQUEST));
        assertTrue(changeRequestConflictModal
            .isOptionAvailable(ChangeRequestConflictModal.ResolutionChoice.KEEP_PUBLISHED));
        assertFalse(changeRequestConflictModal
            .isOptionAvailable(ChangeRequestConflictModal.ResolutionChoice.CUSTOM));

        assertEquals(ChangeRequestConflictModal.ResolutionChoice.KEEP_CHANGE_REQUEST,
            changeRequestConflictModal.getCurrentChoice());
        changeRequestPage = changeRequestConflictModal.submitCurrentChoice();

        fileChangesPane = changeRequestPage.openFileChanges();
        assertEquals(FileChangesPane.ChangeType.EDITION, fileChangesPane.getChangeType(serializedReference));
        assertFalse(fileChangesPane.isDiffOutdated(serializedReference));
        assertFalse(fileChangesPane.isConflictLabelDisplayed(serializedReference));
        assertFalse(fileChangesPane.isRefreshActionAvailable(serializedReference));
        assertFalse(fileChangesPane.isFixConflictActionAvailable(serializedReference));
    }
}
