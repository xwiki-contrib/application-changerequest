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

import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.xwiki.contrib.changerequest.test.po.ChangeRequestPage;
import org.xwiki.contrib.changerequest.test.po.ChangeRequestSaveModal;
import org.xwiki.contrib.changerequest.test.po.ExtendedEditPage;
import org.xwiki.contrib.changerequest.test.po.ExtendedViewPage;
import org.xwiki.contrib.changerequest.test.po.filechanges.FileChangesPane;
import org.xwiki.contrib.changerequest.test.po.filechanges.FilechangesLiveDataElement;
import org.xwiki.contrib.changerequest.test.po.reviews.ReviewContainer;
import org.xwiki.contrib.changerequest.test.po.reviews.ReviewElement;
import org.xwiki.contrib.changerequest.test.po.reviews.ReviewsPane;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.editor.WikiEditPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for checking the delegate approver mechanism.
 *
 * @version $Id$
 * @since 0.13
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
class DelegateApproversIT
{
    static final String TEST_PREFIX = "DelegateApproversIT_";
    static final String FOO_USER = TEST_PREFIX + "Foo";
    static final String BAR_USER = TEST_PREFIX + "Bar";
    static final String BUZ_USER = TEST_PREFIX + "Buz";
    static final String EDITOR = TEST_PREFIX + "Editor";

    @Test
    void delegateApprovalAndReview(TestUtils setup, TestReference testReference)
    {
        // Test fixture:
        // XWikiUser augmented with a Manager field -> will be the field for computing approvers
        // Change request configured with merge strategy allApproversNoFallback
        // 4 Users:
        //   * Editor
        //   * Foo (manager: Bar)
        //   * Bar (manager: Buz)
        //   * Buz (No manager)
        // Page test with 2 approvers: Foo and Bar

        // Scenario:
        //   Check enabling the delegate approval strategy, and wait for the delegate xobjects to be created
        //   Change request created by guest
        //     * Buz approves it on behalf of Bar
        //     * Bar ask for changes for himself -> invalidate previous review
        //     * Bar approves it on behalf of Foo
        //     * Bar approves it for himself -> should invalidate its own private review
        //    -> page should be mergeable only then
        //     * New edition added on the page
        //    -> all approvals outdated and status back to "ready for review"

        String serializedReference = testReference.getLocalDocumentReference().toString();
        setup.loginAsSuperAdmin();
        setup.createUser(EDITOR, EDITOR, null);
        setup.createUser(FOO_USER, FOO_USER, null);
        setup.createUser(BAR_USER, BAR_USER, null);
        setup.createUser(BUZ_USER, BUZ_USER, null);

        setup.addClassProperty("XWiki", "XWikiUsers", "manager", "String");
        // We also force the approvers to use WYSIWYG editor, to avoid any problem to display the review modal.
        // This should be removed once https://jira.xwiki.org/browse/XWIKI-19281 is fixed
        setup.updateObject("XWiki", BAR_USER, "XWiki.XWikiUsers", 0,
            "manager", "XWiki." + BUZ_USER,
            "editor", "Wysiwyg");
        setup.updateObject("XWiki", FOO_USER, "XWiki.XWikiUsers", 0,
            "manager", "XWiki." + BAR_USER,
            "editor", "Wysiwyg");
        setup.updateObject("XWiki", BUZ_USER, "XWiki.XWikiUsers", 0,
            "editor", "Wysiwyg");

        setup.createPage(testReference, "Some content");
        setup.addObject(testReference, "ChangeRequest.Code.ApproversClass", "usersApprovers",
            String.format("XWiki.%s,XWiki.%s", FOO_USER, BAR_USER));

        setup.updateObject(Arrays.asList("ChangeRequest", "Code"), "Configuration",
            "ChangeRequest.Code.ConfigurationClass", 0,
            "minimumApprovers", 0,
            "delegateEnabled", 1,
            "mergeUser", "",
            "delegateClassPropertyList", "manager",
            "approvalStrategy", "allApproversNoFallback");

        setup.getDriver().waitUntilCondition(driver -> {
            try {
                DocumentReference barRef = new DocumentReference("xwiki", "XWiki", BAR_USER);
                ObjectReference objectReference =
                    new ObjectReference("ChangeRequest.Code.DelegateApproversClass[0]", barRef);
                return setup.rest().get(objectReference, false) != null;
            } catch (Exception e) {
               return false;
            }
        });

        setup.login(EDITOR, EDITOR);
        setup.gotoPage(testReference);
        ExtendedViewPage extendedViewPage = new ExtendedViewPage();
        assertTrue(extendedViewPage.hasStandardEditButton());

        ExtendedEditPage<WikiEditPage> editPage = extendedViewPage.clickStandardEdit();
        editPage.getWrappedEditor().setContent("Some new content");
        ChangeRequestSaveModal saveModal = editPage.clickSaveAsChangeRequest();
        saveModal.setChangeRequestTitle("Delegate test");
        ChangeRequestPage changeRequestPage = saveModal.clickSave();
        assertTrue(changeRequestPage.isReviewButtonDisplayed());
        assertFalse(changeRequestPage.isReviewButtonEnabled());

        String changeRequestUrl = setup.getDriver().getCurrentUrl();

        setup.login(BUZ_USER, BUZ_USER);
        setup.gotoPage(changeRequestUrl);
        changeRequestPage = new ChangeRequestPage();

        // Review button should be enabled for buz by Delegation
        assertTrue(changeRequestPage.isReviewButtonDisplayed());
        assertTrue(changeRequestPage.isReviewButtonEnabled());
        ReviewContainer reviewContainer = changeRequestPage.clickReviewButton();
        assertTrue(reviewContainer.isSelectOnBehalfDisplayed());

        Select originalApproverSelector = reviewContainer.getOriginalApproverSelector();
        originalApproverSelector.selectByVisibleText(BAR_USER);
        reviewContainer.selectApprove();
        reviewContainer.save();

        changeRequestPage = new ChangeRequestPage();
        ReviewsPane reviewsPane = changeRequestPage.openReviewsPane();
        List<ReviewElement> reviews = reviewsPane.getReviews();
        assertEquals(1, reviews.size());

        ReviewElement reviewElement = reviews.get(0);
        assertTrue(reviewElement.isApproval());
        assertFalse(reviewElement.isOutdated());
        assertEquals("xwiki:XWiki." + BUZ_USER, reviewElement.getAuthor());
        assertEquals("xwiki:XWiki." + BAR_USER, reviewElement.getOriginalApprover());

        setup.login(BAR_USER, BAR_USER);
        setup.gotoPage(changeRequestUrl);
        changeRequestPage = new ChangeRequestPage();

        assertTrue(changeRequestPage.isReviewButtonDisplayed());
        assertTrue(changeRequestPage.isReviewButtonEnabled());
        reviewContainer = changeRequestPage.clickReviewButton();
        assertTrue(reviewContainer.isSelectOnBehalfDisplayed());
        originalApproverSelector = reviewContainer.getOriginalApproverSelector();

        // There should be 2 options:
        //  - the current user selected (since Bar is also an approver)
        //  - the option to review on behalf of Foo
        assertEquals(2, originalApproverSelector.getOptions().size());
        WebElement selectedOption = originalApproverSelector.getFirstSelectedOption();
        assertEquals("xwiki:XWiki." + BAR_USER, selectedOption.getAttribute("value"));

        reviewContainer.selectRequestChanges();
        reviewContainer.save();

        changeRequestPage = new ChangeRequestPage();
        reviewsPane = changeRequestPage.openReviewsPane();
        reviews = reviewsPane.getReviews();
        assertEquals(2, reviews.size());

        reviewElement = reviews.get(0);
        assertFalse(reviewElement.isApproval());
        assertFalse(reviewElement.isOutdated());
        assertEquals("xwiki:XWiki." + BAR_USER, reviewElement.getAuthor());
        assertNull(reviewElement.getOriginalApprover());

        // previous review performed for Bar is outdated
        reviewElement = reviews.get(1);
        assertTrue(reviewElement.isApproval());
        assertTrue(reviewElement.isOutdated());
        assertEquals("xwiki:XWiki." + BUZ_USER, reviewElement.getAuthor());
        assertEquals("xwiki:XWiki." + BAR_USER, reviewElement.getOriginalApprover());

        reviewContainer = changeRequestPage.clickReviewButton();
        assertTrue(reviewContainer.isSelectOnBehalfDisplayed());
        originalApproverSelector = reviewContainer.getOriginalApproverSelector();

        // Now review for Foo
        originalApproverSelector.selectByVisibleText(FOO_USER);
        reviewContainer.selectApprove();
        reviewContainer.save();

        changeRequestPage = new ChangeRequestPage();
        reviewsPane = changeRequestPage.openReviewsPane();
        reviews = reviewsPane.getReviews();
        assertEquals(3, reviews.size());

        reviewElement = reviews.get(0);
        assertTrue(reviewElement.isApproval());
        assertFalse(reviewElement.isOutdated());
        assertEquals("xwiki:XWiki." + BAR_USER, reviewElement.getAuthor());
        assertEquals("xwiki:XWiki." + FOO_USER, reviewElement.getOriginalApprover());

        // previous review should not be outdated since it doesn't concern same approver
        reviewElement = reviews.get(1);
        assertFalse(reviewElement.isApproval());
        assertFalse(reviewElement.isOutdated());
        assertEquals("xwiki:XWiki." + BAR_USER, reviewElement.getAuthor());
        assertNull(reviewElement.getOriginalApprover());

        reviewContainer = changeRequestPage.clickReviewButton();
        assertTrue(reviewContainer.isSelectOnBehalfDisplayed());
        originalApproverSelector = reviewContainer.getOriginalApproverSelector();

        // Bar should still be selected by default
        assertEquals(2, originalApproverSelector.getOptions().size());
        selectedOption = originalApproverSelector.getFirstSelectedOption();
        assertEquals("xwiki:XWiki." + BAR_USER, selectedOption.getAttribute("value"));

        reviewContainer.selectApprove();
        reviewContainer.save();

        changeRequestPage = new ChangeRequestPage();
        reviewsPane = changeRequestPage.openReviewsPane();
        reviews = reviewsPane.getReviews();
        assertEquals(4, reviews.size());

        reviewElement = reviews.get(0);
        assertTrue(reviewElement.isApproval());
        assertFalse(reviewElement.isOutdated());
        assertEquals("xwiki:XWiki." + BAR_USER, reviewElement.getAuthor());
        assertNull(reviewElement.getOriginalApprover());

        reviewElement = reviews.get(1);
        assertTrue(reviewElement.isApproval());
        assertFalse(reviewElement.isOutdated());
        assertEquals("xwiki:XWiki." + BAR_USER, reviewElement.getAuthor());
        assertEquals("xwiki:XWiki." + FOO_USER, reviewElement.getOriginalApprover());

        reviewElement = reviews.get(2);
        assertFalse(reviewElement.isApproval());
        assertTrue(reviewElement.isOutdated());
        assertEquals("xwiki:XWiki." + BAR_USER, reviewElement.getAuthor());
        assertNull(reviewElement.getOriginalApprover());

        // At this point the review button should not displayed anymore, but the merge button should
        assertFalse(changeRequestPage.isReviewButtonDisplayed());
        assertTrue(changeRequestPage.isMergeButtonDisplayed());
        assertEquals("Ready for publication", changeRequestPage.getStatusLabel());

        // Perform new changes: all reviews should now be outdated
        // Status should be back to "ready for review"
        setup.login(EDITOR, EDITOR);
        setup.gotoPage(changeRequestUrl);
        changeRequestPage = new ChangeRequestPage();
        FileChangesPane fileChangesPane = changeRequestPage.openFileChanges();
        FilechangesLiveDataElement.FilechangesRowElement fileChangeWithReference =
            fileChangesPane.getFileChangesListLiveData().getFileChangeWithReference(serializedReference);
        assertTrue(fileChangeWithReference.isEditActionAvailable());

        editPage = fileChangeWithReference.clickEdit();
        editPage.getWrappedEditor().setContent("Some new content with some new change");
        changeRequestPage = editPage.clickSaveAsChangeRequestInExistingCR();

        reviewsPane = changeRequestPage.openReviewsPane();
        reviews = reviewsPane.getReviews();
        assertEquals(4, reviews.size());

        reviewElement = reviews.get(0);
        assertTrue(reviewElement.isApproval());
        assertTrue(reviewElement.isOutdated());
        assertEquals("xwiki:XWiki." + BAR_USER, reviewElement.getAuthor());
        assertNull(reviewElement.getOriginalApprover());

        reviewElement = reviews.get(1);
        assertTrue(reviewElement.isApproval());
        assertTrue(reviewElement.isOutdated());
        assertEquals("xwiki:XWiki." + BAR_USER, reviewElement.getAuthor());
        assertEquals("xwiki:XWiki." + FOO_USER, reviewElement.getOriginalApprover());

        reviewElement = reviews.get(2);
        assertFalse(reviewElement.isApproval());
        assertTrue(reviewElement.isOutdated());
        assertEquals("xwiki:XWiki." + BAR_USER, reviewElement.getAuthor());
        assertNull(reviewElement.getOriginalApprover());

        assertTrue(changeRequestPage.isReviewButtonDisplayed());
        assertFalse(changeRequestPage.isMergeButtonDisplayed());
        assertEquals("Ready for review", changeRequestPage.getStatusLabel());
    }
}
