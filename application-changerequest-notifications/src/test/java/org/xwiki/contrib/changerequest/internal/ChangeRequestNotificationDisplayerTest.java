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
package org.xwiki.contrib.changerequest.internal;

import java.util.List;
import java.util.Map;

import javax.script.ScriptContext;

import org.junit.jupiter.api.Test;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestCreatedRecordableEvent;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestFileChangeAddedRecordableEvent;
import org.xwiki.eventstream.Event;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ChangeRequestNotificationDisplayer}.
 *
 * @version $Id$
 */
@ComponentTest
class ChangeRequestNotificationDisplayerTest
{
    @InjectMockComponents
    private ChangeRequestNotificationDisplayer displayer;

    @MockComponent
    private DocumentReferenceResolver<ChangeRequest> changeRequestDocumentReferenceResolver;

    @MockComponent
    private TemplateManager templateManager;

    @MockComponent
    private ScriptContextManager scriptContextManager;

    @MockComponent
    private ChangeRequestGroupingStrategy groupingStrategy;

    @Test
    void renderNotification() throws Exception
    {
        CompositeEvent compositeEvent = mock(CompositeEvent.class);
        when(compositeEvent.getType()).thenReturn(ChangeRequestFileChangeAddedRecordableEvent.EVENT_NAME);
        Event event1 = mock(Event.class, "event1");
        Event event2 = mock(Event.class, "event2");
        Event event3 = mock(Event.class, "event3");

        when(compositeEvent.getEvents()).thenReturn(List.of(event1, event2, event3));
        when(this.groupingStrategy.groupEvents(compositeEvent)).thenReturn(List.of(compositeEvent));

        String cr1 = "cr1";
        String cr2 = "cr2";
        when(event1.getCustom()).thenReturn(Map.of("changerequest.id", cr1));
        when(event2.getCustom()).thenReturn(Map.of("changerequest.id", cr2));
        when(event3.getCustom()).thenReturn(Map.of("changerequest.id", cr2));

        DocumentReference crRef1 = mock(DocumentReference.class, "crRef1");
        DocumentReference crRef2 = mock(DocumentReference.class, "crRef2");
        when(this.changeRequestDocumentReferenceResolver.resolve(any())).thenAnswer(invocation -> {
            ChangeRequest changeRequest = invocation.getArgument(0);
            if (changeRequest.getId().equals(cr1)) {
                return crRef1;
            } else if (changeRequest.getId().equals(cr2)) {
                return crRef2;
            } else {
                fail("Wrong change request id: " + changeRequest.getId());
                return null;
            }
        });

        Map<Event, DocumentReference> expectedMap = Map.of(
            event1, crRef1,
            event2, crRef2,
            event3, crRef2
        );

        Template template = mock(Template.class);
        when(this.templateManager.getTemplate("changerequest/alert/filechange.added.vm")).thenReturn(template);

        XDOM xdom = mock(XDOM.class);
        Block block = mock(Block.class);
        when(xdom.getChildren()).thenReturn(List.of(block));
        when(this.templateManager.execute(template)).thenReturn(xdom);

        ScriptContext scriptContext = mock(ScriptContext.class);
        when(this.scriptContextManager.getScriptContext()).thenReturn(scriptContext);

        GroupBlock groupBlock = new GroupBlock();
        groupBlock.addChildren(List.of(block));
        assertEquals(groupBlock, this.displayer.renderNotification(compositeEvent));
        verify(scriptContext).setAttribute("compositeEvent", compositeEvent, ScriptContext.ENGINE_SCOPE);
        verify(scriptContext).setAttribute("changeRequestReferences", expectedMap, ScriptContext.ENGINE_SCOPE);
        verify(scriptContext).removeAttribute("compositeEvent", ScriptContext.ENGINE_SCOPE);
        verify(scriptContext).removeAttribute("changeRequestReferences", ScriptContext.ENGINE_SCOPE);
    }

    @Test
    void renderChangeRequestCreatedNotification() throws Exception
    {
        CompositeEvent compositeEvent = mock(CompositeEvent.class);
        when(compositeEvent.getType()).thenReturn(ChangeRequestCreatedRecordableEvent.EVENT_NAME);
        Event event1 = mock(Event.class, "event1");
        Event event2 = mock(Event.class, "event2");
        Event event3 = mock(Event.class, "event3");
        when(event1.getType()).thenReturn(ChangeRequestCreatedRecordableEvent.EVENT_NAME);
        when(event2.getType()).thenReturn(ChangeRequestCreatedRecordableEvent.EVENT_NAME);
        when(event3.getType()).thenReturn(ChangeRequestCreatedRecordableEvent.EVENT_NAME);

        when(compositeEvent.getEvents()).thenReturn(List.of(event1, event2, event3));

        String cr1 = "cr1";
        String cr2 = "cr2";
        when(event1.getCustom()).thenReturn(Map.of("changerequest.id", cr1));
        when(event2.getCustom()).thenReturn(Map.of("changerequest.id", cr2));
        when(event3.getCustom()).thenReturn(Map.of("changerequest.id", cr2));

        DocumentReference crRef1 = mock(DocumentReference.class, "crRef1");
        DocumentReference crRef2 = mock(DocumentReference.class, "crRef2");
        when(this.changeRequestDocumentReferenceResolver.resolve(any())).thenAnswer(invocation -> {
            ChangeRequest changeRequest = invocation.getArgument(0);
            if (changeRequest.getId().equals(cr1)) {
                return crRef1;
            } else if (changeRequest.getId().equals(cr2)) {
                return crRef2;
            } else {
                fail("Wrong change request id: " + changeRequest.getId());
                return null;
            }
        });

        Template template = mock(Template.class);
        when(this.templateManager.getTemplate("changerequest/alert/create.vm")).thenReturn(template);

        XDOM xdom = mock(XDOM.class);
        Block block = mock(Block.class);
        when(xdom.getChildren()).thenReturn(List.of(block));
        when(this.templateManager.execute(template)).thenReturn(xdom);

        ScriptContext scriptContext = mock(ScriptContext.class);
        when(this.scriptContextManager.getScriptContext()).thenReturn(scriptContext);

        CompositeEvent compositeEvent1 = new CompositeEvent(event1);
        CompositeEvent compositeEvent2 = new CompositeEvent(event2);
        CompositeEvent compositeEvent3 = new CompositeEvent(event3);

        when(this.groupingStrategy.groupEvents(compositeEvent))
            .thenReturn(List.of(compositeEvent1, compositeEvent2, compositeEvent3));

        GroupBlock groupBlock = new GroupBlock();
        groupBlock.addChildren(List.of(block, block, block));
        assertEquals(groupBlock, this.displayer.renderNotification(compositeEvent));
        verify(scriptContext).setAttribute("compositeEvent", compositeEvent1, ScriptContext.ENGINE_SCOPE);
        verify(scriptContext).setAttribute("changeRequestReferences", Map.of(event1, crRef1),
            ScriptContext.ENGINE_SCOPE);
        verify(scriptContext).setAttribute("compositeEvent", compositeEvent2, ScriptContext.ENGINE_SCOPE);
        verify(scriptContext).setAttribute("changeRequestReferences", Map.of(event2, crRef2),
            ScriptContext.ENGINE_SCOPE);
        verify(scriptContext).setAttribute("compositeEvent", compositeEvent3, ScriptContext.ENGINE_SCOPE);
        verify(scriptContext).setAttribute("changeRequestReferences", Map.of(event3, crRef2),
            ScriptContext.ENGINE_SCOPE);

        verify(scriptContext, times(3)).removeAttribute("compositeEvent", ScriptContext.ENGINE_SCOPE);
        verify(scriptContext, times(3)).removeAttribute("changeRequestReferences", ScriptContext.ENGINE_SCOPE);
    }
}