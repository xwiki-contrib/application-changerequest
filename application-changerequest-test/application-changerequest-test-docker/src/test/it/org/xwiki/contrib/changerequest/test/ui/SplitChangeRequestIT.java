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
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.xwiki.contrib.changerequest.test.po.ChangeRequestLiveDataElement;
import org.xwiki.contrib.changerequest.test.po.ChangeRequestPage;
import org.xwiki.contrib.changerequest.test.po.ChangeRequestSaveModal;
import org.xwiki.contrib.changerequest.test.po.ExtendedEditPage;
import org.xwiki.contrib.changerequest.test.po.ExtendedViewPage;
import org.xwiki.contrib.changerequest.test.po.FileChangesPane;
import org.xwiki.contrib.changerequest.test.po.reviews.ReviewElement;
import org.xwiki.contrib.changerequest.test.po.reviews.ReviewModal;
import org.xwiki.contrib.changerequest.test.po.reviews.ReviewsPane;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.SuggestInputElement;
import org.xwiki.test.ui.po.editor.WYSIWYGEditPage;
import org.xwiki.test.ui.po.editor.WikiEditPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the split change request features.
 *
 * @version $Id$
 * @since 0.14
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
class SplitChangeRequestIT
{
    static final String TEST_PREFIX = "SplitCRIT_";
    static final String FOO_USER = TEST_PREFIX + "Foo";
    static final String BAR_USER = TEST_PREFIX + "Bar";
    static final String BAZ_USER = TEST_PREFIX + "Baz";
    static final String BUZ_USER = TEST_PREFIX + "Buz";
    
    static final String XWIKI_USER_PREFIX = "xwiki:XWiki.";

    @Test
    void splitChangeRequest(TestUtils setup, TestReference testReference)
    {
        // Fixture:
        // 4 users:
        //   - Foo
        //   - Bar
        //   - Baz
        //   - Buz
        // 2 pages:
        //    - FooBarPage -> Foo and Bar are approvers
        //    - BuzBazPage -> Buz and Baz are approvers
        // A CR is created including changes from the 2 pages
        // Reviews are produced by Foo and Buz
        // Discussions are also produced
        // Rights are then changed on FooBarPage and BuzBazPage
        // (Foo allowed to view the first one and forbidden to view the second one)
        // Check the splitted CR

        setup.loginAsSuperAdmin();
        setup.createUser(FOO_USER, FOO_USER, null);
        setup.createUser(BAR_USER, BAR_USER, null);
        setup.createUser(BAZ_USER, BAZ_USER, null);
        setup.createUser(BUZ_USER, BUZ_USER, null);

        // We force the approver to use WYSIWYG editor, to avoid any problem to display the review modal.
        // This should be removed once https://jira.xwiki.org/browse/XWIKI-19281 is fixed
        setup.updateObject("XWiki", FOO_USER, "XWiki.XWikiUsers", 0, "editor", "Wysiwyg");
        setup.updateObject("XWiki", BAR_USER, "XWiki.XWikiUsers", 0, "editor", "Wysiwyg");
        setup.updateObject("XWiki", BAZ_USER, "XWiki.XWikiUsers", 0, "editor", "Wysiwyg");
        setup.updateObject("XWiki", BUZ_USER, "XWiki.XWikiUsers", 0, "editor", "Wysiwyg");

        // Ensure to use right strategy
        setup.updateObject(Arrays.asList("ChangeRequest", "Code"), "Configuration",
            "ChangeRequest.Code.ConfigurationClass", 0,
            "minimumApprovers", 0,
            "mergeUser", "",
            "approvalStrategy", "onlyapproved");

        setup.createPage(testReference, "Holder for test pages of Split CR");

        DocumentReference fooBarPageRef = new DocumentReference("FooBarPage", testReference.getLastSpaceReference());
        DocumentReference buzBazPageRef = new DocumentReference("BuzBazPage", testReference.getLastSpaceReference());

        setup.createPage(fooBarPageRef, "Foo Bar page");
        setup.createPage(buzBazPageRef, "Buz Baz Page");

        setup.addObject(fooBarPageRef, "ChangeRequest.Code.ApproversClass", "usersApprovers",
            String.format("XWiki.%s,XWiki.%s", FOO_USER, BAR_USER));
        setup.addObject(buzBazPageRef, "ChangeRequest.Code.ApproversClass", "usersApprovers",
            String.format("XWiki.%s,XWiki.%s", BUZ_USER, BAZ_USER));

        setup.gotoPage(fooBarPageRef);
        ExtendedViewPage viewPage = new ExtendedViewPage();
        ExtendedEditPage<WikiEditPage> editPage = viewPage.clickStandardEdit();
        editPage.getEditor().setContent("Some changes");
        ChangeRequestSaveModal saveModal = editPage.clickSaveAsChangeRequest();
        saveModal.setChangeRequestTitle("SplitTest");
        ChangeRequestPage changeRequestPage = saveModal.clickSave();
        String changeRequestURL = setup.getDriver().getCurrentUrl();

        setup.gotoPage(buzBazPageRef);
        viewPage = new ExtendedViewPage();
        editPage = viewPage.clickStandardEdit();
        editPage.getEditor().setContent("Some other changes");
        saveModal = editPage.clickSaveAsChangeRequest();
        saveModal.openAddChangesToExistingChangeRequestCollapse();
        SuggestInputElement.SuggestionElement selectedCR = saveModal.selectExistingChangeRequest("SplitTest");
        assertEquals("SplitTest", selectedCR.getLabel());
        changeRequestPage = saveModal.clickSave();

        FileChangesPane fileChangesPane = changeRequestPage.openFileChanges();
        List<String> listOfChangedFiles = fileChangesPane.getListOfChangedFiles();

        assertEquals(List.of(fooBarPageRef.toString(), buzBazPageRef.toString()), listOfChangedFiles);

        ReviewsPane reviewsPane = changeRequestPage.openReviewsPane();
        List<String> listOfApprovers = reviewsPane.getListOfApprovers();
        assertEquals(List.of(FOO_USER, BAR_USER, BUZ_USER, BAZ_USER), listOfApprovers);

        setup.login(FOO_USER, FOO_USER);
        setup.gotoPage(changeRequestURL);
        changeRequestPage = new ChangeRequestPage();
        assertTrue(changeRequestPage.isReviewButtonDisplayed());
        assertTrue(changeRequestPage.isReviewButtonEnabled());

        ReviewModal reviewModal = changeRequestPage.clickReviewButton();
        reviewModal.selectRequestChanges();
        reviewModal.setComment("Not good enough yet.");
        reviewModal.save();

        setup.login(BUZ_USER, BUZ_USER);
        setup.gotoPage(changeRequestURL);
        changeRequestPage = new ChangeRequestPage();
        assertTrue(changeRequestPage.isReviewButtonDisplayed());
        assertTrue(changeRequestPage.isReviewButtonEnabled());

        reviewModal = changeRequestPage.clickReviewButton();
        reviewModal.selectApprove();
        reviewModal.setComment("LGTM");
        reviewModal.save();

        // TODO: Improve the test for adding comments
        setup.loginAsSuperAdmin();
        setup.gotoPage(changeRequestURL);
        changeRequestPage = new ChangeRequestPage();
        reviewsPane = changeRequestPage.openReviewsPane();
        List<ReviewElement> reviews = reviewsPane.getReviews();
        assertEquals(2, reviews.size());

        ReviewElement reviewElement = reviews.get(0);
        assertTrue(reviewElement.isApproval());
        assertFalse(reviewElement.isOutdated());
        assertEquals(XWIKI_USER_PREFIX + BUZ_USER, reviewElement.getAuthor());

        reviewElement = reviews.get(1);
        assertFalse(reviewElement.isApproval());
        assertFalse(reviewElement.isOutdated());
        assertEquals(XWIKI_USER_PREFIX + FOO_USER, reviewElement.getAuthor());

        setup.setRights(buzBazPageRef, "", String.format("XWiki.%s", FOO_USER), "view", false);
        setup.setRights(fooBarPageRef, "", String.format("XWiki.%s", FOO_USER), "view", true);

        setup.gotoPage(fooBarPageRef);
        viewPage = new ExtendedViewPage();
        ChangeRequestLiveDataElement crLiveData = viewPage.openChangeRequestTab();
        assertEquals(1, crLiveData.countRows());

        ChangeRequestLiveDataElement.ChangeRequestRowElement crRow = crLiveData.getChangeRequests().get(0);
        assertEquals("SplitTest", crRow.getTitle());

        changeRequestPage = crRow.gotoChangeRequest();
        fileChangesPane = changeRequestPage.openFileChanges();
        listOfChangedFiles = fileChangesPane.getListOfChangedFiles();
        assertEquals(Collections.singletonList(fooBarPageRef.toString()), listOfChangedFiles);

        reviewsPane = changeRequestPage.openReviewsPane();
        listOfApprovers = reviewsPane.getListOfApprovers();
        assertEquals(List.of(FOO_USER, BAR_USER), listOfApprovers);

        reviews = reviewsPane.getReviews();
        assertEquals(2, reviews.size());

        reviewElement = reviews.get(0);
        assertTrue(reviewElement.isApproval());
        assertTrue(reviewElement.isOutdated());
        assertEquals(XWIKI_USER_PREFIX + BUZ_USER, reviewElement.getAuthor());

        reviewElement = reviews.get(1);
        assertFalse(reviewElement.isApproval());
        assertTrue(reviewElement.isOutdated());
        assertEquals(XWIKI_USER_PREFIX + FOO_USER, reviewElement.getAuthor());

        setup.gotoPage(buzBazPageRef);
        viewPage = new ExtendedViewPage();
        crLiveData = viewPage.openChangeRequestTab();
        assertEquals(1, crLiveData.countRows());

        crRow = crLiveData.getChangeRequests().get(0);
        assertEquals("SplitTest", crRow.getTitle());

        changeRequestPage = crRow.gotoChangeRequest();
        fileChangesPane = changeRequestPage.openFileChanges();
        listOfChangedFiles = fileChangesPane.getListOfChangedFiles();
        assertEquals(Collections.singletonList(buzBazPageRef.toString()), listOfChangedFiles);

        reviewsPane = changeRequestPage.openReviewsPane();
        listOfApprovers = reviewsPane.getListOfApprovers();
        assertEquals(List.of(BUZ_USER, BAZ_USER), listOfApprovers);
        reviews = reviewsPane.getReviews();
        assertEquals(2, reviews.size());

        reviewElement = reviews.get(0);
        assertTrue(reviewElement.isApproval());
        assertTrue(reviewElement.isOutdated());
        assertEquals(XWIKI_USER_PREFIX + BUZ_USER, reviewElement.getAuthor());

        reviewElement = reviews.get(1);
        assertFalse(reviewElement.isApproval());
        assertTrue(reviewElement.isOutdated());
        assertEquals(XWIKI_USER_PREFIX + FOO_USER, reviewElement.getAuthor());
    }
}
