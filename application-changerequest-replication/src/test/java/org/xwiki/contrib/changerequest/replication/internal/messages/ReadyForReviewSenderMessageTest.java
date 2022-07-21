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
package org.xwiki.contrib.changerequest.replication.internal.messages;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestReadyForReviewTargetableEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.properties.ConverterManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ReadyForReviewSenderMessage}
 *
 * @version $Id$
 */
@ComponentTest
class ReadyForReviewSenderMessageTest
{
    private static final String CONTEXT_USER = "XWiki.ContextUser";

    @InjectMockComponents
    private ReadyForReviewSenderMessage senderMessage;

    @MockComponent
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private ConverterManager converter;

    private XWikiContext context;

    @BeforeEach
    void setup()
    {
        this.context = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(this.context);
        DocumentReference documentReference = mock(DocumentReference.class, "contextUserRef");
        when(this.entityReferenceSerializer.serialize(documentReference)).thenReturn(CONTEXT_USER);
        when(this.context.getUserReference()).thenReturn(documentReference);

        when(this.converter.convert(eq(String.class), any())).then(invocation -> {
            Object argument = invocation.getArgument(1);
            return argument.toString();
        });
    }

    @Test
    void initialize()
    {
        ChangeRequestReadyForReviewTargetableEvent event =
            new ChangeRequestReadyForReviewTargetableEvent(new HashSet<>(List.of("user1", "user2", "user3")));

        DocumentReference dataDocReference = mock(DocumentReference.class, "dataDocReference");
        String serializedDataDoc = "DataDocReference";
        when(this.entityReferenceSerializer.serialize(dataDocReference)).thenReturn(serializedDataDoc);
        
        this.senderMessage.initialize(event, dataDocReference);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("CONTEXT_USER", Collections.singleton(CONTEXT_USER));
        parameters.put("DATA_DOCUMENT", Collections.singleton(serializedDataDoc));
        parameters.put("TARGETS", Collections.singleton("user1,user2,user3"));

        assertEquals(parameters, this.senderMessage.getCustomMetadata());
    }

    @Test
    void getType()
    {
        assertEquals(ChangeRequestReadyForReviewTargetableEvent.EVENT_NAME, senderMessage.getType());
    }
}