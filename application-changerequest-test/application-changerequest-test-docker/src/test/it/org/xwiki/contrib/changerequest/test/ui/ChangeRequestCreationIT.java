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

import java.util.Date;
import java.util.List;

import org.checkerframework.checker.units.qual.C;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.contrib.changerequest.test.po.description.DescriptionEditPage;
import org.xwiki.contrib.changerequest.test.po.ChangeRequestPage;
import org.xwiki.contrib.changerequest.test.po.ChangeRequestSaveModal;
import org.xwiki.contrib.changerequest.test.po.ExtendedEditPage;
import org.xwiki.contrib.changerequest.test.po.ExtendedViewPage;
import org.xwiki.contrib.changerequest.test.po.FileChangesPane;
import org.xwiki.contrib.changerequest.test.po.description.DescriptionPane;
import org.xwiki.contrib.changerequest.test.po.description.TimelineEvent;
import org.xwiki.contrib.changerequest.test.po.reviews.ReviewElement;
import org.xwiki.contrib.changerequest.test.po.reviews.ReviewModal;
import org.xwiki.contrib.changerequest.test.po.reviews.ReviewsPane;
import org.xwiki.contrib.changerequest.test.po.checks.CheckPanelElement;
import org.xwiki.contrib.changerequest.test.po.checks.ChecksPane;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.diff.DocumentDiffSummary;
import org.xwiki.test.ui.po.diff.EntityDiff;
import org.xwiki.test.ui.po.editor.WYSIWYGEditPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UI tests related to the creation of a change request.
 *
 * @version $Id$
 * @since 0.5
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
class ChangeRequestCreationIT
{
    private static final String CR_CREATOR = "CRCreator";
    private static final String CR_APPROVER = "Approver";
    private static final String CR_MERGER = "Merger";

    @Test
    @Order(1)
    void createChangeRequest(TestUtils setup, TestReference testReference) throws Exception
    {
        String serializedReference = testReference.getLocalDocumentReference().toString();
        setup.loginAsSuperAdmin();
        setup.createPage(testReference, "Some content to the test page.");

        // Fixture with 3 users:
        // CRCreator: does not have edit rights, but has right to create CR
        // Approver: does not have edit rights, but has approval right
        // Merger: does not approval right but has edit right
        setup.createUser(CR_CREATOR, CR_CREATOR, null);
        setup.createUser(CR_APPROVER, CR_APPROVER, null);
        setup.createUser(CR_MERGER, CR_MERGER, null);

        setup.setRights(testReference, "", CR_CREATOR, "edit", false);
        setup.setRights(testReference, "", CR_CREATOR, "changerequest", true);

        setup.setRights(testReference, "", CR_APPROVER, "edit", false);
        setup.setRights(testReference, "", CR_APPROVER, "crapprove", true);

        setup.setRights(testReference, "", CR_MERGER, "crapprove", false);
        setup.setRights(testReference, "", CR_MERGER, "edit", true);

        // We force the approver to use WYSIWYG editor, to avoid any problem to display the review modal.
        // This should be removed once https://jira.xwiki.org/browse/XWIKI-19281 is fixed
        setup.updateObject("XWiki", CR_APPROVER, "XWiki.XWikiUsers", 0, "editor", "Wysiwyg");

        // we go to the page to ensure there's no remaining lock on it.
        setup.gotoPage(testReference);

        setup.login(CR_CREATOR, CR_CREATOR);
        setup.gotoPage(testReference);
        ExtendedViewPage extendedViewPage = new ExtendedViewPage();
        assertTrue(extendedViewPage.hasChangeRequestEditButton());

        // Save the date for checking the events
        Date dateBeforeCR = new Date();
        ExtendedEditPage<WYSIWYGEditPage> extendedEditPage = extendedViewPage.clickChangeRequestEdit();
        extendedEditPage.getEditor().setContent("Some new content.");
        assertTrue(extendedEditPage.hasSaveAsChangeRequestButton());

        ChangeRequestSaveModal changeRequestSaveModal = extendedEditPage.clickSaveAsChangeRequest();
        changeRequestSaveModal.setChangeRequestTitle("CR1");
        ChangeRequestPage changeRequestPage = changeRequestSaveModal.clickSave();

        // Check status and description holder at first save
        assertEquals("Ready for review", changeRequestPage.getStatusLabel());

        DescriptionPane descriptionPane = changeRequestPage.openDescription();
        assertFalse(descriptionPane.hasCustomDescription());

        // Check event
        Date dateAfterCR = new Date();
        List<TimelineEvent> events = descriptionPane.getEvents();
        assertEquals(1, events.size());

        TimelineEvent timelineEvent = events.get(0);
        assertTrue(timelineEvent.getDate().after(dateBeforeCR));
        assertTrue(timelineEvent.getDate().before(dateAfterCR));
        assertEquals("CRCreator\n"
                + "created the change request with changes concerning "
                + "xwiki:NestedChangeRequestCreationIT.createChangeRequest.WebHome",
            timelineEvent.getContent().getText());

        // Update the description and check related event
        assertTrue(descriptionPane.hasEditDescriptionLink());
        DescriptionEditPage descriptionEditPage = descriptionPane.clickEditDescription();
        descriptionEditPage.setDescription("This is a new CR with some content.");
        changeRequestPage = descriptionEditPage.saveDescription();
        descriptionPane = changeRequestPage.openDescription();
        descriptionPane.waitUntilEventsSize(2);

        assertTrue(descriptionPane.hasCustomDescription());
        assertEquals("This is a new CR with some content.", descriptionPane.getDescription());

        Date dateAfterDescriptionUpdate = new Date();

        events = descriptionPane.getEvents();
        assertEquals(2, events.size());
        timelineEvent = events.get(1);
        assertTrue(timelineEvent.getDate().after(dateAfterCR));
        assertTrue(timelineEvent.getDate().before(dateAfterDescriptionUpdate));
        assertEquals("CRCreator\n"
                + "edited the description or the title of the change request",
            timelineEvent.getContent().getText());

        // Check the diff
        FileChangesPane fileChangesPane = changeRequestPage.openFileChanges();
        assertEquals(1, fileChangesPane.getFileChangesListLiveData().getTableLayout().countRows());
        DocumentDiffSummary diffSummary = fileChangesPane.getDiffSummary(serializedReference);
        assertEquals("(2 modified, 0 added, 0 removed)", diffSummary.getPagePropertiesSummary());

        EntityDiff contentDiff = fileChangesPane.getEntityDiff(serializedReference, "Page properties");
        List<String> content = contentDiff.getDiff("Content");
        assertEquals(3, content.size());
        assertEquals("-Some content<del> to the test page</del>.", content.get(1));
        assertEquals("+Some <ins>new </ins>content.", content.get(2));

        // Check the review tab
        ReviewsPane reviewsPane = changeRequestPage.openReviewsPane();
        assertFalse(reviewsPane.hasListOfApprovers());
        assertTrue(reviewsPane.getReviews().isEmpty());

        // Check the check tab
        ChecksPane checksPane = changeRequestPage.openChecksPane();

        assertTrue(checksPane.getStatusCheck().isReady());
        assertTrue(checksPane.getConflictCheck().isReady());

        CheckPanelElement strategyCheck = checksPane.getStrategyCheck();
        assertFalse(strategyCheck.isReady());
        assertFalse(strategyCheck.isOpened());
        strategyCheck.togglePanel();
        assertEquals("The change request cannot be merged without valid approval or "
            + "if at least one review request for changes.", strategyCheck.getBody().getText());

        String pageURL = changeRequestPage.getPageURL();

        // Status transitions that are tested:
        // Ready for review -> Draft -> Ready for review -> Closed -> Draft -> Closed -> Ready for review

        // Check buttons with guest user
        setup.forceGuestUser();
        setup.gotoPage(pageURL);
        changeRequestPage = new ChangeRequestPage();
        assertTrue(changeRequestPage.isAnyChangeRequestButtonDisplayed());
        assertTrue(changeRequestPage.isReviewButtonDisplayed());
        assertFalse(changeRequestPage.isReviewButtonEnabled());

        // Switch the change request to draft: check the status label, the checks and the timeline
        setup.login(CR_CREATOR, CR_CREATOR);
        setup.gotoPage(pageURL);
        changeRequestPage = new ChangeRequestPage();
        assertTrue(changeRequestPage.isReviewButtonDisplayed());

        // The creator cannot review its own CR
        assertFalse(changeRequestPage.isReviewButtonEnabled());
        assertTrue(changeRequestPage.isConvertToDraftEnabled());

        changeRequestPage = changeRequestPage.clickConvertToDraft();
        assertEquals("Draft", changeRequestPage.getStatusLabel());

        checksPane = changeRequestPage.openChecksPane();
        assertFalse(checksPane.getStatusCheck().isReady());

        descriptionPane = changeRequestPage.openDescription();
        descriptionPane.waitUntilEventsSize(3);

        Date dateAfterStatusChange = new Date();
        events = descriptionPane.getEvents();
        assertEquals(3, events.size());

        timelineEvent = events.get(2);
        assertTrue(timelineEvent.getDate().after(dateAfterDescriptionUpdate));
        assertTrue(timelineEvent.getDate().before(dateAfterStatusChange));
        assertEquals("CRCreator\n"
                + "changed the status of the change request from ready for review to draft",
            timelineEvent.getContent().getText());

        // Check buttons with guest user
        setup.forceGuestUser();
        setup.gotoPage(pageURL);
        changeRequestPage = new ChangeRequestPage();
        assertFalse(changeRequestPage.isAnyChangeRequestButtonDisplayed());

        // Switch back the status to ready for review and check again status and timeline
        setup.login(CR_CREATOR, CR_CREATOR);
        setup.gotoPage(pageURL);
        changeRequestPage = new ChangeRequestPage();

        assertTrue(changeRequestPage.isAnyChangeRequestButtonDisplayed());
        assertFalse(changeRequestPage.isReviewButtonDisplayed());
        assertFalse(changeRequestPage.isConvertToDraftEnabled());
        assertTrue(changeRequestPage.isReadyForReviewButtonDisplayed());
        assertTrue(changeRequestPage.isReadyForReviewButtonEnabled());

        changeRequestPage = changeRequestPage.clickReadyForReviewButton();
        assertEquals("Ready for review", changeRequestPage.getStatusLabel());

        checksPane = changeRequestPage.openChecksPane();
        assertTrue(checksPane.getStatusCheck().isReady());

        descriptionPane = changeRequestPage.openDescription();
        descriptionPane.waitUntilEventsSize(4);
        Date dateAfterStatusChange2 = new Date();
        events = descriptionPane.getEvents();
        assertEquals(4, events.size());

        timelineEvent = events.get(3);
        assertTrue(timelineEvent.getDate().after(dateAfterStatusChange));
        assertTrue(timelineEvent.getDate().before(dateAfterStatusChange2));
        assertEquals("CRCreator\n"
                + "changed the status of the change request from draft to ready for review",
            timelineEvent.getContent().getText());

        // Close the change request, check the label and the timeline
        assertTrue(changeRequestPage.isCloseEnabled());
        changeRequestPage = changeRequestPage.clickClose();

        assertEquals("Closed", changeRequestPage.getStatusLabel());

        // TODO: We should check that it's not possible to edit a file from file change when it's closed.

        checksPane = changeRequestPage.openChecksPane();
        assertFalse(checksPane.getStatusCheck().isReady());

        descriptionPane = changeRequestPage.openDescription();
        descriptionPane.waitUntilEventsSize(5);
        Date dateAfterStatusChange3 = new Date();
        events = descriptionPane.getEvents();
        assertEquals(5, events.size());

        timelineEvent = events.get(4);
        assertTrue(timelineEvent.getDate().after(dateAfterStatusChange2));
        assertTrue(timelineEvent.getDate().before(dateAfterStatusChange3));
        assertEquals("CRCreator\n"
                + "changed the status of the change request from ready for review to closed",
            timelineEvent.getContent().getText());

        // Check the button display with guest
        setup.forceGuestUser();
        setup.gotoPage(pageURL);
        changeRequestPage = new ChangeRequestPage();
        assertTrue(changeRequestPage.isAnyChangeRequestButtonDisplayed());
        assertTrue(changeRequestPage.isOpenButtonDisplayed());
        assertFalse(changeRequestPage.isOpenButtonEnabled());

        setup.login(CR_CREATOR, CR_CREATOR);
        setup.gotoPage(pageURL);
        changeRequestPage = new ChangeRequestPage();

        assertTrue(changeRequestPage.isAnyChangeRequestButtonDisplayed());
        assertTrue(changeRequestPage.isOpenButtonDisplayed());
        assertTrue(changeRequestPage.isOpenButtonEnabled());
        assertTrue(changeRequestPage.isOpenAsDraftEnabled());

        // Open as draft, check status
        changeRequestPage = changeRequestPage.clickOpenAsDraft();
        assertEquals("Draft", changeRequestPage.getStatusLabel());

        // Close again
        assertTrue(changeRequestPage.isCloseEnabled());
        changeRequestPage = changeRequestPage.clickClose();
        assertEquals("Closed", changeRequestPage.getStatusLabel());

        // Open as ready for review, check status and check the 3 new events
        assertTrue(changeRequestPage.isAnyChangeRequestButtonDisplayed());
        assertTrue(changeRequestPage.isOpenButtonDisplayed());
        assertTrue(changeRequestPage.isOpenButtonEnabled());
        changeRequestPage = changeRequestPage.clickOpen();
        assertEquals("Ready for review", changeRequestPage.getStatusLabel());

        descriptionPane = changeRequestPage.openDescription();
        descriptionPane.waitUntilEventsSize(8);
        Date dateAfterStatusChange4 = new Date();
        events = descriptionPane.getEvents();
        assertEquals(8, events.size());

        timelineEvent = events.get(5);
        assertTrue(timelineEvent.getDate().after(dateAfterStatusChange3));
        assertTrue(timelineEvent.getDate().before(dateAfterStatusChange4));
        assertEquals("CRCreator\n"
                + "changed the status of the change request from closed to draft",
            timelineEvent.getContent().getText());

        timelineEvent = events.get(6);
        assertTrue(timelineEvent.getDate().after(dateAfterStatusChange3));
        assertTrue(timelineEvent.getDate().before(dateAfterStatusChange4));
        assertEquals("CRCreator\n"
                + "changed the status of the change request from draft to closed",
            timelineEvent.getContent().getText());

        timelineEvent = events.get(7);
        assertTrue(timelineEvent.getDate().after(dateAfterStatusChange3));
        assertTrue(timelineEvent.getDate().before(dateAfterStatusChange4));
        assertEquals("CRCreator\n"
                + "changed the status of the change request from closed to ready for review",
            timelineEvent.getContent().getText());

        // Add an approval review to the page
        setup.login(CR_APPROVER, CR_APPROVER);
        setup.gotoPage(pageURL);
        changeRequestPage = new ChangeRequestPage();
        assertTrue(changeRequestPage.isAnyChangeRequestButtonDisplayed());
        assertTrue(changeRequestPage.isReviewButtonDisplayed());
        assertTrue(changeRequestPage.isReviewButtonEnabled());

        ReviewModal reviewModal = changeRequestPage.clickReviewButton();
        assertFalse(reviewModal.isSaveEnabled());
        reviewModal.selectApprove();
        assertTrue(reviewModal.isSaveEnabled());
        //reviewModal.setComment("This change request looks ok.");
        reviewModal.save();
        Date dateAfterReview = new Date();
        // There is a reload after the save
        changeRequestPage = new ChangeRequestPage();
        reviewsPane = changeRequestPage.openReviewsPane();
        List<ReviewElement> reviews = reviewsPane.getReviews();
        assertEquals(1, reviews.size());

        ReviewElement review = reviews.get(0);
        assertTrue(review.isApproval());
        assertFalse(review.isOutdated());
        assertTrue(review.isToggleValidButtonEnabled());
        assertEquals("xwiki:XWiki.Approver", review.getAuthor());
        assertTrue(review.getDate().after(dateAfterStatusChange4));
        assertTrue(review.getDate().before(dateAfterReview));

        // TODO: check the discussion associated to the review

        // Check the events created
        // There should be 3 more events:
        // 1 for the review created
        // 1 for the status change to ready for merge
        descriptionPane = changeRequestPage.openDescription();
        descriptionPane.waitUntilEventsSize(10);
        changeRequestPage = new ChangeRequestPage();
        descriptionPane = changeRequestPage.openDescription();
        events = descriptionPane.getEvents();
        assertEquals(10, events.size());

        timelineEvent = events.get(8);
        assertTrue(timelineEvent.getDate().after(dateAfterStatusChange4));
        assertTrue(timelineEvent.getDate().before(dateAfterReview));
        assertEquals("Approver\n"
                + "added a new approval review",
            timelineEvent.getContent().getText());

        timelineEvent = events.get(9);
        assertTrue(timelineEvent.getDate().after(dateAfterStatusChange4));
        assertTrue(timelineEvent.getDate().before(dateAfterReview));
        assertEquals("Approver\n"
                + "changed the status of the change request from ready for review to ready for merging",
            timelineEvent.getContent().getText());

        // Since we have an approval review, checks should be all good and CR should be ready to be merged
        checksPane = changeRequestPage.openChecksPane();
        assertTrue(checksPane.getStrategyCheck().isReady());

        assertTrue(changeRequestPage.isMergeButtonDisplayed());

        // Approver doesn't have right to merge CR
        assertFalse(changeRequestPage.isMergeButtonEnabled());

        setup.login(CR_MERGER, CR_MERGER);
        setup.gotoPage(pageURL);
        changeRequestPage = new ChangeRequestPage();
        assertTrue(changeRequestPage.isMergeButtonDisplayed());
        // FIXME: it's false right now.
        //assertTrue(changeRequestPage.isMergeButtonEnabled());

        // Check with guest and creator too
        setup.forceGuestUser();
        setup.gotoPage(pageURL);
        changeRequestPage = new ChangeRequestPage();
        assertTrue(changeRequestPage.isMergeButtonDisplayed());
        assertFalse(changeRequestPage.isMergeButtonEnabled());

        setup.login(CR_CREATOR, CR_CREATOR);
        setup.gotoPage(pageURL);
        changeRequestPage = new ChangeRequestPage();
        assertTrue(changeRequestPage.isMergeButtonDisplayed());
        assertFalse(changeRequestPage.isMergeButtonEnabled());

        // Add a new review requesting for changes
        setup.login(CR_APPROVER, CR_APPROVER);
        setup.gotoPage(pageURL);
        changeRequestPage = new ChangeRequestPage();

        // the review button is not anymore the main button, but it still available in the drop down menu
        assertTrue(changeRequestPage.isAnyChangeRequestButtonDisplayed());
        assertFalse(changeRequestPage.isReviewButtonDisplayed());
        changeRequestPage.toggleChangeRequestActionsMenu();
        assertTrue(changeRequestPage.isReviewButtonDisplayed());
        assertTrue(changeRequestPage.isReviewButtonEnabled());

        reviewModal = changeRequestPage.clickReviewButton();
        assertFalse(reviewModal.isSaveEnabled());
        reviewModal.selectRequestChanges();
        assertTrue(reviewModal.isSaveEnabled());
        //reviewModal.setComment("On second thought I'm not so sure anymore it's good.");
        reviewModal.save();

        Date dateAfterReview2 = new Date();

        // the page has been reloaded for adding the review
        changeRequestPage = new ChangeRequestPage();
        assertFalse(changeRequestPage.isMergeButtonDisplayed());

        checksPane = changeRequestPage.openChecksPane();
        assertFalse(checksPane.getStrategyCheck().isReady());

        reviewsPane = changeRequestPage.openReviewsPane();
        reviews = reviewsPane.getReviews();
        assertEquals(2, reviews.size());

        // Reviews are presented in reverse order
        review = reviews.get(0);
        assertFalse(review.isApproval());
        assertFalse(review.isOutdated());
        assertTrue(review.getDate().after(dateAfterReview));
        assertTrue(review.getDate().before(dateAfterReview2));
        assertEquals("xwiki:XWiki.Approver", review.getAuthor());
        assertTrue(review.isToggleValidButtonEnabled());

        review = reviews.get(1);
        assertTrue(review.isApproval());
        assertTrue(review.isOutdated());
        assertFalse(review.isToggleValidButtonEnabled());
    }
}
