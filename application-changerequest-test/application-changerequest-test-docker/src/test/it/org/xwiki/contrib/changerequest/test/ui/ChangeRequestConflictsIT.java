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

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Keys;
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
import org.xwiki.test.ui.po.SuggestInputElement;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.WYSIWYGEditPage;
import org.xwiki.test.ui.po.editor.WikiEditPage;

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
        // The Solr store is not ready yet to be installed as extension
        "org.xwiki.platform:xwiki-platform-eventstream-store-solr"
    },
    resolveExtraJARs = true
)
class ChangeRequestConflictsIT
{
    private static final String TEST_USER_PREFIX = "CRConflictTest";

    private static final String CR_USER = TEST_USER_PREFIX + "CRUser";
    private static final String CR_APPROVER = TEST_USER_PREFIX + "Approver";

    private static final String FOO = TEST_USER_PREFIX + "Foo";
    private static final String BAR = TEST_USER_PREFIX + "Bar";
    private static final String BUZ = TEST_USER_PREFIX + "Buz";
    private static final String MERGE_USER = TEST_USER_PREFIX + "MergeUser";

    @BeforeAll
    void beforeAll(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
        setup.createUser(CR_USER, CR_USER, null);
        setup.setGlobalRights("", CR_USER, "edit", false);
        setup.setGlobalRights("", CR_USER, "delete", false);

        setup.createUser(FOO, FOO, null);
        setup.createUser(BAR, BAR, null);
        setup.createUser(BUZ, BUZ, null);

        setup.createUser(MERGE_USER, MERGE_USER, null);
        setup.setGlobalRights("", MERGE_USER, "admin", true);

        setup.createUser(CR_APPROVER, CR_APPROVER, null);
        // We force the approver to use WYSIWYG editor, to avoid any problem to display the review modal.
        // This should be removed once https://jira.xwiki.org/browse/XWIKI-19281 is fixed
        setup.updateObject("XWiki", CR_APPROVER, "XWiki.XWikiUsers", 0, "editor", "Wysiwyg");
        setup.updateObject("XWiki", FOO, "XWiki.XWikiUsers", 0, "editor", "Wysiwyg");
        setup.updateObject("XWiki", BAR, "XWiki.XWikiUsers", 0, "editor", "Wysiwyg");
        setup.updateObject("XWiki", BUZ, "XWiki.XWikiUsers", 0, "editor", "Wysiwyg");

        setup.setGlobalRights("", CR_APPROVER, "edit", false);
        setup.setGlobalRights("", CR_APPROVER, "crapprove", true);

        // Ensure to use right strategy
        setup.updateObject(Arrays.asList("ChangeRequest", "Code"), "Configuration",
            "ChangeRequest.Code.ConfigurationClass", 0,
            "minimumApprovers", 0,
            "mergeUser", MERGE_USER,
            "approvalStrategy", "onlyapproved");
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

    /**
     * Purpose of this test is not to create a conflict, but to test the refresh content feature, especially with the
     * impact on approvers.
     *
     * Fixture:
     *   - Page created with approvers Foo and Bar
     *   - CR_USER creates CR1 with changes on title
     *   - CR_USER creates CR2 with changes on content
     *   - CR_USER creates CR3 with changes of approvers to replace Bar by Buz
     *
     * Scenario:
     *   - Foo and Bar approves all 3 CRs
     *   - CR_USER merge CR1 first: CR2 and CR3 are still mergeable, CR_USER is able to refresh the content
     *   - CR_USER refreshes content on CR3 -> approvals are now invalidated on CR3
     *   - Foo and Bar approves again CR3
     *   - CR_USER merges CR3: CR2 is only ready for review, approver list has been updated review from Bar
     *     is invalidated, review from Foo is kept
     *   - Buz approves and merge CR2
     */
    @Test
    @Order(3)
    void refreshContent(TestUtils testUtils, TestReference testReference) throws Exception
    {
        testUtils.loginAsSuperAdmin();
        // We use the allapprovers strategy here.
        testUtils.updateObject(Arrays.asList("ChangeRequest", "Code"), "Configuration",
            "ChangeRequest.Code.ConfigurationClass", 0,
            "approvalStrategy", "allapprovers");
        String serializedReference = testReference.getLocalDocumentReference().toString();
        
        testUtils.createPage(testReference, "Some content");
        testUtils.addObject(testReference, "ChangeRequest.Code.ApproversClass", "usersApprovers",
            String.format("XWiki.%s,XWiki.%s", FOO, BAR));

        testUtils.login(CR_USER, CR_USER);

        // Creation of CR1
        testUtils.gotoPage(testReference);

        ExtendedViewPage extendedViewPage = new ExtendedViewPage();
        ExtendedEditPage<WikiEditPage> editPage = extendedViewPage.clickChangeRequestEdit();

        editPage.getEditor().setTitle("A new title");
        ChangeRequestSaveModal saveModal = editPage.clickSaveAsChangeRequest();
        saveModal.setChangeRequestTitle("CR1");

        ChangeRequestPage changeRequestPage = saveModal.clickSave();
        String cr1Url = testUtils.getDriver().getCurrentUrl();

        assertEquals("Ready for review", changeRequestPage.getStatusLabel());
        ReviewsPane reviewsPane = changeRequestPage.openReviewsPane();
        assertEquals(List.of(FOO, BAR), reviewsPane.getListOfApprovers());

        // Creation of CR2
        testUtils.gotoPage(testReference);

        extendedViewPage = new ExtendedViewPage();
        editPage = extendedViewPage.clickChangeRequestEdit();

        editPage.getEditor().setContent("A new content");
        saveModal = editPage.clickSaveAsChangeRequest();
        saveModal.setChangeRequestTitle("CR2");

        changeRequestPage = saveModal.clickSave();
        String cr2Url = testUtils.getDriver().getCurrentUrl();

        assertEquals("Ready for review", changeRequestPage.getStatusLabel());
        reviewsPane = changeRequestPage.openReviewsPane();
        assertEquals(List.of(FOO, BAR), reviewsPane.getListOfApprovers());

        // Creation of CR3
        testUtils.gotoPage(testReference);

        extendedViewPage = new ExtendedViewPage();
        saveModal = extendedViewPage.clickManageApproversWithoutEditRight();

        assertTrue(saveModal.isApproversSelectionDisplayed());
        SuggestInputElement usersApproverSelector = saveModal.getUsersApproverSelector();
        List<SuggestInputElement.SuggestionElement> selectedSuggestions =
            usersApproverSelector.getSelectedSuggestions();

        assertEquals(2, selectedSuggestions.size());
        assertEquals("XWiki." + BAR, selectedSuggestions.get(1).getValue());
        selectedSuggestions.get(1).delete();

        usersApproverSelector.sendKeys("XWiki." + BUZ).waitForSuggestions().sendKeys(Keys.ENTER);

        saveModal.setChangeRequestTitle("CR3");
        changeRequestPage = saveModal.clickSave();
        String cr3Url = testUtils.getDriver().getCurrentUrl();

        assertEquals("Ready for review", changeRequestPage.getStatusLabel());
        reviewsPane = changeRequestPage.openReviewsPane();
        assertEquals(List.of(FOO, BAR), reviewsPane.getListOfApprovers());

        // Review of CR1, CR2 and CR3 by Foo
        testUtils.login(FOO, FOO);

        testUtils.gotoPage(cr1Url);
        changeRequestPage = new ChangeRequestPage();
        assertTrue(changeRequestPage.isReviewButtonDisplayed());
        assertTrue(changeRequestPage.isReviewButtonEnabled());

        ReviewModal reviewModal = changeRequestPage.clickReviewButton();
        reviewModal.selectApprove();
        reviewModal.save();

        testUtils.gotoPage(cr2Url);
        changeRequestPage = new ChangeRequestPage();
        assertTrue(changeRequestPage.isReviewButtonDisplayed());
        assertTrue(changeRequestPage.isReviewButtonEnabled());

        reviewModal = changeRequestPage.clickReviewButton();
        reviewModal.selectApprove();
        reviewModal.save();

        testUtils.gotoPage(cr3Url);
        changeRequestPage = new ChangeRequestPage();
        assertTrue(changeRequestPage.isReviewButtonDisplayed());
        assertTrue(changeRequestPage.isReviewButtonEnabled());

        reviewModal = changeRequestPage.clickReviewButton();
        reviewModal.selectApprove();
        reviewModal.save();

        // Review of CR1, CR2 and CR3 by Bar
        testUtils.login(BAR, BAR);

        testUtils.gotoPage(cr1Url);
        changeRequestPage = new ChangeRequestPage();
        assertTrue(changeRequestPage.isReviewButtonDisplayed());
        assertTrue(changeRequestPage.isReviewButtonEnabled());

        reviewModal = changeRequestPage.clickReviewButton();
        reviewModal.selectApprove();
        reviewModal.save();

        testUtils.gotoPage(cr2Url);
        changeRequestPage = new ChangeRequestPage();
        assertTrue(changeRequestPage.isReviewButtonDisplayed());
        assertTrue(changeRequestPage.isReviewButtonEnabled());

        reviewModal = changeRequestPage.clickReviewButton();
        reviewModal.selectApprove();
        reviewModal.save();

        testUtils.gotoPage(cr3Url);
        changeRequestPage = new ChangeRequestPage();
        assertTrue(changeRequestPage.isReviewButtonDisplayed());
        assertTrue(changeRequestPage.isReviewButtonEnabled());

        reviewModal = changeRequestPage.clickReviewButton();
        reviewModal.selectApprove();
        reviewModal.save();

        // Check status with CR_USER
        testUtils.login(CR_USER, CR_USER);

        testUtils.gotoPage(cr1Url);
        changeRequestPage = new ChangeRequestPage();

        assertEquals("Ready for merging", changeRequestPage.getStatusLabel());

        // Since CR_USER is the author and not an approver he cannot merge
        assertTrue(changeRequestPage.isMergeButtonDisplayed());
        assertFalse(changeRequestPage.isMergeButtonEnabled());

        testUtils.login(FOO, FOO);
        testUtils.gotoPage(cr1Url);
        changeRequestPage = new ChangeRequestPage();

        assertTrue(changeRequestPage.isMergeButtonDisplayed());
        assertTrue(changeRequestPage.isMergeButtonEnabled());

        changeRequestPage = changeRequestPage.clickMergeButton();
        ViewPage viewPage = testUtils.gotoPage(testReference);
        assertEquals("A new title", viewPage.getDocumentTitle());

        testUtils.gotoPage(cr3Url);
        changeRequestPage = new ChangeRequestPage();

        // check that the reviews are not invalidated
        reviewsPane = changeRequestPage.openReviewsPane();
        List<ReviewElement> reviews = reviewsPane.getReviews();

        assertEquals(2, reviews.size());

        ReviewElement reviewElement = reviews.get(0);
        assertFalse(reviewElement.isOutdated());
        assertTrue(reviewElement.isApproval());
        assertEquals("xwiki:XWiki." + BAR, reviewElement.getAuthor());

        reviewElement = reviews.get(1);
        assertFalse(reviewElement.isOutdated());
        assertTrue(reviewElement.isApproval());
        assertEquals("xwiki:XWiki." + FOO, reviewElement.getAuthor());

        // check that it's still ready for merging
        assertEquals("Ready for merging", changeRequestPage.getStatusLabel());
        assertTrue(changeRequestPage.isMergeButtonDisplayed());
        assertTrue(changeRequestPage.isMergeButtonEnabled());

        // Check that the diff is outdated, and request for content refresh
        FileChangesPane fileChangesPane = changeRequestPage.openFileChanges();
        assertTrue(fileChangesPane.isDiffOutdated(serializedReference));

        // only author is able to refresh
        assertFalse(fileChangesPane.isRefreshActionAvailable(serializedReference));

        testUtils.login(CR_USER, CR_USER);

        testUtils.gotoPage(cr3Url);
        changeRequestPage = new ChangeRequestPage();
        fileChangesPane = changeRequestPage.openFileChanges();
        assertTrue(fileChangesPane.isRefreshActionAvailable(serializedReference));
        changeRequestPage = fileChangesPane.clickRefresh(serializedReference);

        // check that now the reviews are invalidated
        reviewsPane = changeRequestPage.openReviewsPane();
        reviews = reviewsPane.getReviews();

        assertEquals(2, reviews.size());

        reviewElement = reviews.get(0);
        assertTrue(reviewElement.isOutdated());
        assertTrue(reviewElement.isApproval());
        assertEquals("xwiki:XWiki." + BAR, reviewElement.getAuthor());

        reviewElement = reviews.get(1);
        assertTrue(reviewElement.isOutdated());
        assertTrue(reviewElement.isApproval());
        assertEquals("xwiki:XWiki." + FOO, reviewElement.getAuthor());

        // check that it's now back to ready for review
        assertEquals("Ready for review", changeRequestPage.getStatusLabel());
        assertFalse(changeRequestPage.isMergeButtonDisplayed());
        assertTrue(changeRequestPage.isReviewButtonDisplayed());

        // Check that the diff is not outdated anymore
        fileChangesPane = changeRequestPage.openFileChanges();
        assertFalse(fileChangesPane.isDiffOutdated(serializedReference));
        assertFalse(fileChangesPane.isRefreshActionAvailable(serializedReference));

        // Approve back with Foo and Bar
        testUtils.login(FOO, FOO);

        testUtils.gotoPage(cr3Url);
        changeRequestPage = new ChangeRequestPage();
        assertTrue(changeRequestPage.isReviewButtonDisplayed());
        assertTrue(changeRequestPage.isReviewButtonEnabled());

        reviewModal = changeRequestPage.clickReviewButton();
        reviewModal.selectApprove();
        reviewModal.save();

        testUtils.login(BAR, BAR);

        testUtils.gotoPage(cr3Url);
        changeRequestPage = new ChangeRequestPage();
        assertTrue(changeRequestPage.isReviewButtonDisplayed());
        assertTrue(changeRequestPage.isReviewButtonEnabled());

        reviewModal = changeRequestPage.clickReviewButton();
        reviewModal.selectApprove();
        reviewModal.save();

        changeRequestPage = new ChangeRequestPage();

        assertEquals("Ready for merging", changeRequestPage.getStatusLabel());
        assertTrue(changeRequestPage.isMergeButtonDisplayed());
        assertTrue(changeRequestPage.isMergeButtonEnabled());

        // Merge CR3
        changeRequestPage = changeRequestPage.clickMergeButton();

        testUtils.login(CR_USER, CR_USER);
        viewPage = testUtils.gotoPage(testReference);
        assertEquals("A new title", viewPage.getDocumentTitle());

        // check approvers list
        extendedViewPage = new ExtendedViewPage();
        saveModal = extendedViewPage.clickManageApproversWithoutEditRight();
        assertTrue(saveModal.isApproversSelectionDisplayed());

        usersApproverSelector = saveModal.getUsersApproverSelector();

        assertEquals(List.of("XWiki." + FOO, "XWiki." + BUZ), usersApproverSelector.getValues());
        saveModal.close();

        testUtils.gotoPage(cr2Url);
        changeRequestPage = new ChangeRequestPage();

        // check that it's now back to ready for review
        assertEquals("Ready for review", changeRequestPage.getStatusLabel());
        assertFalse(changeRequestPage.isMergeButtonDisplayed());
        assertTrue(changeRequestPage.isReviewButtonDisplayed());

        // check that now the review of Bar only is invalidated
        reviewsPane = changeRequestPage.openReviewsPane();
        reviews = reviewsPane.getReviews();

        assertEquals(2, reviews.size());

        reviewElement = reviews.get(0);
        assertTrue(reviewElement.isOutdated());
        assertTrue(reviewElement.isApproval());
        assertEquals("xwiki:XWiki." + BAR, reviewElement.getAuthor());

        reviewElement = reviews.get(1);
        assertFalse(reviewElement.isOutdated());
        assertTrue(reviewElement.isApproval());
        assertEquals("xwiki:XWiki." + FOO, reviewElement.getAuthor());

        // Check the list of approvers is updated
        assertEquals(List.of(FOO, BUZ), reviewsPane.getListOfApprovers());

        // Check that the diff is outdated
        fileChangesPane = changeRequestPage.openFileChanges();
        assertTrue(fileChangesPane.isDiffOutdated(serializedReference));
        assertTrue(fileChangesPane.isRefreshActionAvailable(serializedReference));

        testUtils.login(BUZ, BUZ);

        testUtils.gotoPage(cr2Url);
        changeRequestPage = new ChangeRequestPage();
        assertTrue(changeRequestPage.isReviewButtonDisplayed());
        assertTrue(changeRequestPage.isReviewButtonEnabled());

        reviewModal = changeRequestPage.clickReviewButton();
        reviewModal.selectApprove();
        reviewModal.save();

        changeRequestPage = new ChangeRequestPage();
        assertEquals("Ready for merging", changeRequestPage.getStatusLabel());
        assertTrue(changeRequestPage.isMergeButtonDisplayed());
        assertTrue(changeRequestPage.isMergeButtonEnabled());

        // Merge CR2
        changeRequestPage = changeRequestPage.clickMergeButton();
        viewPage = testUtils.gotoPage(testReference);
        assertEquals("A new title", viewPage.getDocumentTitle());
        assertEquals("A new content", viewPage.getContent());
    }
}
