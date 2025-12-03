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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.xwiki.ckeditor.test.po.CKEditor;
import org.xwiki.contrib.changerequest.test.po.ChangeRequestPage;
import org.xwiki.contrib.changerequest.test.po.ChangeRequestSaveModal;
import org.xwiki.contrib.changerequest.test.po.ExtendedEditPage;
import org.xwiki.contrib.changerequest.test.po.description.DescriptionPane;
import org.xwiki.contrib.changerequest.test.po.description.TimelineEvent;
import org.xwiki.contrib.changerequest.test.po.discussion.DiffMessageElement;
import org.xwiki.contrib.changerequest.test.po.discussion.DiscussionEditor;
import org.xwiki.contrib.changerequest.test.po.discussion.MessageElement;
import org.xwiki.contrib.changerequest.test.po.discussion.ReviewDiscussion;
import org.xwiki.contrib.changerequest.test.po.filechanges.ChangeRequestEntityDiff;
import org.xwiki.contrib.changerequest.test.po.filechanges.EntityDiffCoordinate;
import org.xwiki.contrib.changerequest.test.po.filechanges.FileChangesPane;
import org.xwiki.contrib.changerequest.test.po.filechanges.LineChange;
import org.xwiki.contrib.changerequest.test.po.reviews.ReviewContainer;
import org.xwiki.contrib.changerequest.test.po.reviews.ReviewElement;
import org.xwiki.contrib.changerequest.test.po.reviews.ReviewsPane;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.editor.ObjectEditPage;
import org.xwiki.test.ui.po.editor.ObjectEditPane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests related to handling of discussions in change requests.
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
class ChangeRequestDiscussionIT
{
    private static final String TEST_USER_PREFIX = "CRDiscussionIT";
    private static final String CR_USER = TEST_USER_PREFIX + "CRUser";
    private static final String CR_CREATOR = TEST_USER_PREFIX  + "CRCreator";

    @BeforeAll
    void setupUsers(TestUtils setup)
    {
        setup.loginAsSuperAdmin();

        // Fixture with 2 users: we don't really care about rights here,
        // we only want different users to check authors
        setup.createUser(CR_CREATOR, CR_CREATOR, null);
        setup.createUser(CR_USER, CR_USER, null);

        setup.updateObject("XWiki", CR_CREATOR, "XWiki.XWikiUsers", 0, "editor", "Wysiwyg");
        setup.updateObject("XWiki", CR_USER, "XWiki.XWikiUsers", 0, "editor", "Wysiwyg");

        // Ensure to use right strategy
        setup.updateObject(Arrays.asList("ChangeRequest", "Code"), "Configuration",
            "ChangeRequest.Code.ConfigurationClass", 0,
            "minimumApprovers", 0,
            "mergeUser", "",
            "approvalStrategy", "onlyapproved");
    }

    @Test
    void createDiscussions(TestUtils testUtils, TestReference testReference)
    {
        String serializedReference = testReference.getLocalDocumentReference().toString();

        // We're giving approval right to the CR_USER to allow performing review
        testUtils.setRights(testReference, "", CR_USER, "crapprove", true);
        testUtils.createPage(testReference, "Some content", "Some title");

        // We inject an object in the page so that we can test the diff on different type of elements
        Map<String, String> objectProperties = new HashMap<>();
        objectProperties.put("name", "my style");
        objectProperties.put("code", ".customClass { color: red; }");
        testUtils.addObject(testReference, "XWiki.StyleSheetExtension", objectProperties);
        // Go to the page to release the lock.
        testUtils.gotoPage(testReference);

        testUtils.login(CR_CREATOR, CR_CREATOR);
        testUtils.gotoPage(testReference, "edit", Collections.singletonMap("editor", "object"));
        ObjectEditPage objectEditPage = new ObjectEditPage();
        List<ObjectEditPane> objects = objectEditPage.getObjectsOfClass("XWiki.StyleSheetExtension");
        assertEquals(1, objects.size());

        ObjectEditPane objectEditPane = objects.get(0);
        objectEditPane.displayObject();
        objectEditPane.setPropertyValue("name", "Another style")
            .setPropertyValue("code", ".customClass { color: blue; }");

        ExtendedEditPage extendedEditPage = new ExtendedEditPage(objectEditPage);
        assertTrue(extendedEditPage.hasSaveAsChangeRequestButton());
        ChangeRequestSaveModal saveModal = extendedEditPage.clickSaveAsChangeRequest();
        saveModal.setChangeRequestTitle("Discussion test");
        ChangeRequestPage changeRequestPage = saveModal.clickSave();
        String changeRequestUrl = testUtils.getDriver().getCurrentUrl();

        DescriptionPane descriptionPane = changeRequestPage.openDescription();

        assertTrue(descriptionPane.hasCommentButton());
        Date beforeComment = new Date();
        DiscussionEditor discussionEditor = descriptionPane.clickCommentButton();
        discussionEditor.setContent("This is a global comment");
        discussionEditor.clickSave();
        changeRequestPage.waitForTimelineRefresh();
        descriptionPane.waitUntilEventsSize(2);
        Date afterComment = new Date();
        List<TimelineEvent> events = descriptionPane.getEvents();
        assertEquals("changerequest.create", events.get(0).getEventType());

        TimelineEvent event = events.get(1);
        assertEquals("changerequest.discussions", event.getEventType());
        MessageElement message = event.getMessage();
        assertEquals("xwiki:XWiki." + CR_CREATOR, message.getAuthor());
        assertEquals("This is a global comment", message.getContent());
        Date messageDate = message.getDate();
        assertTrue(beforeComment.before(messageDate) && afterComment.after(messageDate));
        assertTrue(message.isExpanded());
        assertFalse(message.isOutdated());

        FileChangesPane fileChangesPane = changeRequestPage.openFileChanges();
        ChangeRequestEntityDiff entityDiff =
            fileChangesPane.getEntityDiff(serializedReference, "XWiki.StyleSheetExtension[0]");
        List<String> diff = entityDiff.getDiff("Code");
        assertEquals(List.of(
            "@@ -1,1 +1,1 @@",
            "-.customClass { color: <del>r</del>e<del>d</del>; }",
            "+.customClass { color: <ins>blu</ins>e; }"
        ), diff);
        assertFalse(entityDiff.hasMessages());

        discussionEditor = entityDiff.clickAddingDiffComment("Code", 1, LineChange.REMOVED);
        discussionEditor.setContent("This is a diff comment");
        beforeComment = new Date();
        discussionEditor.clickSave();
        changeRequestPage.waitForTimelineRefresh();
        afterComment = new Date();

        assertTrue(entityDiff.hasMessages());
        assertEquals(1, entityDiff.countMessages());

        Map<EntityDiffCoordinate, List<MessageElement>> allMessages = entityDiff.getAllMessages();
        assertEquals(1, allMessages.size());

        EntityDiffCoordinate expectedCoordinate = new EntityDiffCoordinate("code", LineChange.REMOVED, 1);
        List<MessageElement> messageElements = allMessages.get(expectedCoordinate);
        assertEquals(1, messageElements.size());

        message = messageElements.get(0);
        assertEquals("This is a diff comment", message.getContent());
        assertTrue(message.isExpanded());
        assertFalse(message.isAnswer());
        assertFalse(message.isOutdated());
        assertEquals("xwiki:XWiki." + CR_CREATOR, message.getAuthor());

        descriptionPane = changeRequestPage.openDescription();
        descriptionPane.waitUntilEventsSize(3);

        events = descriptionPane.getEvents();
        event = events.get(2);
        assertEquals("changerequest.discussions", event.getEventType());
        message = event.getMessage();
        assertEquals("xwiki:XWiki." + CR_CREATOR, message.getAuthor());
        assertEquals("This is a diff comment", message.getContent());
        messageDate = message.getDate();
        assertTrue(beforeComment.before(messageDate) && afterComment.after(messageDate));
        assertTrue(message.isExpanded());
        assertFalse(message.isOutdated());

        assertTrue(message instanceof DiffMessageElement);
        DiffMessageElement diffMessage = (DiffMessageElement) message;
        assertTrue(diffMessage.isDiffDisplayed());
        assertEquals(List.of(
            "@@ -1,1 +1,0 @@",
            "-.customClass { color: <del>r</del>e<del>d</del>; }"
        ), diffMessage.getDiff());
        assertFalse(diffMessage.isDetailsExpanded());
        assertEquals("Some title", diffMessage.getPageTitle());
        assertEquals(String.format("object:%s^XWiki.StyleSheetExtension[0]", testReference),
            diffMessage.getFullReference());

        diffMessage.clickToggleDetails();
        assertTrue(diffMessage.isDetailsExpanded());
        assertEquals("Property of object XWiki.StyleSheetExtension[0]", diffMessage.getLocationDetails());
        assertEquals("Property name: code", diffMessage.getPropertyDetails());

        testUtils.loginAsSuperAdmin();
        testUtils.gotoPage(testReference, "save", "content", Collections.singletonMap("content", "Some new content"));

        testUtils.login(CR_CREATOR, CR_CREATOR);
        testUtils.gotoPage(changeRequestUrl);
        changeRequestPage = new ChangeRequestPage();

        descriptionPane = changeRequestPage.openDescription();
        events = descriptionPane.getEvents();
        assertEquals(3, events.size());

        event = events.get(2);
        assertEquals("changerequest.discussions", event.getEventType());
        message = event.getMessage();
        assertFalse(message.isOutdated());

        fileChangesPane = changeRequestPage.openFileChanges();
        assertTrue(fileChangesPane.isDiffOutdated(serializedReference));

        entityDiff = fileChangesPane.getEntityDiff(serializedReference, "XWiki.StyleSheetExtension[0]");
        assertTrue(entityDiff.hasMessages());
        assertEquals(1, entityDiff.countMessages());

        allMessages = entityDiff.getAllMessages();
        assertEquals(1, allMessages.size());

        expectedCoordinate = new EntityDiffCoordinate("code", LineChange.REMOVED, 1);
        messageElements = allMessages.get(expectedCoordinate);
        assertEquals(1, messageElements.size());

        message = messageElements.get(0);
        assertEquals("This is a diff comment", message.getContent());
        assertTrue(message.isExpanded());
        assertFalse(message.isAnswer());
        assertFalse(message.isOutdated());
        assertEquals("xwiki:XWiki." + CR_CREATOR, message.getAuthor());

        changeRequestPage =
            fileChangesPane.getFileChangesListLiveData().getFileChangeWithReference(serializedReference)
                .clickRefresh(false);

        descriptionPane = changeRequestPage.openDescription();
        descriptionPane.waitUntilEventsSize(4);

        events = descriptionPane.getEvents();
        event = events.get(2);
        assertEquals("changerequest.discussions", event.getEventType());
        message = event.getMessage();
        assertFalse(message.isOutdated());

        fileChangesPane = changeRequestPage.openFileChanges();
        fileChangesPane.getFileChangesListLiveData().getFileChangeWithReference(serializedReference).clickEdit();
        ExtendedEditPage<CKEditor> editPage = new ExtendedEditPage<>(new CKEditor("content"));

        editPage.getWrappedEditor().getRichTextArea().setContent("Change request content");
        changeRequestPage = editPage.clickSaveAsChangeRequestInExistingCR(true);
        descriptionPane = changeRequestPage.openDescription();
        descriptionPane.waitUntilEventsSize(5);

        fileChangesPane = changeRequestPage.openFileChanges();

        entityDiff = fileChangesPane.getEntityDiff(serializedReference, "Page properties");
        assertEquals(List.of(
            "@@ -1,1 +1,1 @@",
            "-<del>{co</del>n<del>t</del>e<del>nt=Some</del> <del>n</del>e<del>w</del> content<del>}</del>",
            "+<ins>Cha</ins>n<ins>g</ins>e <ins>r</ins>e<ins>quest</ins> content"
            ), entityDiff.getDiff("Content"));

        testUtils.login(CR_USER, CR_USER);
        testUtils.gotoPage(changeRequestUrl);
        changeRequestPage = new ChangeRequestPage();

        fileChangesPane = changeRequestPage.openFileChanges();
        entityDiff = fileChangesPane.getEntityDiff(serializedReference, "XWiki.StyleSheetExtension[0]");
        assertTrue(entityDiff.hasMessages());
        assertEquals(1, entityDiff.countMessages());

        allMessages = entityDiff.getAllMessages();
        assertEquals(1, allMessages.size());

        expectedCoordinate = new EntityDiffCoordinate("code", LineChange.REMOVED, 1);
        messageElements = allMessages.get(expectedCoordinate);
        assertEquals(1, messageElements.size());

        message = messageElements.get(0);
        assertEquals("This is a diff comment", message.getContent());
        assertTrue(message.isExpanded());
        assertFalse(message.isAnswer());
        assertFalse(message.isOutdated());
        assertEquals("xwiki:XWiki." + CR_CREATOR, message.getAuthor());

        entityDiff = fileChangesPane.getEntityDiff(serializedReference, "Page properties");
        assertFalse(entityDiff.hasMessages());

        discussionEditor = entityDiff.clickAddingDiffComment("Content", 1, LineChange.ADDED);
        discussionEditor.setContent("A new diff comment from CRUser");
        beforeComment = new Date();
        discussionEditor.clickSave();
        changeRequestPage.waitForTimelineRefresh();
        afterComment = new Date();

        assertTrue(entityDiff.hasMessages());
        assertEquals(1, entityDiff.countMessages());

        allMessages = entityDiff.getAllMessages();
        assertEquals(1, allMessages.size());

        expectedCoordinate = new EntityDiffCoordinate("content", LineChange.ADDED, 1);
        messageElements = allMessages.get(expectedCoordinate);
        assertEquals(1, messageElements.size());

        message = messageElements.get(0);
        assertEquals("A new diff comment from CRUser", message.getContent());
        assertTrue(message.isExpanded());
        assertFalse(message.isAnswer());
        assertFalse(message.isOutdated());
        assertEquals("xwiki:XWiki." + CR_USER, message.getAuthor());

        descriptionPane = changeRequestPage.openDescription();
        descriptionPane.waitUntilEventsSize(6);

        events = descriptionPane.getEvents();
        event = events.get(5);
        assertEquals("changerequest.discussions", event.getEventType());
        message = event.getMessage();
        assertEquals("xwiki:XWiki." + CR_USER, message.getAuthor());
        assertEquals("A new diff comment from CRUser", message.getContent());
        messageDate = message.getDate();
        assertTrue(beforeComment.before(messageDate) && afterComment.after(messageDate));
        assertTrue(message.isExpanded());
        assertFalse(message.isOutdated());

        assertTrue(message instanceof DiffMessageElement);
        diffMessage = (DiffMessageElement) message;
        assertTrue(diffMessage.isDiffDisplayed());
        assertEquals(List.of(
            "@@ -1,1 +1,1 @@",
            "-<del>{co</del>n<del>t</del>e<del>nt=Some</del> <del>n</del>e<del>w</del> content<del>}</del>",
            "+<ins>Cha</ins>n<ins>g</ins>e <ins>r</ins>e<ins>quest</ins> content"
        ), diffMessage.getDiff());
        assertFalse(diffMessage.isDetailsExpanded());
        assertEquals("Some title", diffMessage.getPageTitle());
        assertEquals(String.format("document:%s", testReference),
            diffMessage.getFullReference());

        diffMessage.clickToggleDetails();
        assertTrue(diffMessage.isDetailsExpanded());
        assertEquals("Page property", diffMessage.getLocationDetails());
        assertEquals("Property name: content", diffMessage.getPropertyDetails());

        assertTrue(changeRequestPage.isReviewButtonDisplayed());
        assertTrue(changeRequestPage.isReviewButtonEnabled());

        beforeComment = new Date();
        ReviewContainer reviewContainer = changeRequestPage.clickReviewButton();
        reviewContainer.selectApprove();
        reviewContainer.setComment("This is a review comment");
        changeRequestPage = reviewContainer.save();
        descriptionPane = changeRequestPage.openDescription();

        // There's 8 events at this point because the status changed to ready for merging
        descriptionPane.waitUntilEventsSize(8);
        afterComment = new Date();

        events = descriptionPane.getEvents();

        event = events.get(7);
        assertEquals("CRDiscussionITCRUser changed the status of the Change Request from ready for review to ready "
            + "for publication", event.getContent().getText());

        event = events.get(6);
        assertEquals("changerequest.review.added", event.getEventType());

        ReviewDiscussion reviewDiscussion = event.getReviewDiscussion();
        List<MessageElement> reviewMessages = reviewDiscussion.getMessages();
        assertEquals(1, reviewMessages.size());

        message = reviewMessages.get(0);
        assertEquals("xwiki:XWiki." + CR_USER, message.getAuthor());
        assertEquals("This is a review comment", message.getContent());
        messageDate = message.getDate();
        assertTrue(beforeComment.before(messageDate) && afterComment.after(messageDate));
        assertTrue(message.isExpanded());
        assertFalse(message.isOutdated());

        // Start replies
        event = events.get(2);
        assertEquals("changerequest.discussions", event.getEventType());
        message = event.getMessage();
        assertEquals("xwiki:XWiki." + CR_CREATOR, message.getAuthor());
        assertEquals("This is a diff comment", message.getContent());
        assertTrue(message.isExpanded());
        assertFalse(message.isOutdated());
        assertFalse(message.isAnswer());

        beforeComment = new Date();
        discussionEditor = message.clickReply();

        discussionEditor.setContent("Replying to the first diff message");
        discussionEditor.clickSave();
        changeRequestPage.waitForTimelineRefresh();
        descriptionPane.waitUntilEventsSize(9);
        afterComment = new Date();

        events = descriptionPane.getEvents();
        event = events.get(8);
        assertEquals("changerequest.discussions", event.getEventType());
        message = event.getMessage();
        assertEquals("xwiki:XWiki." + CR_USER, message.getAuthor());
        assertEquals("Replying to the first diff message", message.getContent());
        messageDate = message.getDate();
        assertTrue(beforeComment.before(messageDate) && afterComment.after(messageDate));
        assertTrue(message.isExpanded());
        assertFalse(message.isOutdated());
        assertTrue(message.isAnswer());

        assertTrue(message instanceof DiffMessageElement);
        diffMessage = (DiffMessageElement) message;
        assertTrue(diffMessage.isDiffDisplayed());
        assertEquals(List.of(
            "@@ -1,1 +1,0 @@",
            "-.customClass { color: <del>r</del>e<del>d</del>; }"
        ), diffMessage.getDiff());
        assertFalse(diffMessage.isDetailsExpanded());
        assertEquals("Some title", diffMessage.getPageTitle());
        assertEquals(String.format("object:%s^XWiki.StyleSheetExtension[0]", testReference),
            diffMessage.getFullReference());

        event = events.get(1);
        assertEquals("changerequest.discussions", event.getEventType());
        message = event.getMessage();
        assertEquals("xwiki:XWiki." + CR_CREATOR, message.getAuthor());
        assertEquals("This is a global comment", message.getContent());
        assertTrue(message.isExpanded());
        assertFalse(message.isOutdated());
        assertFalse(message.isAnswer());

        beforeComment = new Date();
        discussionEditor = message.clickReply();

        discussionEditor.setContent("Replying to the global message");
        discussionEditor.clickSave();
        changeRequestPage.waitForTimelineRefresh();
        descriptionPane.waitUntilEventsSize(10);
        afterComment = new Date();

        events = descriptionPane.getEvents();
        event = events.get(9);

        assertEquals("changerequest.discussions", event.getEventType());
        message = event.getMessage();
        assertEquals("xwiki:XWiki." + CR_USER, message.getAuthor());
        assertEquals("Replying to the global message", message.getContent());
        messageDate = message.getDate();
        assertTrue(beforeComment.before(messageDate) && afterComment.after(messageDate));
        assertTrue(message.isExpanded());
        assertFalse(message.isOutdated());
        assertTrue(message.isAnswer());
        assertFalse(message instanceof DiffMessageElement);

        testUtils.login(CR_CREATOR, CR_CREATOR);
        testUtils.gotoPage(changeRequestUrl);

        changeRequestPage = new ChangeRequestPage();
        ReviewsPane reviewsPane = changeRequestPage.openReviewsPane();
        List<ReviewElement> reviews = reviewsPane.getReviews();
        assertEquals(1, reviews.size());

        reviewDiscussion = reviews.get(0).getDiscussion();
        reviewMessages = reviewDiscussion.getMessages();
        assertEquals(1, reviewMessages.size());

        message = reviewMessages.get(0);
        assertEquals("xwiki:XWiki." + CR_USER, message.getAuthor());
        assertEquals("This is a review comment", message.getContent());
        assertTrue(message.isExpanded());
        assertFalse(message.isOutdated());
        assertFalse(message.isAnswer());

        beforeComment = new Date();
        discussionEditor = message.clickReply();
        discussionEditor.setContent("Replying to the review comment.");
        discussionEditor.clickSave();
        changeRequestPage.waitForTimelineRefresh();

        fileChangesPane = changeRequestPage.openFileChanges();
        entityDiff = fileChangesPane.getEntityDiff(serializedReference, "XWiki.StyleSheetExtension[0]");
        assertTrue(entityDiff.hasMessages());
        assertEquals(2, entityDiff.countMessages());

        allMessages = entityDiff.getAllMessages();
        assertEquals(1, allMessages.size());

        expectedCoordinate = new EntityDiffCoordinate("code", LineChange.REMOVED, 1);
        messageElements = allMessages.get(expectedCoordinate);
        assertEquals(2, messageElements.size());

        message = messageElements.get(0);
        assertEquals("This is a diff comment", message.getContent());
        assertTrue(message.isExpanded());
        assertFalse(message.isAnswer());
        assertFalse(message.isOutdated());
        assertEquals("xwiki:XWiki." + CR_CREATOR, message.getAuthor());

        message = messageElements.get(1);
        assertEquals("Replying to the first diff message", message.getContent());
        assertTrue(message.isExpanded());
        assertTrue(message.isAnswer());
        assertFalse(message.isOutdated());
        assertEquals("xwiki:XWiki." + CR_USER, message.getAuthor());

        afterComment = new Date();

        fileChangesPane.getFileChangesListLiveData().getFileChangeWithReference(serializedReference).clickEdit();
        editPage = new ExtendedEditPage<>(new CKEditor("content"));

        editPage.getWrappedEditor().getRichTextArea().setContent("A considerably different content");
        changeRequestPage = editPage.clickSaveAsChangeRequestInExistingCR(true);

        descriptionPane = changeRequestPage.openDescription();
        // 12 events: we also have a status changed here
        descriptionPane.waitUntilEventsSize(12);

        events = descriptionPane.getEvents();

        event = events.get(11);
        assertEquals("CRDiscussionITCRCreator changed the status of the Change Request from ready for publication "
            + "to ready for review", event.getContent().getText());

        // We check the review added in comments only there since we don't have a proper mechanism to wait...
        event = events.get(6);
        assertEquals("changerequest.review.added", event.getEventType());

        reviewDiscussion = event.getReviewDiscussion();
        reviewMessages = reviewDiscussion.getMessages();
        assertEquals(2, reviewMessages.size());

        message = reviewMessages.get(0);
        assertEquals("xwiki:XWiki." + CR_USER, message.getAuthor());
        assertEquals("This is a review comment", message.getContent());
        assertTrue(message.isExpanded());
        assertFalse(message.isOutdated());
        assertFalse(message.isAnswer());

        message = reviewMessages.get(1);
        assertEquals("xwiki:XWiki." + CR_CREATOR, message.getAuthor());
        assertEquals("Replying to the review comment.", message.getContent());
        messageDate = message.getDate();
        assertTrue(beforeComment.before(messageDate) && afterComment.after(messageDate));
        assertTrue(message.isExpanded());
        assertFalse(message.isOutdated());
        assertTrue(message.isAnswer());

        event = events.get(2);
        assertEquals("changerequest.discussions", event.getEventType());
        message = event.getMessage();
        assertEquals("xwiki:XWiki." + CR_CREATOR, message.getAuthor());
        assertEquals("This is a diff comment", message.getContent());
        assertTrue(message.isExpanded());
        assertFalse(message.isOutdated());

        assertTrue(message instanceof DiffMessageElement);
        diffMessage = (DiffMessageElement) message;
        assertTrue(diffMessage.isDiffDisplayed());
        assertEquals(List.of(
            "@@ -1,1 +1,0 @@",
            "-.customClass { color: <del>r</del>e<del>d</del>; }"
        ), diffMessage.getDiff());

        event = events.get(5);
        assertEquals("changerequest.discussions", event.getEventType());
        message = event.getMessage();
        assertFalse(message.isExpanded());
        assertTrue(message.isOutdated());
        message.clickExpand();
        assertEquals("xwiki:XWiki." + CR_USER, message.getAuthor());
        assertEquals("A new diff comment from CRUser", message.getContent());
        assertFalse(message.isAnswer());

        assertTrue(message instanceof DiffMessageElement);
        diffMessage = (DiffMessageElement) message;
        assertTrue(diffMessage.isDiffDisplayed());
        assertEquals(List.of(
            "@@ -1,1 +1,1 @@",
            "-<del>{co</del>n<del>t</del>e<del>nt=Some</del> <del>n</del>e<del>w</del> content<del>}</del>",
            "+<ins>Cha</ins>n<ins>g</ins>e <ins>r</ins>e<ins>quest</ins> content"
        ), diffMessage.getDiff());
        assertFalse(diffMessage.isDetailsExpanded());
        assertEquals("Some title", diffMessage.getPageTitle());
        assertEquals(String.format("document:%s", testReference),
            diffMessage.getFullReference());

        beforeComment = new Date();
        discussionEditor = message.clickReply();
        discussionEditor.setContent("Replying to an outdated diff message");
        discussionEditor.clickSave();
        changeRequestPage.waitForTimelineRefresh();
        descriptionPane.waitUntilEventsSize(13);
        afterComment = new Date();

        events = descriptionPane.getEvents();
        event = events.get(12);
        assertEquals("changerequest.discussions", event.getEventType());
        message = event.getMessage();
        assertFalse(message.isExpanded());
        assertTrue(message.isOutdated());
        message.clickExpand();
        assertEquals("xwiki:XWiki." + CR_CREATOR, message.getAuthor());
        assertEquals("Replying to an outdated diff message", message.getContent());
        messageDate = message.getDate();
        assertTrue(beforeComment.before(messageDate) && afterComment.after(messageDate));
        assertTrue(message.isAnswer());

        fileChangesPane = changeRequestPage.openFileChanges();
        entityDiff = fileChangesPane.getEntityDiff(serializedReference, "XWiki.StyleSheetExtension[0]");
        assertTrue(entityDiff.hasMessages());
        assertEquals(2, entityDiff.countMessages());

        allMessages = entityDiff.getAllMessages();
        assertEquals(1, allMessages.size());

        expectedCoordinate = new EntityDiffCoordinate("code", LineChange.REMOVED, 1);
        messageElements = allMessages.get(expectedCoordinate);
        assertEquals(2, messageElements.size());

        message = messageElements.get(0);
        assertEquals("This is a diff comment", message.getContent());
        assertTrue(message.isExpanded());
        assertFalse(message.isAnswer());
        assertFalse(message.isOutdated());
        assertEquals("xwiki:XWiki." + CR_CREATOR, message.getAuthor());

        message = messageElements.get(1);
        assertEquals("Replying to the first diff message", message.getContent());
        assertTrue(message.isExpanded());
        assertTrue(message.isAnswer());
        assertFalse(message.isOutdated());
        assertEquals("xwiki:XWiki." + CR_USER, message.getAuthor());

        // Replying in diff
        beforeComment = new Date();
        discussionEditor = message.clickReply();
        discussionEditor.setContent("Replying inside diff view");
        discussionEditor.clickSave();
        changeRequestPage.waitForTimelineRefresh();
        afterComment = new Date();

        assertTrue(entityDiff.hasMessages());
        assertEquals(3, entityDiff.countMessages());

        allMessages = entityDiff.getAllMessages();
        assertEquals(1, allMessages.size());
        messageElements = allMessages.get(expectedCoordinate);
        assertEquals(3, messageElements.size());

        message = messageElements.get(0);
        assertEquals("This is a diff comment", message.getContent());
        assertTrue(message.isExpanded());
        assertFalse(message.isAnswer());
        assertFalse(message.isOutdated());
        assertEquals("xwiki:XWiki." + CR_CREATOR, message.getAuthor());

        message = messageElements.get(1);
        assertEquals("Replying to the first diff message", message.getContent());
        assertTrue(message.isExpanded());
        assertTrue(message.isAnswer());
        assertFalse(message.isOutdated());
        assertEquals("xwiki:XWiki." + CR_USER, message.getAuthor());

        message = messageElements.get(2);
        assertEquals("Replying inside diff view", message.getContent());
        assertTrue(message.isExpanded());
        assertTrue(message.isAnswer());
        assertFalse(message.isOutdated());
        assertEquals("xwiki:XWiki." + CR_CREATOR, message.getAuthor());

        descriptionPane = changeRequestPage.openDescription();
        event = descriptionPane.getEvents().get(13);

        assertEquals("changerequest.discussions", event.getEventType());
        message = event.getMessage();
        assertTrue(message.isExpanded());
        assertFalse(message.isOutdated());
        assertEquals("xwiki:XWiki." + CR_CREATOR, message.getAuthor());
        assertEquals("Replying inside diff view", message.getContent());
        messageDate = message.getDate();
        assertTrue(beforeComment.before(messageDate) && afterComment.after(messageDate));
        assertTrue(message.isAnswer());
    }
}
