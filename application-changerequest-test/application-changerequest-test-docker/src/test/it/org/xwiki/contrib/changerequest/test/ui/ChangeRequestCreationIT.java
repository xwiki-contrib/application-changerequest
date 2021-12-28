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
    @Test
    @Order(1)
    void createChangeRequest(TestUtils setup, TestReference testReference)
    {
        setup.loginAsSuperAdmin();
        setup.createPage(testReference, "Some content to the test page.");
        setup.createUser("CRCreator", "CRCreator", null);
        setup.createUser("Approver", "Approver", null);

        setup.setRights(testReference, "", "CRCreator", "edit", false);
        setup.setRights(testReference, "", "CRCreator", "changerequest", true);
        setup.setRights(testReference, "", "Approver", "crapprove", true);

        // we go to the page to ensure there's no remaining lock on it.
        setup.gotoPage(testReference);

        setup.login("CRCreator", "CRCreator");
        setup.gotoPage(testReference);
        ExtendedViewPage viewPage = new ExtendedViewPage();
        assertTrue(viewPage.hasChangeRequestEditButton());

        // Save the date for checking the events
        Date dateBeforeCR = new Date();
        ExtendedEditPage<WYSIWYGEditPage> extendedEditPage = viewPage.clickChangeRequestEdit();
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

        assertTrue(descriptionPane.hasCustomDescription());
        assertEquals("This is a new CR with some content.", descriptionPane.getDescription());

        descriptionPane.waitUntilEventsSize(2);
        Date dateAfterDescriptionUpdate = new Date();

        events = descriptionPane.getEvents();
        assertEquals(2, events.size());
        timelineEvent = events.get(1);
        assertTrue(timelineEvent.getDate().after(dateAfterCR));
        assertTrue(timelineEvent.getDate().before(dateAfterDescriptionUpdate));
        assertEquals("CRCreator\n"
                + "edited the description of the change request",
            timelineEvent.getContent().getText());

        // Check the diff
        FileChangesPane fileChangesPane = changeRequestPage.openFileChanges();
        assertEquals(1, fileChangesPane.getFileChangesListLiveTable().getRowCount());
        String pageName = testReference.toString();
        DocumentDiffSummary diffSummary = fileChangesPane.getDiffSummary(pageName);
        assertEquals("(1 modified, 0 added, 0 removed)", diffSummary.getPagePropertiesSummary());

        EntityDiff contentDiff = fileChangesPane.getEntityDiff(pageName, "Page properties");
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
        setup.login("CRCreator", "CRCreator");
        setup.gotoPage(pageURL);
        changeRequestPage = new ChangeRequestPage();
        assertTrue(changeRequestPage.isReviewButtonDisplayed());
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
        setup.login("CRCreator", "CRCreator");
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

        setup.login("CRCreator", "CRCreator");
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
    }
}
