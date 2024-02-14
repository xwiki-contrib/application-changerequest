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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.script.ScriptContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.internal.converters.AbstractChangeRequestRecordableEventConverter;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestCreatedRecordableEvent;
import org.xwiki.contrib.changerequest.script.ChangeRequestNotificationsScriptService;
import org.xwiki.contrib.changerequest.script.ChangeRequestScriptService;
import org.xwiki.diff.display.UnifiedDiffBlock;
import org.xwiki.diff.display.UnifiedDiffElement;
import org.xwiki.diff.script.DiffDisplayerScriptService;
import org.xwiki.diff.script.DiffScriptService;
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
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.script.WikiManagerScriptService;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
class NotificationAlertPageTest extends PageTest
{
    private static final DocumentReference USER_REFERENCE = new DocumentReference("xwiki", "XWiki", "User");

    private static final String TEMPLATE_PATH = "changerequest/alert/%s.vm";
    private static final String NOTIF_APPLI_NAME = "Change Request";
    private static final String CR_ID_PARAM_KEY = "changerequest.id";
    private static final String FILECHANGE_ID_PARAM_KEY = "changerequest.filechange.id";
    private static final String DESCRIPTOR_ICON = "branch";

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

        RecordableEventDescriptor recordableEventDescriptor = mock(RecordableEventDescriptor.class);
        when(this.eventStreamScriptService
            .getDescriptorForEventType(ChangeRequestCreatedRecordableEvent.EVENT_NAME, true))
            .thenReturn(recordableEventDescriptor);
        when(recordableEventDescriptor.getApplicationName()).thenReturn(NOTIF_APPLI_NAME);
        when(recordableEventDescriptor.getApplicationIcon()).thenReturn(DESCRIPTOR_ICON);
        when(this.iconManagerScriptService.renderHTML(DESCRIPTOR_ICON)).thenReturn("Icon Branch");
        when(this.localizationScriptService.render(NOTIF_APPLI_NAME)).thenReturn("CR APPLI");

        when(this.oldcore.getMockRightService().hasProgrammingRights(this.context)).thenReturn(true);
    }

    @Test
    void createNotificationTemplate() throws Exception
    {
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
        // Mock the user's name.
        when(this.oldcore.getSpyXWiki().getPlainUserName(USER_REFERENCE, this.context)).thenReturn("First & Name");
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

        Map<Event, DocumentReference> crReferences = Map.of(testEvent, crDocReference);
        this.scriptContext.setAttribute("changeRequestReferences", crReferences, ScriptContext.ENGINE_SCOPE);
        this.scriptContext.setAttribute("compositeEvent", new CompositeEvent(testEvent), ScriptContext.ENGINE_SCOPE);

        String result = this.templateManager.render(String.format(TEMPLATE_PATH, "create"));
        String expectedResult = "<div class=\"clearfix row\">\n"
            + "    <div class=\"col-xs-3 notification-icon\">\n"
            + "      <div class=\"img-thumbnail\">\n"
            + "        Icon Branch\n"
            + "      </div>\n"
            + "          </div>\n"
            + "    <div class=\"col-xs-9 notification-content\">\n"
            + "      <div class=\"notification-page\">\n"
            + "                <a href=\"/xwiki/bin/view/ChangeRequest/CR1\">CR 1</a>\n"
            + "                    </div>\n"
            + "<div class=\"notification-description\">\n"
            + "    Change request created by             "
            + "<span class=\"notification-event-user\" data-xwiki-lightbox=\"false\">\n"
            + "    <img src=\"/xwiki/bin/skin/skins/flamingo/icons/xwiki/noavatar.png\" alt=\"First &#38; Name\"/>"
            + "User  </span> for                         \n"
            + "                                                                                                        "
            + "<div class=\"changerequest-impacted-page\">\n"
            + "            \n"
            + "                    <a href=\"/xwiki/bin/view/Space/ModifiedDoc\">Modified doc title</a>\n"
            + "    </div>\n"
            + "\n"
            + "  <div><small class=\"text-muted\">A few minutes ago</small></div>\n"
            + "</div>\n"
            + "\n"
            + "          </div>\n"
            + "      </div>";
        assertEquals(expectedResult, result.trim());
    }
}
