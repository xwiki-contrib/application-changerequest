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
package org.xwiki.contrib.changerequest.templates;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.script.ScriptContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestStatus;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestCreatedRecordableEvent;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestFileChangeAddedRecordableEvent;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestRebasedRecordableEvent;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestReviewAddedRecordableEvent;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestStatusChangedRecordableEvent;
import org.xwiki.contrib.changerequest.notifications.events.StaleChangeRequestRecordableEvent;
import org.xwiki.contrib.changerequest.script.ChangeRequestScriptService;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.RecordableEventDescriptor;
import org.xwiki.eventstream.internal.DefaultEvent;
import org.xwiki.eventstream.script.EventStreamScriptService;
import org.xwiki.icon.IconManagerScriptService;
import org.xwiki.localization.script.LocalizationScriptService;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.platform.date.script.DateScriptService;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.script.service.ScriptService;
import org.xwiki.template.TemplateManager;
import org.xwiki.template.script.TemplateScriptService;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.wiki.script.WikiManagerScriptService;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests the {@code changerequest/alert} notification templates.
 *
 * @version $Id: 2cafc94346ba4061bd85f680ba0ea477de6467cc $
 */
@ComponentList({
    TemplateScriptService.class
})
@HTML50ComponentList
class NotificationEmailHtmlPageTest extends PageTest
{
    private static final DocumentReference USER_REFERENCE = new DocumentReference("xwiki", "XWiki", "User");
    private static final DocumentReference USER_REFERENCE_2 = new DocumentReference("xwiki", "XWiki", "User2");

    private static final String TEMPLATE_PATH = "changerequest/email/html/%s.vm";
    private static final String NOTIF_APPLI_NAME = "Change Request";
    private static final String CR_ID_PARAM_KEY = "changerequest.id";
    private static final String FILECHANGE_ID_PARAM_KEY = "changerequest.filechange.id";
    private static final String DESCRIPTOR_ICON = "branch";
    private static final String CR_STATUS_OLD_PARAM_KEY = "changerequest.status.old";
    private static final String CR_STATUS_NEW_PARAM_KEY = "changerequest.status.new";

    @Mock
    private ChangeRequestScriptService changeRequestScriptService;

    @Mock
    private EventStreamScriptService eventStreamScriptService;

    @Mock
    private LocalizationScriptService localizationScriptService;

    @Mock
    private DateScriptService dateScriptService;

    @Mock
    private IconManagerScriptService iconManagerScriptService;

    @Mock
    private WikiManagerScriptService wikiManagerScriptService;

    private TemplateManager templateManager;

    private ScriptContext scriptContext;

    @BeforeEach
    void setUp() throws Exception
    {
        this.scriptContext = this.oldcore.getMocker().<ScriptContextManager>getInstance(ScriptContextManager.class)
            .getCurrentScriptContext();
        this.templateManager = this.oldcore.getMocker().getInstance(TemplateManager.class);
        this.oldcore.getMocker().registerComponent(ScriptService.class, "eventstream", this.eventStreamScriptService);
        this.oldcore.getMocker().registerComponent(ScriptService.class, "localization", this.localizationScriptService);
        this.oldcore.getMocker().registerComponent(ScriptService.class, "date", this.dateScriptService);
        this.oldcore.getMocker().registerComponent(ScriptService.class, "icon", this.iconManagerScriptService);
        this.oldcore.getMocker().registerComponent(ScriptService.class, "changerequest",
            this.changeRequestScriptService);
        this.oldcore.getMocker().registerComponent(ScriptService.class, "wiki", this.wikiManagerScriptService);


        when(this.iconManagerScriptService.renderHTML(DESCRIPTOR_ICON)).thenReturn("Icon Branch");
        when(this.localizationScriptService.render(NOTIF_APPLI_NAME)).thenReturn("CR APPLI");
        when(this.localizationScriptService.render("notifications.macro.showEventDetails")).thenReturn("Show details");

        when(this.oldcore.getMockRightService().hasProgrammingRights(this.context)).thenReturn(true);

        // Mock the user's name.
        when(this.oldcore.getSpyXWiki().getPlainUserName(USER_REFERENCE, this.context)).thenReturn("First & Name");
        when(this.oldcore.getSpyXWiki().getPlainUserName(USER_REFERENCE_2, this.context)).thenReturn("User2");
    }

    @Test
    void createNotificationTemplate() throws Exception
    {
        RecordableEventDescriptor recordableEventDescriptor = mock(RecordableEventDescriptor.class);
        when(this.eventStreamScriptService
            .getDescriptorForEventType(ChangeRequestCreatedRecordableEvent.EVENT_NAME, true))
            .thenReturn(recordableEventDescriptor);
        when(recordableEventDescriptor.getApplicationName()).thenReturn(NOTIF_APPLI_NAME);
        when(recordableEventDescriptor.getApplicationIcon()).thenReturn(DESCRIPTOR_ICON);

        String fileChangeId = "fileChangeId";
        String changeRequestId = "changeRequestId";

        DocumentReference crDocReference = new DocumentReference("xwiki", "ChangeRequest", "CR1");
        XWikiDocument crDoc = new XWikiDocument(crDocReference);
        crDoc.setTitle("CR 1");
        crDoc.setSyntax(Syntax.XWIKI_2_1);
        this.oldcore.getSpyXWiki().saveDocument(crDoc, this.context);

        DocumentReference modifiedDocReference = new DocumentReference("xwiki", "Space", "ModifiedDoc");
        XWikiDocument modifiedDoc = new XWikiDocument(modifiedDocReference);
        modifiedDoc.setTitle("Modified document");
        modifiedDoc.setSyntax(Syntax.XWIKI_2_1);
        this.oldcore.getSpyXWiki().saveDocument(modifiedDoc, this.context);

        ChangeRequest changeRequest = mock(ChangeRequest.class);
        when(this.changeRequestScriptService.getChangeRequest(changeRequestId)).thenReturn(Optional.of(changeRequest));
        FileChange fileChange = mock(FileChange.class);
        when(changeRequest.getFileChangeById(fileChangeId)).thenReturn(Optional.of(fileChange));
        when(fileChange.getTargetEntity()).thenReturn(modifiedDocReference);

        Event testEvent = new DefaultEvent();
        testEvent.setApplication(NOTIF_APPLI_NAME);
        testEvent.setType(ChangeRequestCreatedRecordableEvent.EVENT_NAME);
        testEvent.setCustom(Map.of(
            CR_ID_PARAM_KEY, changeRequestId,
            FILECHANGE_ID_PARAM_KEY, fileChangeId
        ));

        // Mock date formatting to avoid issues with timezones.
        Date testDate = new Date(12);
        when(this.dateScriptService.displayTimeAgo(testDate)).thenReturn("A few minutes ago");
        testEvent.setDate(testDate);
        testEvent.setUser(USER_REFERENCE);
        testEvent.setDocument(crDocReference);

        String docTitle = "Modified doc title";
        when(this.changeRequestScriptService.getPageTitle(changeRequestId, fileChangeId)).thenReturn(docTitle);
        this.context.setOriginalWikiId("xwiki");
        when(this.localizationScriptService.render(
            eq("changerequest.notifications.create.description"),
            any(Collection.class)))
            .then(invocationOnMock -> {
            List<String> parameters = invocationOnMock.getArgument(1);
            assertEquals(2, parameters.size());

            return String.format("Change request created by %s for %s", parameters.get(0), parameters.get(1));
        });

        this.scriptContext.setAttribute("event", new CompositeEvent(testEvent), ScriptContext.ENGINE_SCOPE);

        String result = this.templateManager.render(String.format(TEMPLATE_PATH, "create"));
        String expectedResult = "<table width=\"100%\">\n"
            + "    <tr>\n"
            + "                        <td width=\"25%\" style=\"width: 25%; vertical-align: top;\" valign=\"top\">\n"
            + "            <strong>Change Request</strong>\n"
            + "\n"
            + "      </td>\n"
            + "                        <td style=\"vertical-align: top;\" valign=\"top\">\n"
            + "                                                    \n"
            + "                                            <div>\n"
            + "              <a color=\"#0088CC\" style=\"color: #0088CC; text-decoration: none;\" "
            + "href=\"/xwiki/bin/view/ChangeRequest/CR1\">Modified doc title</a>\n"
            + "</div>\n"
            + "                    Change request created by             <span class=\"notification-event-user\" "
            + "data-xwiki-lightbox=\"false\">\n"
            + "    <img src=\"/xwiki/bin/skin/skins/flamingo/icons/xwiki/noavatar.png\" alt=\"First &#38; Name\"/>"
            + "User  </span> for                         \n"
            + "                                            <div>\n"
            + "              <a color=\"#0088CC\" style=\"color: #0088CC; text-decoration: none;\" "
            + "href=\"/xwiki/bin/view/ChangeRequest/CR1\">Modified doc title</a>\n"
            + "</div>\n"
            + "\n"
            + "      <div>\n"
            + "    <small style=\"color: #777777; font-size: 0.8em;\">\n"
            + "      A few minutes ago\n"
            + "    </small>\n"
            + "  </div>\n"
            + "\n"
            + "      </td>\n"
            + "    </tr>\n"
            + "  </table>";
        assertEquals(expectedResult, result.trim());

        // Before 1.13 events were sent with the modified doc as data, not the CR doc
        testEvent.setDocument(modifiedDocReference);
        assertEquals(expectedResult, result.trim());
    }

    @Test
    void fileChangeAddedNotificationTemplate() throws Exception
    {
        RecordableEventDescriptor recordableEventDescriptor = mock(RecordableEventDescriptor.class);
        when(this.eventStreamScriptService
            .getDescriptorForEventType(ChangeRequestFileChangeAddedRecordableEvent.EVENT_NAME, true))
            .thenReturn(recordableEventDescriptor);
        when(recordableEventDescriptor.getApplicationName()).thenReturn(NOTIF_APPLI_NAME);
        when(recordableEventDescriptor.getApplicationIcon()).thenReturn(DESCRIPTOR_ICON);
        when(recordableEventDescriptor.getEventTypeIcon()).thenReturn(DESCRIPTOR_ICON);

        String fileChangeId = "fileChangeId";
        String changeRequestId = "changeRequestId";

        String fileChangeId2 = "fileChangeId2";
        String changeRequestId2 = "changeRequestId2";

        DocumentReference crDocReference = new DocumentReference("xwiki", "ChangeRequest", "CR1");
        XWikiDocument crDoc = new XWikiDocument(crDocReference);
        crDoc.setTitle("CR 1");
        crDoc.setSyntax(Syntax.XWIKI_2_1);
        this.oldcore.getSpyXWiki().saveDocument(crDoc, this.context);

        DocumentReference crDocReference2 = new DocumentReference("xwiki", "ChangeRequest", "CR2");
        XWikiDocument crDoc2 = new XWikiDocument(crDocReference2);
        crDoc2.setTitle("CR 2");
        crDoc2.setSyntax(Syntax.XWIKI_2_1);
        this.oldcore.getSpyXWiki().saveDocument(crDoc2, this.context);

        DocumentReference modifiedDocReference = new DocumentReference("xwiki", "Space", "ModifiedDoc");
        XWikiDocument modifiedDoc = new XWikiDocument(modifiedDocReference);
        modifiedDoc.setTitle("Modified document");
        modifiedDoc.setSyntax(Syntax.XWIKI_2_1);
        this.oldcore.getSpyXWiki().saveDocument(modifiedDoc, this.context);

        ChangeRequest changeRequest = mock(ChangeRequest.class);
        when(this.changeRequestScriptService.getChangeRequest(changeRequestId)).thenReturn(Optional.of(changeRequest));
        FileChange fileChange = mock(FileChange.class);
        when(changeRequest.getFileChangeById(fileChangeId)).thenReturn(Optional.of(fileChange));
        when(fileChange.getTargetEntity()).thenReturn(modifiedDocReference);

        ChangeRequest changeRequest2 = mock(ChangeRequest.class);
        when(this.changeRequestScriptService.getChangeRequest(changeRequestId2))
            .thenReturn(Optional.of(changeRequest2));
        FileChange fileChange2 = mock(FileChange.class);
        when(changeRequest2.getFileChangeById(fileChangeId2)).thenReturn(Optional.of(fileChange2));
        when(fileChange2.getTargetEntity()).thenReturn(modifiedDocReference);

        Event testEvent1 = new DefaultEvent();
        testEvent1.setApplication(NOTIF_APPLI_NAME);
        testEvent1.setType(ChangeRequestFileChangeAddedRecordableEvent.EVENT_NAME);
        testEvent1.setCustom(Map.of(
            CR_ID_PARAM_KEY, changeRequestId,
            FILECHANGE_ID_PARAM_KEY, fileChangeId
        ));
        Date testDate = new Date(1212121);
        when(this.dateScriptService.displayTimeAgo(testDate)).thenReturn("A few minutes ago");
        testEvent1.setDate(testDate);
        testEvent1.setUser(USER_REFERENCE);
        testEvent1.setDocument(crDocReference);
        testEvent1.setDocumentVersion(crDoc.getVersion());

        Event testEvent2 = new DefaultEvent();
        testEvent2.setApplication(NOTIF_APPLI_NAME);
        testEvent2.setType(ChangeRequestFileChangeAddedRecordableEvent.EVENT_NAME);
        testEvent2.setCustom(Map.of(
            CR_ID_PARAM_KEY, changeRequestId2,
            FILECHANGE_ID_PARAM_KEY, fileChangeId2
        ));
        Date testDate2 = new Date(5858558);
        when(this.dateScriptService.displayTimeAgo(testDate2)).thenReturn("A few hours ago");
        testEvent2.setDate(testDate2);
        testEvent2.setUser(USER_REFERENCE_2);
        testEvent2.setDocument(crDocReference2);
        testEvent2.setDocumentVersion(crDoc2.getVersion());

        String docTitle = "Modified doc title";
        when(this.changeRequestScriptService.getPageTitle(changeRequestId, fileChangeId)).thenReturn(docTitle);
        String docTitle2 = "Modified doc title 2";
        when(this.changeRequestScriptService.getPageTitle(changeRequestId2, fileChangeId2)).thenReturn(docTitle2);

        this.context.setOriginalWikiId("xwiki");
        when(this.localizationScriptService.render(
            eq("notifications.events.changerequest.filechange.added.description.by.users"),
            any(Collection.class)))
            .then(invocationOnMock -> {
                List<String> parameters = invocationOnMock.getArgument(1);
                assertEquals(1, parameters.size());

                return String.format("Change request filechange added by %s", parameters.get(0));
            });

        when(this.localizationScriptService.render(
            eq("changerequest.notifications.filechange.added.description"),
            any(Collection.class)))
            .then(invocationOnMock -> {
                List<String> parameters = invocationOnMock.getArgument(1);
                assertEquals(1, parameters.size());

                return String.format("File change %s was added", parameters.get(0));
            });

        CompositeEvent compositeEvent = new CompositeEvent(testEvent1);
        compositeEvent.add(testEvent2, 0);
        Map<Event, DocumentReference> crReferences = Map.of(testEvent1, crDocReference, testEvent2, crDocReference2);
        this.scriptContext.setAttribute("changeRequestReferences", crReferences, ScriptContext.ENGINE_SCOPE);
        this.scriptContext.setAttribute("event", compositeEvent, ScriptContext.ENGINE_SCOPE);

        String result = this.templateManager.render(String.format(TEMPLATE_PATH, "filechange.added"));
        String expectedResult = "<table width=\"100%\">\n"
            + "    <tr>\n"
            + "                        <td width=\"25%\" style=\"width: 25%; vertical-align: top;\" valign=\"top\">\n"
            + "            <strong>Change Request</strong>\n"
            + "\n"
            + "      </td>\n"
            + "                        <td style=\"vertical-align: top;\" valign=\"top\">\n"
            + "                                                \n"
            + "                                                                                                       "
            + "         <div>\n"
            + "              <a color=\"#0088CC\" style=\"color: #0088CC; text-decoration: none;\" "
            + "href=\"/xwiki/bin/view/Space/ModifiedDoc\">Modified doc title 2</a>\n"
            + "</div>\n"
            + "                      <div>\n"
            + "      Change request filechange added by 2\n"
            + "    </div>\n"
            + "    <div>\n"
            + "                            <img src=\"cid:User2.jpg\" alt=\"U\" width=\"16\" height=\"16\" "
            + "style=\"vertical-align: middle;\"/>\n"
            + "                           <img src=\"cid:User.jpg\" alt=\"U\" width=\"16\" height=\"16\" "
            + "style=\"vertical-align: middle;\"/>\n"
            + "             </div>\n"
            + "        <div>\n"
            + "    <small style=\"color: #777777; font-size: 0.8em;\">\n"
            + "      A few hours ago\n"
            + "    </small>\n"
            + "  </div>\n"
            + "                                <table width=\"100%\" style=\"margin: 5px 0; font-size: 0.8em; "
            + "border-top: 1px dashed #e8e8e8\">\n"
            + "                                                        <tr style=\"vertical-align: top;\">\n"
            + "                <td width=\"33%\" style=\"overflow: hidden;\">        <img src=\"cid:User2.jpg\" "
            + "alt=\"U\" width=\"16\" height=\"16\" style=\"vertical-align: middle;\"/>\n"
            + "     <a color=\"#0088CC\" style=\"color: #0088CC; text-decoration: none;\" "
            + "href=\"/xwiki/bin/view/XWiki/User2\">User2</a>\n"
            + "</td>\n"
            + "                <td width=\"45%\">File change CR 2 was added</td>\n"
            + "                <td>\n"
            + "                                              <a color=\"#0088CC\" style=\"color: #0088CC; "
            + "text-decoration: none;\" href=\"/xwiki/bin/view/ChangeRequest/CR2?viewer=changes&#38;rev2=1.1\">"
            + "1970/01/01 02:37</a>\n"
            + "                                    </td>\n"
            + "            </tr>\n"
            + "                                                        <tr style=\"vertical-align: top;\">\n"
            + "                <td width=\"33%\" style=\"overflow: hidden;\">        <img src=\"cid:User.jpg\" "
            + "alt=\"U\" width=\"16\" height=\"16\" style=\"vertical-align: middle;\"/>\n"
            + "     <a color=\"#0088CC\" style=\"color: #0088CC; text-decoration: none;\" "
            + "href=\"/xwiki/bin/view/XWiki/User\">First & Name</a>\n"
            + "</td>\n"
            + "                <td width=\"45%\">File change CR 1 was added</td>\n"
            + "                <td>\n"
            + "                                              <a color=\"#0088CC\" style=\"color: #0088CC; "
            + "text-decoration: none;\" href=\"/xwiki/bin/view/ChangeRequest/CR1?viewer=changes&#38;rev2=1.1\">"
            + "1970/01/01 01:20</a>\n"
            + "                                    </td>\n"
            + "            </tr>\n"
            + "            </table>\n"
            + "        \n"
            + "      </td>\n"
            + "    </tr>\n"
            + "  </table>";
        assertEquals(expectedResult, result.trim());

        // Before 1.13 events were sent with the modified doc as data, not the CR doc
        testEvent1.setDocument(modifiedDocReference);
        assertEquals(expectedResult, result.trim());
    }

    @Test
    void rebasedNotificationTemplate() throws Exception
    {
        RecordableEventDescriptor recordableEventDescriptor = mock(RecordableEventDescriptor.class);
        when(this.eventStreamScriptService
            .getDescriptorForEventType(ChangeRequestRebasedRecordableEvent.EVENT_NAME, true))
            .thenReturn(recordableEventDescriptor);
        when(recordableEventDescriptor.getApplicationName()).thenReturn(NOTIF_APPLI_NAME);
        when(recordableEventDescriptor.getApplicationIcon()).thenReturn(DESCRIPTOR_ICON);
        when(recordableEventDescriptor.getEventTypeIcon()).thenReturn(DESCRIPTOR_ICON);

        String changeRequestId = "changeRequestId";

        String changeRequestId2 = "changeRequestId2";

        DocumentReference crDocReference = new DocumentReference("xwiki", "ChangeRequest", "CR1");
        XWikiDocument crDoc = new XWikiDocument(crDocReference);
        crDoc.setTitle("CR 1");
        crDoc.setSyntax(Syntax.XWIKI_2_1);
        this.oldcore.getSpyXWiki().saveDocument(crDoc, this.context);

        DocumentReference crDocReference2 = new DocumentReference("xwiki", "ChangeRequest", "CR2");
        XWikiDocument crDoc2 = new XWikiDocument(crDocReference2);
        crDoc2.setTitle("CR 2");
        crDoc2.setSyntax(Syntax.XWIKI_2_1);
        this.oldcore.getSpyXWiki().saveDocument(crDoc2, this.context);

        DocumentReference modifiedDocReference = new DocumentReference("xwiki", "Space", "ModifiedDoc");
        XWikiDocument modifiedDoc = new XWikiDocument(modifiedDocReference);
        modifiedDoc.setTitle("Modified document");
        modifiedDoc.setSyntax(Syntax.XWIKI_2_1);
        this.oldcore.getSpyXWiki().saveDocument(modifiedDoc, this.context);

        ChangeRequest changeRequest = mock(ChangeRequest.class);
        when(this.changeRequestScriptService.getChangeRequest(changeRequestId)).thenReturn(Optional.of(changeRequest));

        ChangeRequest changeRequest2 = mock(ChangeRequest.class);
        when(this.changeRequestScriptService.getChangeRequest(changeRequestId2))
            .thenReturn(Optional.of(changeRequest2));

        Event testEvent1 = new DefaultEvent();
        testEvent1.setApplication(NOTIF_APPLI_NAME);
        testEvent1.setType(ChangeRequestRebasedRecordableEvent.EVENT_NAME);
        testEvent1.setCustom(Map.of(
            CR_ID_PARAM_KEY, changeRequestId
        ));
        Date testDate = new Date(1212121);
        when(this.dateScriptService.displayTimeAgo(testDate)).thenReturn("A few minutes ago");
        testEvent1.setDate(testDate);
        testEvent1.setUser(USER_REFERENCE);
        testEvent1.setDocument(crDocReference);

        Event testEvent2 = new DefaultEvent();
        testEvent2.setApplication(NOTIF_APPLI_NAME);
        testEvent2.setType(ChangeRequestRebasedRecordableEvent.EVENT_NAME);
        testEvent2.setCustom(Map.of(
            CR_ID_PARAM_KEY, changeRequestId2
        ));
        Date testDate2 = new Date(5858558);
        when(this.dateScriptService.displayTimeAgo(testDate2)).thenReturn("A few hours ago");
        testEvent2.setDate(testDate2);
        testEvent2.setUser(USER_REFERENCE_2);
        testEvent2.setDocument(crDocReference2);

        this.context.setOriginalWikiId("xwiki");
        when(this.localizationScriptService.render(
            eq("notifications.events.changerequest.rebased.description.by.users"),
            any(Collection.class)))
            .then(invocationOnMock -> {
                List<String> parameters = invocationOnMock.getArgument(1);
                assertEquals(1, parameters.size());

                return String.format("Change request rebased by %s", parameters.get(0));
            });

        when(this.localizationScriptService.render(
            eq("changerequest.notifications.rebased.description"),
            any(Collection.class)))
            .then(invocationOnMock -> {
                List<String> parameters = invocationOnMock.getArgument(1);
                assertEquals(1, parameters.size());

                return String.format("CR %s was rebased", parameters.get(0));
            });

        CompositeEvent compositeEvent = new CompositeEvent(testEvent1);
        compositeEvent.add(testEvent2, 0);
        Map<Event, DocumentReference> crReferences = Map.of(testEvent1, crDocReference, testEvent2, crDocReference2);
        this.scriptContext.setAttribute("changeRequestReferences", crReferences, ScriptContext.ENGINE_SCOPE);
        this.scriptContext.setAttribute("event", compositeEvent, ScriptContext.ENGINE_SCOPE);

        String result = this.templateManager.render(String.format(TEMPLATE_PATH, "rebased"));
        String expectedResult = "<table width=\"100%\">\n"
            + "    <tr>\n"
            + "                        <td width=\"25%\" style=\"width: 25%; vertical-align: top;\" valign=\"top\">\n"
            + "            <strong>Change Request</strong>\n"
            + "\n"
            + "      </td>\n"
            + "                        <td style=\"vertical-align: top;\" valign=\"top\">\n"
            + "                              <a color=\"#0088CC\" style=\"color: #0088CC; text-decoration: none;\" "
            + "href=\"/xwiki/bin/view/ChangeRequest/CR2\">CR 2</a>\n"
            + "                      <div>\n"
            + "      Change request rebased by 2\n"
            + "    </div>\n"
            + "    <div>\n"
            + "                            <img src=\"cid:User2.jpg\" alt=\"U\" width=\"16\" height=\"16\" "
            + "style=\"vertical-align: middle;\"/>\n"
            + "                           <img src=\"cid:User.jpg\" alt=\"U\" width=\"16\" height=\"16\" "
            + "style=\"vertical-align: middle;\"/>\n"
            + "             </div>\n"
            + "        <div>\n"
            + "    <small style=\"color: #777777; font-size: 0.8em;\">\n"
            + "      A few hours ago\n"
            + "    </small>\n"
            + "  </div>\n"
            + "                                <table width=\"100%\" style=\"margin: 5px 0; font-size: 0.8em; "
            + "border-top: 1px dashed #e8e8e8\">\n"
            + "                                            <tr style=\"vertical-align: top;\">\n"
            + "                <td width=\"33%\" style=\"overflow: hidden;\">        <img src=\"cid:User2.jpg\" "
            + "alt=\"U\" width=\"16\" height=\"16\" style=\"vertical-align: middle;\"/>\n"
            + "     <a color=\"#0088CC\" style=\"color: #0088CC; text-decoration: none;\" "
            + "href=\"/xwiki/bin/view/XWiki/User2\">User2</a>\n"
            + "</td>\n"
            + "                <td width=\"45%\">CR CR 2 was rebased</td>\n"
            + "                <td>\n"
            + "                                              <a color=\"#0088CC\" style=\"color: #0088CC; "
            + "text-decoration: none;\" href=\"/xwiki/bin/view/ChangeRequest/CR2\">1970/01/01 02:37</a>\n"
            + "                                    </td>\n"
            + "            </tr>\n"
            + "                                            <tr style=\"vertical-align: top;\">\n"
            + "                <td width=\"33%\" style=\"overflow: hidden;\">        <img src=\"cid:User.jpg\" "
            + "alt=\"U\" width=\"16\" height=\"16\" style=\"vertical-align: middle;\"/>\n"
            + "     <a color=\"#0088CC\" style=\"color: #0088CC; text-decoration: none;\" "
            + "href=\"/xwiki/bin/view/XWiki/User\">First & Name</a>\n"
            + "</td>\n"
            + "                <td width=\"45%\">CR CR 1 was rebased</td>\n"
            + "                <td>\n"
            + "                                              <a color=\"#0088CC\" style=\"color: #0088CC; "
            + "text-decoration: none;\" href=\"/xwiki/bin/view/ChangeRequest/CR1\">1970/01/01 01:20</a>\n"
            + "                                    </td>\n"
            + "            </tr>\n"
            + "            </table>\n"
            + "        \n"
            + "      </td>\n"
            + "    </tr>\n"
            + "  </table>";
        assertEquals(expectedResult, result.trim());

        // Before 1.13 events were sent with the modified doc as data, not the CR doc
        testEvent1.setDocument(modifiedDocReference);
        assertEquals(expectedResult, result.trim());
    }

    @Test
    void reviewAddedNotificationTemplate() throws Exception
    {
        RecordableEventDescriptor recordableEventDescriptor = mock(RecordableEventDescriptor.class);
        when(this.eventStreamScriptService
            .getDescriptorForEventType(ChangeRequestReviewAddedRecordableEvent.EVENT_NAME, true))
            .thenReturn(recordableEventDescriptor);
        when(recordableEventDescriptor.getApplicationName()).thenReturn(NOTIF_APPLI_NAME);
        when(recordableEventDescriptor.getApplicationIcon()).thenReturn(DESCRIPTOR_ICON);
        when(recordableEventDescriptor.getEventTypeIcon()).thenReturn(DESCRIPTOR_ICON);

        String changeRequestId = "changeRequestId";

        String changeRequestId2 = "changeRequestId2";

        DocumentReference crDocReference = new DocumentReference("xwiki", "ChangeRequest", "CR1");
        XWikiDocument crDoc = new XWikiDocument(crDocReference);
        crDoc.setTitle("CR 1");
        crDoc.setSyntax(Syntax.XWIKI_2_1);
        this.oldcore.getSpyXWiki().saveDocument(crDoc, this.context);

        DocumentReference crDocReference2 = new DocumentReference("xwiki", "ChangeRequest", "CR2");
        XWikiDocument crDoc2 = new XWikiDocument(crDocReference2);
        crDoc2.setTitle("CR 2");
        crDoc2.setSyntax(Syntax.XWIKI_2_1);
        this.oldcore.getSpyXWiki().saveDocument(crDoc2, this.context);

        DocumentReference modifiedDocReference = new DocumentReference("xwiki", "Space", "ModifiedDoc");
        XWikiDocument modifiedDoc = new XWikiDocument(modifiedDocReference);
        modifiedDoc.setTitle("Modified document");
        modifiedDoc.setSyntax(Syntax.XWIKI_2_1);
        this.oldcore.getSpyXWiki().saveDocument(modifiedDoc, this.context);

        ChangeRequest changeRequest = mock(ChangeRequest.class);
        when(this.changeRequestScriptService.getChangeRequest(changeRequestId)).thenReturn(Optional.of(changeRequest));

        ChangeRequest changeRequest2 = mock(ChangeRequest.class);
        when(this.changeRequestScriptService.getChangeRequest(changeRequestId2))
            .thenReturn(Optional.of(changeRequest2));

        Event testEvent1 = new DefaultEvent();
        testEvent1.setApplication(NOTIF_APPLI_NAME);
        testEvent1.setType(ChangeRequestReviewAddedRecordableEvent.EVENT_NAME);
        testEvent1.setCustom(Map.of(
            CR_ID_PARAM_KEY, changeRequestId
        ));
        Date testDate = new Date(1212121);
        when(this.dateScriptService.displayTimeAgo(testDate)).thenReturn("A few minutes ago");
        testEvent1.setDate(testDate);
        testEvent1.setUser(USER_REFERENCE);
        testEvent1.setDocument(crDocReference);
        testEvent1.setDocumentVersion(crDoc.getVersion());

        Event testEvent2 = new DefaultEvent();
        testEvent2.setApplication(NOTIF_APPLI_NAME);
        testEvent2.setType(ChangeRequestReviewAddedRecordableEvent.EVENT_NAME);
        testEvent2.setCustom(Map.of(
            CR_ID_PARAM_KEY, changeRequestId2
        ));
        Date testDate2 = new Date(5858558);
        when(this.dateScriptService.displayTimeAgo(testDate2)).thenReturn("A few hours ago");
        testEvent2.setDate(testDate2);
        testEvent2.setUser(USER_REFERENCE_2);
        testEvent2.setDocument(crDocReference2);
        testEvent2.setDocumentVersion(crDoc2.getVersion());

        this.context.setOriginalWikiId("xwiki");
        when(this.localizationScriptService.render(
            eq("notifications.events.changerequest.review.added.description.by.users"),
            any(Collection.class)))
            .then(invocationOnMock -> {
                List<String> parameters = invocationOnMock.getArgument(1);
                assertEquals(1, parameters.size());

                return String.format("Change request review added by %s", parameters.get(0));
            });

        when(this.localizationScriptService.render(
            eq("notifications.events.changerequest.review.added.description")))
            .then(invocationOnMock -> "added a review");

        CompositeEvent compositeEvent = new CompositeEvent(testEvent1);
        compositeEvent.add(testEvent2, 0);
        Map<Event, DocumentReference> crReferences = Map.of(testEvent1, crDocReference, testEvent2, crDocReference2);
        this.scriptContext.setAttribute("changeRequestReferences", crReferences, ScriptContext.ENGINE_SCOPE);
        this.scriptContext.setAttribute("event", compositeEvent, ScriptContext.ENGINE_SCOPE);

        String result = this.templateManager.render(String.format(TEMPLATE_PATH, "review.added"));
        String expectedResult = "<table width=\"100%\">\n"
            + "    <tr>\n"
            + "                        <td width=\"25%\" style=\"width: 25%; vertical-align: top;\" valign=\"top\">\n"
            + "            <strong>Change Request</strong>\n"
            + "\n"
            + "      </td>\n"
            + "                        <td style=\"vertical-align: top;\" valign=\"top\">\n"
            + "                              <a color=\"#0088CC\" style=\"color: #0088CC; text-decoration: none;\" "
            + "href=\"/xwiki/bin/view/ChangeRequest/CR2\">CR 2</a>\n"
            + "                      <div>\n"
            + "      Change request review added by 2\n"
            + "    </div>\n"
            + "    <div>\n"
            + "                            <img src=\"cid:User2.jpg\" alt=\"U\" width=\"16\" height=\"16\" "
            + "style=\"vertical-align: middle;\"/>\n"
            + "                           <img src=\"cid:User.jpg\" alt=\"U\" width=\"16\" height=\"16\" "
            + "style=\"vertical-align: middle;\"/>\n"
            + "             </div>\n"
            + "        <div>\n"
            + "    <small style=\"color: #777777; font-size: 0.8em;\">\n"
            + "      A few hours ago\n"
            + "    </small>\n"
            + "  </div>\n"
            + "                                <table width=\"100%\" style=\"margin: 5px 0; font-size: 0.8em; "
            + "border-top: 1px dashed #e8e8e8\">\n"
            + "                                                        <tr style=\"vertical-align: top;\">\n"
            + "                <td width=\"33%\" style=\"overflow: hidden;\">        <img src=\"cid:User2.jpg\" "
            + "alt=\"U\" width=\"16\" height=\"16\" style=\"vertical-align: middle;\"/>\n"
            + "     <a color=\"#0088CC\" style=\"color: #0088CC; text-decoration: none;\" "
            + "href=\"/xwiki/bin/view/XWiki/User2\">User2</a>\n"
            + "</td>\n"
            + "                <td width=\"45%\">added a review</td>\n"
            + "                <td>\n"
            + "                                              <a color=\"#0088CC\" style=\"color: #0088CC; "
            + "text-decoration: none;\" href=\"/xwiki/bin/view/ChangeRequest/CR2?viewer=changes&#38;rev2=1.1\">"
            + "1970/01/01 02:37</a>\n"
            + "                                    </td>\n"
            + "            </tr>\n"
            + "                                                        <tr style=\"vertical-align: top;\">\n"
            + "                <td width=\"33%\" style=\"overflow: hidden;\">        <img src=\"cid:User.jpg\" "
            + "alt=\"U\" width=\"16\" height=\"16\" style=\"vertical-align: middle;\"/>\n"
            + "     <a color=\"#0088CC\" style=\"color: #0088CC; text-decoration: none;\" "
            + "href=\"/xwiki/bin/view/XWiki/User\">First & Name</a>\n"
            + "</td>\n"
            + "                <td width=\"45%\">added a review</td>\n"
            + "                <td>\n"
            + "                                              <a color=\"#0088CC\" style=\"color: #0088CC; "
            + "text-decoration: none;\" href=\"/xwiki/bin/view/ChangeRequest/CR1?viewer=changes&#38;rev2=1.1\">"
            + "1970/01/01 01:20</a>\n"
            + "                                    </td>\n"
            + "            </tr>\n"
            + "            </table>\n"
            + "        \n"
            + "      </td>\n"
            + "    </tr>\n"
            + "  </table>";
        assertEquals(expectedResult, result.trim());

        // Before 1.13 events were sent with the modified doc as data, not the CR doc
        testEvent1.setDocument(modifiedDocReference);
        assertEquals(expectedResult, result.trim());
    }

    @Test
    void staleNotificationTemplate() throws Exception
    {
        RecordableEventDescriptor recordableEventDescriptor = mock(RecordableEventDescriptor.class);
        when(this.eventStreamScriptService
            .getDescriptorForEventType(StaleChangeRequestRecordableEvent.EVENT_NAME, true))
            .thenReturn(recordableEventDescriptor);
        when(recordableEventDescriptor.getApplicationName()).thenReturn(NOTIF_APPLI_NAME);
        when(recordableEventDescriptor.getApplicationIcon()).thenReturn(DESCRIPTOR_ICON);
        when(recordableEventDescriptor.getEventTypeIcon()).thenReturn(DESCRIPTOR_ICON);

        String changeRequestId = "changeRequestId";

        String changeRequestId2 = "changeRequestId2";

        DocumentReference crDocReference = new DocumentReference("xwiki", "ChangeRequest", "CR1");
        XWikiDocument crDoc = new XWikiDocument(crDocReference);
        crDoc.setTitle("CR 1");
        crDoc.setSyntax(Syntax.XWIKI_2_1);
        this.oldcore.getSpyXWiki().saveDocument(crDoc, this.context);

        DocumentReference crDocReference2 = new DocumentReference("xwiki", "ChangeRequest", "CR2");
        XWikiDocument crDoc2 = new XWikiDocument(crDocReference2);
        crDoc2.setTitle("CR 2");
        crDoc2.setSyntax(Syntax.XWIKI_2_1);
        this.oldcore.getSpyXWiki().saveDocument(crDoc2, this.context);

        DocumentReference modifiedDocReference = new DocumentReference("xwiki", "Space", "ModifiedDoc");
        XWikiDocument modifiedDoc = new XWikiDocument(modifiedDocReference);
        modifiedDoc.setTitle("Modified document");
        modifiedDoc.setSyntax(Syntax.XWIKI_2_1);
        this.oldcore.getSpyXWiki().saveDocument(modifiedDoc, this.context);

        ChangeRequest changeRequest = mock(ChangeRequest.class);
        when(this.changeRequestScriptService.getChangeRequest(changeRequestId)).thenReturn(Optional.of(changeRequest));

        ChangeRequest changeRequest2 = mock(ChangeRequest.class);
        when(this.changeRequestScriptService.getChangeRequest(changeRequestId2))
            .thenReturn(Optional.of(changeRequest2));

        Event testEvent1 = new DefaultEvent();
        testEvent1.setApplication(NOTIF_APPLI_NAME);
        testEvent1.setType(StaleChangeRequestRecordableEvent.EVENT_NAME);
        testEvent1.setCustom(Map.of(
            CR_ID_PARAM_KEY, changeRequestId
        ));
        Date testDate = new Date(1212121);
        when(this.dateScriptService.displayTimeAgo(testDate)).thenReturn("A few minutes ago");
        testEvent1.setDate(testDate);
        testEvent1.setUser(USER_REFERENCE);
        testEvent1.setDocument(crDocReference);
        testEvent1.setDocumentVersion(crDoc.getVersion());

        Event testEvent2 = new DefaultEvent();
        testEvent2.setApplication(NOTIF_APPLI_NAME);
        testEvent2.setType(StaleChangeRequestRecordableEvent.EVENT_NAME);
        testEvent2.setCustom(Map.of(
            CR_ID_PARAM_KEY, changeRequestId2
        ));
        Date testDate2 = new Date(5858558);
        when(this.dateScriptService.displayTimeAgo(testDate2)).thenReturn("A few hours ago");
        testEvent2.setDate(testDate2);
        testEvent2.setUser(USER_REFERENCE_2);
        testEvent2.setDocument(crDocReference2);
        testEvent2.setDocumentVersion(crDoc2.getVersion());

        this.context.setOriginalWikiId("xwiki");
        when(this.localizationScriptService.render(
            eq("changerequest.notifications.stale.description")))
            .then(invocationOnMock -> "CR is stale");

        when(this.localizationScriptService.render(
            eq("notifications.events.changerequest.stale.description.by.users"),
            any(Collection.class)))
            .then(invocationOnMock -> {
                List<String> parameters = invocationOnMock.getArgument(1);
                assertEquals(1, parameters.size());

                return String.format("Change request has been marked stale by %s", parameters.get(0));
            });

        CompositeEvent compositeEvent = new CompositeEvent(testEvent1);
        compositeEvent.add(testEvent2, 0);
        Map<Event, DocumentReference> crReferences = Map.of(testEvent1, crDocReference, testEvent2, crDocReference2);
        this.scriptContext.setAttribute("changeRequestReferences", crReferences, ScriptContext.ENGINE_SCOPE);
        this.scriptContext.setAttribute("event", compositeEvent, ScriptContext.ENGINE_SCOPE);

        String result = this.templateManager.render(String.format(TEMPLATE_PATH, "stale"));
        String expectedResult = "<table width=\"100%\">\n"
            + "    <tr>\n"
            + "                        <td width=\"25%\" style=\"width: 25%; vertical-align: top;\" valign=\"top\">\n"
            + "            <strong>Change Request</strong>\n"
            + "\n"
            + "      </td>\n"
            + "                        <td style=\"vertical-align: top;\" valign=\"top\">\n"
            + "                              <a color=\"#0088CC\" style=\"color: #0088CC; text-decoration: none;\" "
            + "href=\"/xwiki/bin/view/ChangeRequest/CR2\">CR 2</a>\n"
            + "                      <div>\n"
            + "      Change request has been marked stale by 2\n"
            + "    </div>\n"
            + "    <div>\n"
            + "                            <img src=\"cid:User2.jpg\" alt=\"U\" width=\"16\" height=\"16\""
            + " style=\"vertical-align: middle;\"/>\n"
            + "                           <img src=\"cid:User.jpg\" alt=\"U\" width=\"16\" height=\"16\" "
            + "style=\"vertical-align: middle;\"/>\n"
            + "             </div>\n"
            + "        <div>\n"
            + "    <small style=\"color: #777777; font-size: 0.8em;\">\n"
            + "      A few hours ago\n"
            + "    </small>\n"
            + "  </div>\n"
            + "                                <table width=\"100%\" style=\"margin: 5px 0; font-size: 0.8em; "
            + "border-top: 1px dashed #e8e8e8\">\n"
            + "                                                        <tr style=\"vertical-align: top;\">\n"
            + "                <td width=\"33%\" style=\"overflow: hidden;\">        <img src=\"cid:User2.jpg\" "
            + "alt=\"U\" width=\"16\" height=\"16\" style=\"vertical-align: middle;\"/>\n"
            + "     <a color=\"#0088CC\" style=\"color: #0088CC; text-decoration: none;\" "
            + "href=\"/xwiki/bin/view/XWiki/User2\">User2</a>\n"
            + "</td>\n"
            + "                <td width=\"45%\">CR is stale</td>\n"
            + "                <td>\n"
            + "                                              <a color=\"#0088CC\" style=\"color: #0088CC; "
            + "text-decoration: none;\" href=\"/xwiki/bin/view/ChangeRequest/CR2?viewer=changes&#38;rev2=1.1\">"
            + "1970/01/01 02:37</a>\n"
            + "                                    </td>\n"
            + "            </tr>\n"
            + "                                                        <tr style=\"vertical-align: top;\">\n"
            + "                <td width=\"33%\" style=\"overflow: hidden;\">        <img src=\"cid:User.jpg\" "
            + "alt=\"U\" width=\"16\" height=\"16\" style=\"vertical-align: middle;\"/>\n"
            + "     <a color=\"#0088CC\" style=\"color: #0088CC; text-decoration: none;\" "
            + "href=\"/xwiki/bin/view/XWiki/User\">First & Name</a>\n"
            + "</td>\n"
            + "                <td width=\"45%\">CR is stale</td>\n"
            + "                <td>\n"
            + "                                              <a color=\"#0088CC\" style=\"color: #0088CC; "
            + "text-decoration: none;\" href=\"/xwiki/bin/view/ChangeRequest/CR1?viewer=changes&#38;rev2=1.1\">"
            + "1970/01/01 01:20</a>\n"
            + "                                    </td>\n"
            + "            </tr>\n"
            + "            </table>\n"
            + "        \n"
            + "      </td>\n"
            + "    </tr>\n"
            + "  </table>";
        assertEquals(expectedResult, result.trim());

        // Before 1.13 events were sent with the modified doc as data, not the CR doc
        testEvent1.setDocument(modifiedDocReference);
        assertEquals(expectedResult, result.trim());
    }

    @Test
    void statusModifiedNotificationTemplate() throws Exception
    {
        RecordableEventDescriptor recordableEventDescriptor = mock(RecordableEventDescriptor.class);
        when(this.eventStreamScriptService
            .getDescriptorForEventType(ChangeRequestStatusChangedRecordableEvent.EVENT_NAME, true))
            .thenReturn(recordableEventDescriptor);
        when(recordableEventDescriptor.getApplicationName()).thenReturn(NOTIF_APPLI_NAME);
        when(recordableEventDescriptor.getApplicationIcon()).thenReturn(DESCRIPTOR_ICON);
        when(recordableEventDescriptor.getEventTypeIcon()).thenReturn(DESCRIPTOR_ICON);

        String changeRequestId = "changeRequestId";

        String changeRequestId2 = "changeRequestId2";

        DocumentReference crDocReference = new DocumentReference("xwiki", "ChangeRequest", "CR1");
        XWikiDocument crDoc = new XWikiDocument(crDocReference);
        crDoc.setTitle("CR 1");
        crDoc.setSyntax(Syntax.XWIKI_2_1);
        this.oldcore.getSpyXWiki().saveDocument(crDoc, this.context);

        DocumentReference crDocReference2 = new DocumentReference("xwiki", "ChangeRequest", "CR2");
        XWikiDocument crDoc2 = new XWikiDocument(crDocReference2);
        crDoc2.setTitle("CR 2");
        crDoc2.setSyntax(Syntax.XWIKI_2_1);
        this.oldcore.getSpyXWiki().saveDocument(crDoc2, this.context);

        DocumentReference modifiedDocReference = new DocumentReference("xwiki", "Space", "ModifiedDoc");
        XWikiDocument modifiedDoc = new XWikiDocument(modifiedDocReference);
        modifiedDoc.setTitle("Modified document");
        modifiedDoc.setSyntax(Syntax.XWIKI_2_1);
        this.oldcore.getSpyXWiki().saveDocument(modifiedDoc, this.context);

        ChangeRequest changeRequest = mock(ChangeRequest.class);
        when(this.changeRequestScriptService.getChangeRequest(changeRequestId)).thenReturn(Optional.of(changeRequest));

        ChangeRequest changeRequest2 = mock(ChangeRequest.class);
        when(this.changeRequestScriptService.getChangeRequest(changeRequestId2))
            .thenReturn(Optional.of(changeRequest2));

        Event testEvent1 = new DefaultEvent();
        testEvent1.setApplication(NOTIF_APPLI_NAME);
        testEvent1.setType(ChangeRequestStatusChangedRecordableEvent.EVENT_NAME);
        testEvent1.setCustom(Map.of(
            CR_ID_PARAM_KEY, changeRequestId,
            CR_STATUS_OLD_PARAM_KEY, ChangeRequestStatus.DRAFT,
            CR_STATUS_NEW_PARAM_KEY, ChangeRequestStatus.READY_FOR_MERGING
        ));
        Date testDate = new Date(1212121);
        when(this.dateScriptService.displayTimeAgo(testDate)).thenReturn("A few minutes ago");
        testEvent1.setDate(testDate);
        testEvent1.setUser(USER_REFERENCE);
        testEvent1.setDocument(crDocReference);
        testEvent1.setDocumentVersion(crDoc.getVersion());

        Event testEvent2 = new DefaultEvent();
        testEvent2.setApplication(NOTIF_APPLI_NAME);
        testEvent2.setType(ChangeRequestStatusChangedRecordableEvent.EVENT_NAME);
        testEvent2.setCustom(Map.of(
            CR_ID_PARAM_KEY, changeRequestId2,
            CR_STATUS_OLD_PARAM_KEY, ChangeRequestStatus.READY_FOR_REVIEW,
            CR_STATUS_NEW_PARAM_KEY, ChangeRequestStatus.CLOSED
        ));
        Date testDate2 = new Date(5858558);
        when(this.dateScriptService.displayTimeAgo(testDate2)).thenReturn("A few hours ago");
        testEvent2.setDate(testDate2);
        testEvent2.setUser(USER_REFERENCE_2);
        testEvent2.setDocument(crDocReference2);
        testEvent2.setDocumentVersion(crDoc2.getVersion());

        this.context.setOriginalWikiId("xwiki");
        when(this.localizationScriptService.render(
            eq("notifications.events.changerequest.status.modified.description.by.users"),
            any(Collection.class)))
            .then(invocationOnMock -> {
                List<String> parameters = invocationOnMock.getArgument(1);
                assertEquals(1, parameters.size());

                return String.format("Change request status modified by %s", parameters.get(0));
            });

        when(this.localizationScriptService.render(
            eq("changerequest.notifications.status.modified.description"),
            any(Collection.class)))
            .then(invocationOnMock -> {
                List<String> parameters = invocationOnMock.getArgument(1);
                assertEquals(2, parameters.size());

                return String.format("Change request status modified from %s to %s", parameters.get(0),
                    parameters.get(1));
            });
        when(this.localizationScriptService.render(startsWith("ChangeRequest.Code.ChangeRequestClass_status_")))
            .then(invocationOnMock -> String.format("Status %s", invocationOnMock.getArgument(0).toString()));

        CompositeEvent compositeEvent = new CompositeEvent(testEvent1);
        compositeEvent.add(testEvent2, 0);
        Map<Event, DocumentReference> crReferences = Map.of(testEvent1, crDocReference, testEvent2, crDocReference2);
        this.scriptContext.setAttribute("changeRequestReferences", crReferences, ScriptContext.ENGINE_SCOPE);
        this.scriptContext.setAttribute("event", compositeEvent, ScriptContext.ENGINE_SCOPE);

        String result = this.templateManager.render(String.format(TEMPLATE_PATH, "status.modified"));
        String expectedResult = "<table width=\"100%\">\n"
            + "    <tr>\n"
            + "                        <td width=\"25%\" style=\"width: 25%; vertical-align: top;\" valign=\"top\">\n"
            + "            <strong>Change Request</strong>\n"
            + "\n"
            + "      </td>\n"
            + "                        <td style=\"vertical-align: top;\" valign=\"top\">\n"
            + "                              <a color=\"#0088CC\" style=\"color: #0088CC; text-decoration: none;\" "
            + "href=\"/xwiki/bin/view/ChangeRequest/CR2\">CR 2</a>\n"
            + "                      <div>\n"
            + "      Change request status modified by 2\n"
            + "    </div>\n"
            + "    <div>\n"
            + "                            <img src=\"cid:User2.jpg\" alt=\"U\" width=\"16\" height=\"16\" "
            + "style=\"vertical-align: middle;\"/>\n"
            + "                           <img src=\"cid:User.jpg\" alt=\"U\" width=\"16\" height=\"16\" "
            + "style=\"vertical-align: middle;\"/>\n"
            + "             </div>\n"
            + "        <div>\n"
            + "    <small style=\"color: #777777; font-size: 0.8em;\">\n"
            + "      A few hours ago\n"
            + "    </small>\n"
            + "  </div>\n"
            + "                                <table width=\"100%\" style=\"margin: 5px 0; font-size: 0.8em; "
            + "border-top: 1px dashed #e8e8e8\">\n"
            + "                                            <tr style=\"vertical-align: top;\">\n"
            + "                <td width=\"33%\" style=\"overflow: hidden;\">        <img src=\"cid:User2.jpg\" "
            + "alt=\"U\" width=\"16\" height=\"16\" style=\"vertical-align: middle;\"/>\n"
            + "     <a color=\"#0088CC\" style=\"color: #0088CC; text-decoration: none;\" "
            + "href=\"/xwiki/bin/view/XWiki/User2\">User2</a>\n"
            + "</td>\n"
            + "                <td width=\"45%\">Change request status modified from "
            + "status changerequest.code.changerequestclass_status_ready_for_review\n"
            + " to status changerequest.code.changerequestclass_status_closed\n"
            + "</td>\n"
            + "                <td>\n"
            + "                                              <a color=\"#0088CC\" style=\"color: #0088CC; "
            + "text-decoration: none;\" href=\"/xwiki/bin/view/ChangeRequest/CR2\">1970/01/01 02:37</a>\n"
            + "                                    </td>\n"
            + "            </tr>\n"
            + "                                            <tr style=\"vertical-align: top;\">\n"
            + "                <td width=\"33%\" style=\"overflow: hidden;\">        <img src=\"cid:User.jpg\" "
            + "alt=\"U\" width=\"16\" height=\"16\" style=\"vertical-align: middle;\"/>\n"
            + "     <a color=\"#0088CC\" style=\"color: #0088CC; text-decoration: none;\" "
            + "href=\"/xwiki/bin/view/XWiki/User\">First & Name</a>\n"
            + "</td>\n"
            + "                <td width=\"45%\">Change request status modified from "
            + "status changerequest.code.changerequestclass_status_draft\n"
            + " to status changerequest.code.changerequestclass_status_ready_for_merging\n"
            + "</td>\n"
            + "                <td>\n"
            + "                                              "
            + "<a color=\"#0088CC\" style=\"color: #0088CC; text-decoration: none;\" "
            + "href=\"/xwiki/bin/view/ChangeRequest/CR1\">1970/01/01 01:20</a>\n"
            + "                                    </td>\n"
            + "            </tr>\n"
            + "            </table>\n"
            + "        \n"
            + "      </td>\n"
            + "    </tr>\n"
            + "  </table>";
        assertEquals(expectedResult, result.trim());

        // Before 1.13 events were sent with the modified doc as data, not the CR doc
        testEvent1.setDocument(modifiedDocReference);
        assertEquals(expectedResult, result.trim());
    }
}
