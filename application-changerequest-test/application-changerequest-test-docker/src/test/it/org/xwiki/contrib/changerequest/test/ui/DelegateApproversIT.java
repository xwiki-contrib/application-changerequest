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
import org.xwiki.contrib.changerequest.test.po.reviews.ReviewElement;
import org.xwiki.contrib.changerequest.test.po.reviews.ReviewModal;
import org.xwiki.contrib.changerequest.test.po.reviews.ReviewsPane;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.editor.WYSIWYGEditPage;

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
        // It's currently not possible to install a JAR contributing a Hibernate mapping file as an Extension. Thus
        // we need to provide the JAR inside WEB-INF/lib. See https://jira.xwiki.org/browse/XWIKI-8271
        "org.xwiki.platform:xwiki-platform-eventstream-store-hibernate",
        // The Solr store is not ready yet to be installed as extension
        "org.xwiki.platform:xwiki-platform-eventstream-store-solr"
    },
    resolveExtraJARs = true
)
class DelegateApproversIT
{
    @Test
    void delegateApprovalAndReview(TestUtils setup, TestReference testReference)
    {
        // Test fixture:
        // XWikiUser augmented with a Manager field -> will be the field for computing approvers
        // Change request configured with merge strategy allApproversNoFallback
        // 3 Users:
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

        setup.loginAsSuperAdmin();
        setup.createUser("Foo", "FooPassword", null);
        setup.createUser("Bar", "BarPassword", null);
        setup.createUser("Buz", "BuzPassword", null);

        setup.addClassProperty("XWiki", "XWikiUsers", "manager", "String");
        // We also force the approvers to use WYSIWYG editor, to avoid any problem to display the review modal.
        // This should be removed once https://jira.xwiki.org/browse/XWIKI-19281 is fixed
        setup.updateObject("XWiki", "Bar", "XWiki.XWikiUsers", 0,
            "manager", "XWiki.Buz",
            "editor", "Wysiwyg");
        setup.updateObject("XWiki", "Foo", "XWiki.XWikiUsers", 0,
            "manager", "XWiki.Bar",
            "editor", "Wysiwyg");
        setup.updateObject("XWiki", "Buz", "XWiki.XWikiUsers", 0,
            "editor", "Wysiwyg");

        setup.createPage(testReference, "Some content");
        setup.addObject(testReference, "ChangeRequest.Code.ApproversClass", "usersApprovers", "XWiki.Foo,XWiki.Bar");

        setup.updateObject(Arrays.asList("ChangeRequest", "Code"), "Configuration",
            "ChangeRequest.Code.ConfigurationClass", 0,
            "delegateEnabled", 1,
            "delegateClassPropertyList", "manager",
            "approvalStrategy", "allApproversNoFallback");

        setup.getDriver().waitUntilCondition(driver -> {
            try {
                DocumentReference barRef = new DocumentReference("xwiki", "XWiki", "Bar");
                ObjectReference objectReference =
                    new ObjectReference("ChangeRequest.Code.DelegateApproversClass[0]", barRef);
                return setup.rest().get(objectReference, false) != null;
            } catch (Exception e) {
               return false;
            }
        });

        setup.forceGuestUser();
        setup.gotoPage(testReference);
        ExtendedViewPage extendedViewPage = new ExtendedViewPage();
        assertTrue(extendedViewPage.hasStandardEditButton());

        ExtendedEditPage<WYSIWYGEditPage> editPage = extendedViewPage.clickStandardEdit();
        editPage.getEditor().setContent("Some new content");
        ChangeRequestSaveModal saveModal = editPage.clickSaveAsChangeRequest();
        saveModal.setChangeRequestTitle("Delegate test");
        ChangeRequestPage changeRequestPage = saveModal.clickSave();
        assertTrue(changeRequestPage.isReviewButtonDisplayed());
        assertFalse(changeRequestPage.isReviewButtonEnabled());

        String changeRequestUrl = setup.getDriver().getCurrentUrl();

        setup.login("Buz", "BuzPassword");
        setup.gotoPage(changeRequestUrl);
        changeRequestPage = new ChangeRequestPage();

        // Review button should be enabled for buz by Delegation
        assertTrue(changeRequestPage.isReviewButtonDisplayed());
        assertTrue(changeRequestPage.isReviewButtonEnabled());
        ReviewModal reviewModal = changeRequestPage.clickReviewButton();
        assertTrue(reviewModal.isSelectOnBehalfDisplayed());

        Select originalApproverSelector = reviewModal.getOriginalApproverSelector();
        originalApproverSelector.selectByVisibleText("Bar");
        reviewModal.selectApprove();
        reviewModal.save();

        changeRequestPage = new ChangeRequestPage();
        ReviewsPane reviewsPane = changeRequestPage.openReviewsPane();
        List<ReviewElement> reviews = reviewsPane.getReviews();
        assertEquals(1, reviews.size());

        ReviewElement reviewElement = reviews.get(0);
        assertTrue(reviewElement.isApproval());
        assertFalse(reviewElement.isOutdated());
        assertEquals("xwiki:XWiki.Buz", reviewElement.getAuthor());
        assertEquals("xwiki:XWiki.Bar", reviewElement.getOriginalApprover());

        setup.login("Bar", "BarPassword");
        setup.gotoPage(changeRequestUrl);
        changeRequestPage = new ChangeRequestPage();

        assertTrue(changeRequestPage.isReviewButtonDisplayed());
        assertTrue(changeRequestPage.isReviewButtonEnabled());
        reviewModal = changeRequestPage.clickReviewButton();
        assertTrue(reviewModal.isSelectOnBehalfDisplayed());
        originalApproverSelector = reviewModal.getOriginalApproverSelector();

        // There should be 2 options:
        //  - the current user selected (since Bar is also an approver)
        //  - the option to review on behalf of Foo
        assertEquals(2, originalApproverSelector.getOptions().size());
        WebElement selectedOption = originalApproverSelector.getFirstSelectedOption();
        assertEquals("xwiki:XWiki.Bar", selectedOption.getAttribute("value"));

        reviewModal.selectRequestChanges();
        reviewModal.save();

        changeRequestPage = new ChangeRequestPage();
        reviewsPane = changeRequestPage.openReviewsPane();
        reviews = reviewsPane.getReviews();
        assertEquals(2, reviews.size());

        reviewElement = reviews.get(0);
        assertFalse(reviewElement.isApproval());
        assertFalse(reviewElement.isOutdated());
        assertEquals("xwiki:XWiki.Bar", reviewElement.getAuthor());
        assertNull(reviewElement.getOriginalApprover());

        // previous review performed for Bar is outdated
        reviewElement = reviews.get(1);
        assertTrue(reviewElement.isApproval());
        assertTrue(reviewElement.isOutdated());
        assertEquals("xwiki:XWiki.Buz", reviewElement.getAuthor());
        assertEquals("xwiki:XWiki.Bar", reviewElement.getOriginalApprover());

        reviewModal = changeRequestPage.clickReviewButton();
        assertTrue(reviewModal.isSelectOnBehalfDisplayed());
        originalApproverSelector = reviewModal.getOriginalApproverSelector();

        // Now review for Foo
        originalApproverSelector.selectByVisibleText("Foo");
        reviewModal.selectApprove();
        reviewModal.save();

        changeRequestPage = new ChangeRequestPage();
        reviewsPane = changeRequestPage.openReviewsPane();
        reviews = reviewsPane.getReviews();
        assertEquals(3, reviews.size());

        reviewElement = reviews.get(0);
        assertTrue(reviewElement.isApproval());
        assertFalse(reviewElement.isOutdated());
        assertEquals("xwiki:XWiki.Bar", reviewElement.getAuthor());
        assertEquals("xwiki:XWiki.Foo", reviewElement.getOriginalApprover());

        // previous review should not be outdated since it doesn't concern same approver
        reviewElement = reviews.get(1);
        assertFalse(reviewElement.isApproval());
        assertFalse(reviewElement.isOutdated());
        assertEquals("xwiki:XWiki.Bar", reviewElement.getAuthor());
        assertNull(reviewElement.getOriginalApprover());

        reviewModal = changeRequestPage.clickReviewButton();
        assertTrue(reviewModal.isSelectOnBehalfDisplayed());
        originalApproverSelector = reviewModal.getOriginalApproverSelector();

        // Bar should still be selected by default
        assertEquals(2, originalApproverSelector.getOptions().size());
        selectedOption = originalApproverSelector.getFirstSelectedOption();
        assertEquals("xwiki:XWiki.Bar", selectedOption.getAttribute("value"));

        reviewModal.selectApprove();
        reviewModal.save();

        changeRequestPage = new ChangeRequestPage();
        reviewsPane = changeRequestPage.openReviewsPane();
        reviews = reviewsPane.getReviews();
        assertEquals(4, reviews.size());

        reviewElement = reviews.get(0);
        assertTrue(reviewElement.isApproval());
        assertFalse(reviewElement.isOutdated());
        assertEquals("xwiki:XWiki.Bar", reviewElement.getAuthor());
        assertNull(reviewElement.getOriginalApprover());

        reviewElement = reviews.get(1);
        assertTrue(reviewElement.isApproval());
        assertFalse(reviewElement.isOutdated());
        assertEquals("xwiki:XWiki.Bar", reviewElement.getAuthor());
        assertEquals("xwiki:XWiki.Foo", reviewElement.getOriginalApprover());

        reviewElement = reviews.get(2);
        assertFalse(reviewElement.isApproval());
        assertTrue(reviewElement.isOutdated());
        assertEquals("xwiki:XWiki.Bar", reviewElement.getAuthor());
        assertNull(reviewElement.getOriginalApprover());

        // At this point the review button should not displayed anymore, but the merge button should
        assertFalse(changeRequestPage.isReviewButtonDisplayed());
        assertTrue(changeRequestPage.isMergeButtonDisplayed());
    }
}
