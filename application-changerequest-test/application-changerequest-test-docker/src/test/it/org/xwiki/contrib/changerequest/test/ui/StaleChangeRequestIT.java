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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.xwiki.contrib.changerequest.test.po.ChangeRequestPage;
import org.xwiki.contrib.changerequest.test.po.ChangeRequestSaveModal;
import org.xwiki.contrib.changerequest.test.po.ExtendedEditPage;
import org.xwiki.contrib.changerequest.test.po.ExtendedViewPage;
import org.xwiki.contrib.changerequest.test.po.description.DescriptionEditPage;
import org.xwiki.contrib.changerequest.test.po.description.DescriptionPane;
import org.xwiki.contrib.changerequest.test.po.description.TimelineEvent;
import org.xwiki.contrib.changerequest.test.po.discussion.DiscussionEditor;
import org.xwiki.contrib.changerequest.test.po.filechanges.FileChangesPane;
import org.xwiki.contrib.changerequest.test.po.filechanges.FilechangesLiveDataElement;
import org.xwiki.contrib.changerequest.test.po.reviews.ReviewContainer;
import org.xwiki.scheduler.test.po.SchedulerHomePage;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.editor.WikiEditPage;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests of the stale change request mechanism.
 *
 * @version $Id$
 * @since 1.12
 */
@UITest(
    properties = {
        "xwikiDbHbmCommonExtraMappings=notification-filter-preferences.hbm.xml",
        "xwikiCfgPlugins=com.xpn.xwiki.plugin.scheduler.SchedulerPlugin"
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
public class StaleChangeRequestIT
{
    /*
    Scenario:
      - Stale duration set to 1 second so that the test is fast
      - Creation of CRs Foo and Bar
      - wait 1 sec
      - Trigger stale notification CR scheduler
      - Verify Foo is marked as stale and perform a review in it
      - Verify Bar is marked as stale and edit the title in it
      - wait 1 sec and trigger close notification CR scheduler
      - Check Foo and Bar and ensure they're both still open and no new marked as stale event
      - trigger stale notification CR
      - Check Foo and Bar and ensure they're both marked as stale
      - Close Foo
      - trigger close notification CR scheduler
      - Check Foo and ensure it hasn't changed -> reopen it
      - Check Bar and ensure it's closed by stale -> reopen it
      - Wait 1 sec and trigger stale notification CR scheduler
      - Check Foo and ensure it's again marked as stale, add a new change in it
      - Check Bar and ensure it's again marked as stale, add a comment in it
      - Wait 1 sec and trigger close stale CR
      - Ensure Foo and Bar are still opened
     */

    private static final String TEST_USER_PREFIX = "StaleChangeIT";
    private static final String CR_USER = TEST_USER_PREFIX + "CRUser";
    private static final String CR_CREATOR = TEST_USER_PREFIX + "CRCreator";

    @BeforeAll
    void setupUsers(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
        setup.createUser(CR_CREATOR, CR_CREATOR, null);
        setup.createUser(CR_USER, CR_USER, null);

        setup.updateObject("XWiki", CR_USER, "XWiki.XWikiUsers", 0, "editor", "Wysiwyg");

        // Ensure to use right strategy
        setup.updateObject(Arrays.asList("ChangeRequest", "Code"), "Configuration",
            "ChangeRequest.Code.ConfigurationClass", 0,
            "minimumApprovers", 0,
            "mergeUser", "",
            "approvalStrategy", "onlyapproved",
            "durationBeforeClosingStale", 1,
            "durationBeforeNotifyingStale", 1,
            "durationUnit", "seconds"
        );
        // Force a bit the timeout for those tests as the scheduler each second might slow down everything.
        setup.getDriver().setDriverImplicitWait(30);
    }

    @AfterAll
    void afterAll(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
        // Ensure that the timeouts won't mess up other tests.
        setup.updateObject(Arrays.asList("ChangeRequest", "Code"), "Configuration",
            "ChangeRequest.Code.ConfigurationClass", 0,
            "minimumApprovers", 0,
            "mergeUser", "",
            "approvalStrategy", "onlyapproved",
            "durationBeforeClosingStale", 1,
            "durationBeforeNotifyingStale", 1,
            "durationUnit", "days"
        );
    }

    @Test
    void createStaleCR(TestUtils testUtils, TestReference testReference) throws InterruptedException
    {
        // We're giving approval right to the CR_USER to allow performing review
        testUtils.setRights(testReference, "", CR_USER, "crapprove", true);
        testUtils.createPage(testReference, "Some content", "Some title");

        testUtils.login(CR_CREATOR, CR_CREATOR);
        // Create Foo
        testUtils.gotoPage(testReference);
        ExtendedViewPage extendedViewPage = new ExtendedViewPage();

        ExtendedEditPage<WikiEditPage> extendedEditPage = extendedViewPage.clickStandardEdit();
        extendedEditPage.getWrappedEditor().setContent("Some new content.");
        extendedEditPage.getWrappedEditor().setTitle("A new title");

        ChangeRequestSaveModal changeRequestSaveModal = extendedEditPage.clickSaveAsChangeRequest();
        changeRequestSaveModal.setChangeRequestTitle("Stale Foo");
        changeRequestSaveModal.clickSave();

        String fooCRURL = testUtils.getDriver().getCurrentUrl();

        // Create Bar
        testUtils.gotoPage(testReference);
        extendedViewPage = new ExtendedViewPage();

        extendedEditPage = extendedViewPage.clickStandardEdit();
        extendedEditPage.getWrappedEditor().setContent("Some new content 2.");
        extendedEditPage.getWrappedEditor().setTitle("A new title 2");

        changeRequestSaveModal = extendedEditPage.clickSaveAsChangeRequest();
        changeRequestSaveModal.setChangeRequestTitle("Stale Bar");
        changeRequestSaveModal.clickSave();

        String barCRURL = testUtils.getDriver().getCurrentUrl();

        triggerNotifyStaleCRScheduler(testUtils);

        // Check Foo is stale and perform review
        testUtils.gotoPage(fooCRURL);
        ChangeRequestPage changeRequestPage = new ChangeRequestPage();
        assertEquals("Ready for review", changeRequestPage.getStatusLabel());
        DescriptionPane descriptionPane = changeRequestPage.openDescription();
        descriptionPane = descriptionPane.waitUntilEventsSize(2);
        List<TimelineEvent> events = descriptionPane.getEvents();

        assertEquals(2, events.size());

        TimelineEvent timelineEvent = events.get(1);
        assertEquals("changerequest.stale", timelineEvent.getEventType());

        ReviewContainer reviewContainer = changeRequestPage.clickReviewButton();
        reviewContainer.selectRequestChanges();
        reviewContainer.save();

        // Check Bar is stale and edit description
        testUtils.login(CR_CREATOR, CR_CREATOR);
        testUtils.gotoPage(barCRURL);
        changeRequestPage = new ChangeRequestPage();
        assertEquals("Ready for review", changeRequestPage.getStatusLabel());
        descriptionPane = changeRequestPage.openDescription();
        descriptionPane = descriptionPane.waitUntilEventsSize(2);

        events = descriptionPane.getEvents();

        assertEquals(2, events.size());

        timelineEvent = events.get(1);
        assertEquals("changerequest.stale", timelineEvent.getEventType());

        DescriptionEditPage descriptionEditPage = descriptionPane.clickEditDescription();
        descriptionEditPage.setDescription("Some description");
        descriptionEditPage.saveDescription();

        triggerCloseStaleCRScheduler(testUtils);

        // Check Foo is still open
        testUtils.gotoPage(fooCRURL);
        changeRequestPage = new ChangeRequestPage();
        assertEquals("Ready for review", changeRequestPage.getStatusLabel());

        descriptionPane = changeRequestPage.openDescription();
        descriptionPane = descriptionPane.waitUntilEventsSize(3);
        events = descriptionPane.getEvents();

        assertEquals(3, events.size());

        timelineEvent = events.get(2);
        assertEquals("changerequest.review.added", timelineEvent.getEventType());

        // Check Bar is still open
        testUtils.gotoPage(barCRURL);
        changeRequestPage = new ChangeRequestPage();
        assertEquals("Ready for review", changeRequestPage.getStatusLabel());

        descriptionPane = changeRequestPage.openDescription();
        descriptionPane = descriptionPane.waitUntilEventsSize(3);
        events = descriptionPane.getEvents();

        assertEquals(3, events.size());

        timelineEvent = events.get(2);
        assertEquals("changerequest.updated", timelineEvent.getEventType());

        triggerNotifyStaleCRScheduler(testUtils);

        // Check Foo is stale and close it
        testUtils.login(CR_CREATOR, CR_CREATOR);
        testUtils.gotoPage(fooCRURL);
        changeRequestPage = new ChangeRequestPage();
        assertEquals("Ready for review", changeRequestPage.getStatusLabel());
        descriptionPane = changeRequestPage.openDescription();
        descriptionPane = descriptionPane.waitUntilEventsSize(4);
        events = descriptionPane.getEvents();

        assertEquals(4, events.size());

        timelineEvent = events.get(3);
        assertEquals("changerequest.stale", timelineEvent.getEventType());

        changeRequestPage.clickClose();

        // Check Bar is stale and do nothing
        testUtils.gotoPage(barCRURL);
        changeRequestPage = new ChangeRequestPage();
        assertEquals("Ready for review", changeRequestPage.getStatusLabel());
        descriptionPane = changeRequestPage.openDescription();
        descriptionPane = descriptionPane.waitUntilEventsSize(4);
        events = descriptionPane.getEvents();

        assertEquals(4, events.size());

        timelineEvent = events.get(3);
        assertEquals("changerequest.stale", timelineEvent.getEventType());

        triggerCloseStaleCRScheduler(testUtils);

        // Check Foo is closed and reopen
        testUtils.login(CR_CREATOR, CR_CREATOR);
        testUtils.gotoPage(fooCRURL);
        changeRequestPage = new ChangeRequestPage();
        assertEquals("Closed", changeRequestPage.getStatusLabel());
        descriptionPane = changeRequestPage.openDescription();
        descriptionPane = descriptionPane.waitUntilEventsSize(5);
        events = descriptionPane.getEvents();

        assertEquals(5, events.size());

        timelineEvent = events.get(4);
        assertEquals("changerequest.status.modified", timelineEvent.getEventType());
        assertEquals("StaleChangeITCRCreator changed the status of the Change Request from ready for review to closed",
            timelineEvent.getContent().getText());

        changeRequestPage.clickOpen();

        // Check Bar is closed as stale and reopen
        testUtils.gotoPage(barCRURL);
        changeRequestPage = new ChangeRequestPage();
        assertEquals("Closed (stale)", changeRequestPage.getStatusLabel());
        descriptionPane = changeRequestPage.openDescription();
        descriptionPane = descriptionPane.waitUntilEventsSize(5);
        events = descriptionPane.getEvents();

        assertEquals(5, events.size());

        timelineEvent = events.get(4);
        assertEquals("changerequest.status.modified", timelineEvent.getEventType());
        assertEquals("Unknown User changed the status of the Change Request from ready for review to closed (stale)",
            timelineEvent.getContent().getText());

        changeRequestPage.clickOpen();

        triggerNotifyStaleCRScheduler(testUtils);

        // Check Foo is open but marked as stale and add change
        testUtils.login(CR_CREATOR, CR_CREATOR);
        testUtils.gotoPage(fooCRURL);
        changeRequestPage = new ChangeRequestPage();
        assertEquals("Ready for review", changeRequestPage.getStatusLabel());
        descriptionPane = changeRequestPage.openDescription();
        descriptionPane = descriptionPane.waitUntilEventsSize(7);
        events = descriptionPane.getEvents();

        assertEquals(7, events.size());

        timelineEvent = events.get(6);
        assertEquals("changerequest.stale", timelineEvent.getEventType());

        FileChangesPane fileChangesPane = changeRequestPage.openFileChanges();
        FilechangesLiveDataElement fileChangesListLiveData = fileChangesPane.getFileChangesListLiveData();
        List<FilechangesLiveDataElement.FilechangesRowElement> fileChanges = fileChangesListLiveData.getFileChanges();
        assertEquals(1, fileChanges.size());

        FilechangesLiveDataElement.FilechangesRowElement rowElement = fileChanges.get(0);
        extendedEditPage = rowElement.clickEdit();
        extendedEditPage.getWrappedEditor().setContent("Some other content for another edition");
        extendedEditPage.clickSaveAsChangeRequestInExistingCR();

        // Check Bar is open but marked as stale and add comment
        testUtils.login(CR_USER, CR_USER);
        testUtils.gotoPage(barCRURL);
        changeRequestPage = new ChangeRequestPage();
        assertEquals("Ready for review", changeRequestPage.getStatusLabel());
        descriptionPane = changeRequestPage.openDescription();
        descriptionPane = descriptionPane.waitUntilEventsSize(7);
        events = descriptionPane.getEvents();

        assertEquals(7, events.size());

        timelineEvent = events.get(6);
        assertEquals("changerequest.stale", timelineEvent.getEventType());

        DiscussionEditor discussionEditor = descriptionPane.clickCommentButton();
        discussionEditor.setContent("Some global comment");
        discussionEditor.clickSave();

        triggerCloseStaleCRScheduler(testUtils);

        // Check Foo is still opened
        testUtils.gotoPage(fooCRURL);
        changeRequestPage = new ChangeRequestPage();
        assertEquals("Ready for review", changeRequestPage.getStatusLabel());
        descriptionPane = changeRequestPage.openDescription();
        descriptionPane = descriptionPane.waitUntilEventsSize(8);
        events = descriptionPane.getEvents();

        assertEquals(8, events.size());

        timelineEvent = events.get(7);
        assertEquals("changerequest.filechange.added", timelineEvent.getEventType());

        // Check Bar is still opened
        testUtils.gotoPage(barCRURL);
        changeRequestPage = new ChangeRequestPage();
        assertEquals("Ready for review", changeRequestPage.getStatusLabel());
        descriptionPane = changeRequestPage.openDescription();
        descriptionPane = descriptionPane.waitUntilEventsSize(8);
        events = descriptionPane.getEvents();

        assertEquals(8, events.size());

        timelineEvent = events.get(7);
        assertEquals("changerequest.discussions", timelineEvent.getEventType());
    }

    private void triggerNotifyStaleCRScheduler(TestUtils testUtils) throws InterruptedException
    {
        // Wait 1 sec since that's the minimum duration for the schedulers.
        Thread.sleep(1000);

        testUtils.loginAsSuperAdmin();

        SchedulerHomePage schedulerHomePage = SchedulerHomePage.gotoPage();
        schedulerHomePage.clickJobActionTrigger("Stale Change Request Notifier");

        testUtils.login(CR_USER, CR_USER);
    }

    private void triggerCloseStaleCRScheduler(TestUtils testUtils) throws InterruptedException
    {
        // Wait 1 sec since that's the minimum duration for the schedulers.
        Thread.sleep(1000);

        testUtils.loginAsSuperAdmin();
        SchedulerHomePage schedulerHomePage = SchedulerHomePage.gotoPage();
        schedulerHomePage.clickJobActionTrigger("Stale Change Request Closer");
        testUtils.login(CR_USER, CR_USER);
    }
}
