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

import java.util.Optional;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.internal.cache.DiffCacheManager;
import org.xwiki.contrib.changerequest.storage.FileChangeStorageManager;
import org.xwiki.diff.DiffException;
import org.xwiki.diff.xml.XMLDiffConfiguration;
import org.xwiki.diff.xml.XMLDiffManager;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DefaultChangeRequestDiffManager}.
 *
 * @version $Id$
 * @since 1.3
 */
@ComponentTest
class DefaultChangeRequestDiffManagerTest
{
    @InjectMockComponents
    private DefaultChangeRequestDiffManager diffManager;

    @MockComponent
    private FileChangeStorageManager fileChangeStorageManager;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    @Named("html/unified")
    private XMLDiffManager xmlDiffManager;

    @MockComponent
    @Named("html")
    private Provider<XMLDiffConfiguration> xmlDiffConfigurationProvider;

    @MockComponent
    private DiffCacheManager diffCacheManager;

    private XWikiContext context;
    private XMLDiffConfiguration xmlDiffConfiguration;

    @BeforeEach
    void setup()
    {
        this.context = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(this.context);

        this.xmlDiffConfiguration = mock(XMLDiffConfiguration.class);
        when(this.xmlDiffConfigurationProvider.get()).thenReturn(this.xmlDiffConfiguration);
    }

    @Test
    void getHtmlDiffForEdition() throws ChangeRequestException, XWikiException, DiffException
    {
        FileChange fileChange = mock(FileChange.class);
        String expectedResult = "some changes";
        when(this.diffCacheManager.getRenderedDiff(fileChange)).thenReturn(Optional.of(expectedResult));

        assertEquals(expectedResult, this.diffManager.getHtmlDiff(fileChange));
        verifyNoInteractions(xmlDiffManager);

        when(this.diffCacheManager.getRenderedDiff(fileChange)).thenReturn(Optional.empty());

        when(fileChange.getType()).thenReturn(FileChange.FileChangeType.EDITION);
        XWikiDocument modifiedDoc = mock(XWikiDocument.class, "modifiedDoc");
        when(this.fileChangeStorageManager.getModifiedDocumentFromFileChange(fileChange)).thenReturn(modifiedDoc);
        when(this.fileChangeStorageManager.getPreviousDocumentFromFileChange(fileChange)).thenReturn(Optional.empty());

        assertNull(this.diffManager.getHtmlDiff(fileChange));
        verify(this.diffCacheManager).setRenderedDiff(fileChange, null);
        verifyNoInteractions(xmlDiffManager);

        XWikiDocument previousDoc = mock(XWikiDocument.class, "previousDoc");
        when(this.fileChangeStorageManager.getPreviousDocumentFromFileChange(fileChange))
            .thenReturn(Optional.of(previousDoc));

        String modifiedDocHtml = "modified doc html";
        when(modifiedDoc.displayDocument(Syntax.HTML_5_0, true, this.context)).thenReturn(modifiedDocHtml);

        String previousDocHtml = "previous doc html";
        when(previousDoc.displayDocument(Syntax.HTML_5_0, true, this.context)).thenReturn(previousDocHtml);

        expectedResult = "real diff";
        when(this.xmlDiffManager.diff(previousDocHtml, modifiedDocHtml, this.xmlDiffConfiguration))
            .thenReturn(expectedResult);
        assertEquals(expectedResult, this.diffManager.getHtmlDiff(fileChange));
        verify(this.diffCacheManager).setRenderedDiff(fileChange, expectedResult);
    }

    @Test
    void getHtmlDiffForCreation() throws ChangeRequestException, XWikiException, DiffException
    {
        FileChange fileChange = mock(FileChange.class);
        String expectedResult = "some changes";
        when(this.diffCacheManager.getRenderedDiff(fileChange)).thenReturn(Optional.of(expectedResult));

        assertEquals(expectedResult, this.diffManager.getHtmlDiff(fileChange));
        verifyNoInteractions(xmlDiffManager);

        when(this.diffCacheManager.getRenderedDiff(fileChange)).thenReturn(Optional.empty());

        when(fileChange.getType()).thenReturn(FileChange.FileChangeType.CREATION);
        XWikiDocument modifiedDoc = mock(XWikiDocument.class, "modifiedDoc");
        when(this.fileChangeStorageManager.getModifiedDocumentFromFileChange(fileChange)).thenReturn(modifiedDoc);

        String modifiedDocHtml = "modified doc html";
        when(modifiedDoc.displayDocument(Syntax.HTML_5_0, true, this.context)).thenReturn(modifiedDocHtml);

        expectedResult = "real diff";
        when(this.xmlDiffManager.diff("", modifiedDocHtml, this.xmlDiffConfiguration))
            .thenReturn(expectedResult);
        assertEquals(expectedResult, this.diffManager.getHtmlDiff(fileChange));
        verify(this.diffCacheManager).setRenderedDiff(fileChange, expectedResult);
    }

    @Test
    void getHtmlDiffForDeletion() throws ChangeRequestException, XWikiException, DiffException
    {
        FileChange fileChange = mock(FileChange.class);
        String expectedResult = "some changes";
        when(this.diffCacheManager.getRenderedDiff(fileChange)).thenReturn(Optional.of(expectedResult));

        assertEquals(expectedResult, this.diffManager.getHtmlDiff(fileChange));
        verifyNoInteractions(xmlDiffManager);

        when(this.diffCacheManager.getRenderedDiff(fileChange)).thenReturn(Optional.empty());

        when(fileChange.getType()).thenReturn(FileChange.FileChangeType.DELETION);
        when(this.fileChangeStorageManager.getPreviousDocumentFromFileChange(fileChange)).thenReturn(Optional.empty());

        assertNull(this.diffManager.getHtmlDiff(fileChange));
        verify(this.diffCacheManager).setRenderedDiff(fileChange, null);
        verifyNoInteractions(xmlDiffManager);

        XWikiDocument previousDoc = mock(XWikiDocument.class, "previousDoc");
        when(this.fileChangeStorageManager.getPreviousDocumentFromFileChange(fileChange))
            .thenReturn(Optional.of(previousDoc));

        String previousDocHtml = "previous doc html";
        when(previousDoc.displayDocument(Syntax.HTML_5_0, true, this.context)).thenReturn(previousDocHtml);

        expectedResult = "real diff";
        when(this.xmlDiffManager.diff(previousDocHtml, "", this.xmlDiffConfiguration))
            .thenReturn(expectedResult);
        assertEquals(expectedResult, this.diffManager.getHtmlDiff(fileChange));
        verify(this.diffCacheManager).setRenderedDiff(fileChange, expectedResult);
    }

    @Test
    void getHtmlDiffForNoChange() throws ChangeRequestException
    {
        FileChange fileChange = mock(FileChange.class);
        String expectedResult = "some changes";
        when(this.diffCacheManager.getRenderedDiff(fileChange)).thenReturn(Optional.of(expectedResult));

        assertEquals(expectedResult, this.diffManager.getHtmlDiff(fileChange));
        verifyNoInteractions(xmlDiffManager);

        when(this.diffCacheManager.getRenderedDiff(fileChange)).thenReturn(Optional.empty());

        when(fileChange.getType()).thenReturn(FileChange.FileChangeType.NO_CHANGE);

        assertEquals("", this.diffManager.getHtmlDiff(fileChange));
        verify(this.diffCacheManager).setRenderedDiff(fileChange, "");
    }
}