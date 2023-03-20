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

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.xwiki.contrib.changerequest.test.po.ChangeRequestPage;
import org.xwiki.contrib.changerequest.test.po.ChangeRequestSaveModal;
import org.xwiki.contrib.changerequest.test.po.ExtendedEditPage;
import org.xwiki.contrib.changerequest.test.po.description.DescriptionPane;
import org.xwiki.contrib.changerequest.test.po.description.TimelineEvent;
import org.xwiki.contrib.changerequest.test.po.discussion.DiffMessageElement;
import org.xwiki.contrib.changerequest.test.po.discussion.DiscussionEditor;
import org.xwiki.contrib.changerequest.test.po.discussion.MessageElement;
import org.xwiki.contrib.changerequest.test.po.filechanges.FileChangesPane;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.diff.EntityDiff;
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
    private static final String TEST_USER_PREFIX = "CRAddChangesIT";
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
    }

    @Test
    void createDiscussions(TestUtils testUtils, TestReference testReference)
    {
        String serializedReference = testReference.getLocalDocumentReference().toString();
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
        DescriptionPane descriptionPane = changeRequestPage.openDescription();

        assertTrue(descriptionPane.hasCommentButton());
        DiscussionEditor discussionEditor = descriptionPane.clickCommentButton();
        discussionEditor.setContent("This is a global comment");
        Date beforeComment = new Date();
        discussionEditor.clickSave();
        descriptionPane.waitForTimelineRefresh();
        Date afterComment = new Date();
        descriptionPane.waitUntilEventsSize(2);
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

        FileChangesPane fileChangesPane = changeRequestPage.openFileChanges();
        EntityDiff entityDiff = fileChangesPane.getEntityDiff(serializedReference, "XWiki.StyleSheetExtension[0]");
        List<String> diff = entityDiff.getDiff("Code");
        assertEquals(List.of(
            "@@ -1,1 +1,1 @@",
            "-.customClass { color: <del>r</del>e<del>d</del>; }",
            "+.customClass { color: <ins>blu</ins>e; }"
        ), diff);

        discussionEditor = fileChangesPane.clickAddingDiffComment(serializedReference, "XWiki.StyleSheetExtension[0]",
            "Code", 1, FileChangesPane.LineType.DELETION);
        discussionEditor.setContent("This is a diff comment");
        beforeComment = new Date();
        discussionEditor.clickSave();
        descriptionPane.waitForTimelineRefresh();
        afterComment = new Date();

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

        // TODO:
        // - Add review message
        // - Reply to messages (global, diff, review)
        // - Add another diff message
        // - Perform changes and check outdated messages
    }
}
