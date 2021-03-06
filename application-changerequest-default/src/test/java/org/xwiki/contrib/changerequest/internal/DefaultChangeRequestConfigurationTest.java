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

import java.time.temporal.ChronoUnit;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.contrib.changerequest.internal.strategies.AcceptAllMergeApprovalStrategy;
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
 * Tests for {@link DefaultChangeRequestConfiguration}.
 *
 * @version $Id$
 * @since 0.1
 */
@ComponentTest
class DefaultChangeRequestConfigurationTest
{
    @InjectMockComponents
    private DefaultChangeRequestConfiguration configuration;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    @Named("changerequest")
    private ConfigurationSource configurationSource;

    @RegisterExtension
    LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    private XWikiContext context;

    @BeforeEach
    void setup()
    {
        this.context = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(this.context);
    }

    @Test
    void getMergeApprovalStrategy()
    {
        when(this.configurationSource
            .getProperty("approvalStrategy", DefaultChangeRequestConfiguration.DEFAULT_APPROVAL_STRATEGY))
            .thenReturn("something");
        assertEquals("something", this.configuration.getMergeApprovalStrategy());
    }

    @Test
    void getChangeRequestSpaceLocation()
    {
        WikiReference wikiReference = new WikiReference("foo");
        when(this.context.getWikiReference()).thenReturn(wikiReference);
        SpaceReference expected = new SpaceReference("foo", "ChangeRequest", "Data");
        assertEquals(expected, this.configuration.getChangeRequestSpaceLocation());
    }

    @Test
    void getDurationUnit()
    {
        assertEquals(ChronoUnit.DAYS, this.configuration.getDurationUnit());

        when(this.configurationSource.getProperty("durationUnit")).thenReturn("absurd value");
        assertEquals(ChronoUnit.DAYS, this.configuration.getDurationUnit());
        assertEquals(1, logCapture.size());
        assertEquals("Unsupported duration unit [absurd value]. Fallback to days unit.", logCapture.getMessage(0));

        when(this.configurationSource.getProperty("durationUnit")).thenReturn("hours");
        assertEquals(ChronoUnit.HOURS, this.configuration.getDurationUnit());
    }
}
