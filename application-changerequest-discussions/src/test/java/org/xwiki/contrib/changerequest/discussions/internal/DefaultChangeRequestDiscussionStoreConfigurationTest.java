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
package org.xwiki.contrib.changerequest.discussions.internal;

import java.util.Optional;

import javax.inject.Provider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.storage.ChangeRequestStorageManager;
import org.xwiki.contrib.discussions.DiscussionStoreConfigurationParameters;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DefaultChangeRequestDiscussionStoreConfiguration}.
 *
 * @version $Id$
 */
@ComponentTest
class DefaultChangeRequestDiscussionStoreConfigurationTest
{
    private static final String PARAM_KEY =
        DefaultChangeRequestDiscussionStoreConfiguration.CHANGE_REQUEST_ID_PARAMETER_KEY;

    @InjectMockComponents
    private DefaultChangeRequestDiscussionStoreConfiguration configuration;

    @MockComponent
    private DocumentReferenceResolver<ChangeRequest> changeRequestDocumentReferenceResolver;

    @MockComponent
    private ChangeRequestStorageManager changeRequestStorageManager;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @Test
    void getDiscussionContextSpaceStorageLocationWhenStringParameter() throws ChangeRequestException
    {
        DiscussionStoreConfigurationParameters parameters = new DiscussionStoreConfigurationParameters();
        parameters.put(PARAM_KEY, "CRID");

        ChangeRequest changeRequest = mock(ChangeRequest.class);
        when(this.changeRequestStorageManager.load("CRID")).thenReturn(Optional.of(changeRequest));
        DocumentReference documentReference = new DocumentReference("wiki", "ChangeRequestSpace", "WebHome");
        when(this.changeRequestDocumentReferenceResolver.resolve(changeRequest)).thenReturn(documentReference);

        SpaceReference result = this.configuration.getDiscussionContextSpaceStorageLocation(parameters, null);

        assertEquals(new SpaceReference("DiscussionContext",
            new SpaceReference("Discussions", documentReference.getLastSpaceReference())), result);
    }

    @Test
    void getDiscussionSpaceStorageLocationWhenArrayParameter() throws ChangeRequestException
    {
        DiscussionStoreConfigurationParameters parameters = new DiscussionStoreConfigurationParameters();
        parameters.put(PARAM_KEY, new String[] { "CRID" });

        ChangeRequest changeRequest = mock(ChangeRequest.class);
        when(this.changeRequestStorageManager.load("CRID")).thenReturn(Optional.of(changeRequest));
        DocumentReference documentReference = new DocumentReference("wiki", "ChangeRequestSpace", "WebHome");
        when(this.changeRequestDocumentReferenceResolver.resolve(changeRequest)).thenReturn(documentReference);

        SpaceReference result = this.configuration.getDiscussionSpaceStorageLocation(parameters);

        assertEquals(new SpaceReference("Discussion",
            new SpaceReference("Discussions", documentReference.getLastSpaceReference())), result);
    }

    @Test
    void getMessageSpaceStorageLocationWhenMissingParameterFallsBackToContextWiki()
    {
        DiscussionStoreConfigurationParameters parameters = new DiscussionStoreConfigurationParameters();

        XWikiContext context = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(context);
        WikiReference wikiReference = new WikiReference("wiki");
        when(context.getWikiReference()).thenReturn(wikiReference);

        SpaceReference result = this.configuration.getMessageSpaceStorageLocation(parameters, null);

        assertEquals(new SpaceReference("Message",
            new SpaceReference("Discussions", new SpaceReference("ChangeRequest", wikiReference))), result);
        assertEquals(1, this.logCapture.size());
        assertEquals("Missing or wrong parameter for change request id: [null]", this.logCapture.getMessage(0));
    }

    @Test
    void getDiscussionContextSpaceStorageLocationWhenLoadFailsFallsBackToContextWiki() throws ChangeRequestException
    {
        DiscussionStoreConfigurationParameters parameters = new DiscussionStoreConfigurationParameters();
        parameters.put(PARAM_KEY, "CRID");

        when(this.changeRequestStorageManager.load("CRID")).thenThrow(new ChangeRequestException("error"));

        XWikiContext context = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(context);
        WikiReference wikiReference = new WikiReference("wiki");
        when(context.getWikiReference()).thenReturn(wikiReference);

        SpaceReference result = this.configuration.getDiscussionContextSpaceStorageLocation(parameters, null);

        assertEquals(new SpaceReference("DiscussionContext",
            new SpaceReference("Discussions", new SpaceReference("ChangeRequest", wikiReference))), result);
        assertEquals(1, this.logCapture.size());
        assertEquals("Error while getting value for change request id from [CRID]: "
            + "[ChangeRequestException: error]", this.logCapture.getMessage(0));
    }
}
