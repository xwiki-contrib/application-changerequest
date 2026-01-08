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
package org.xwiki.contrib.changerequest.internal.diff;

import java.util.Optional;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.changerequest.ChangeRequestConfiguration;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.diff.ChangeRequestDiffRenderContent;
import org.xwiki.contrib.changerequest.diff.HtmlDiffResult;
import org.xwiki.contrib.changerequest.internal.cache.DiffCacheManager;
import org.xwiki.contrib.changerequest.storage.FileChangeStorageManager;
import org.xwiki.diff.DiffException;
import org.xwiki.diff.xml.XMLDiffConfiguration;
import org.xwiki.diff.xml.XMLDiffManager;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
    @Named("html/unified")
    private XMLDiffManager xmlDiffManager;

    @MockComponent
    @Named("html")
    private Provider<XMLDiffConfiguration> xmlDiffConfigurationProvider;

    @MockComponent
    private DiffCacheManager diffCacheManager;

    @MockComponent
    private ChangeRequestConfiguration configuration;

    @MockComponent
    private ChangeRequestDiffRenderContent diffRenderContent;

    @MockComponent
    private RequiredSkinExtensionsRecorder requiredSkinExtensionsRecorder;

    private XMLDiffConfiguration xmlDiffConfiguration;

    @BeforeComponent
    void setupContext(MockitoComponentManager componentManager) throws Exception
    {
        componentManager.registerComponent(ComponentManager.class, "context", componentManager);
    }

    @BeforeEach
    void setup() throws ChangeRequestException
    {
        this.xmlDiffConfiguration = mock(XMLDiffConfiguration.class);
        when(this.xmlDiffConfigurationProvider.get()).thenReturn(this.xmlDiffConfiguration);
        when(this.diffRenderContent.getRenderedContent(isNull(), any(FileChange.class))).thenReturn("");
    }

    @Test
    void getHtmlDiffForEdition() throws ChangeRequestException, XWikiException, DiffException
    {
        FileChange fileChange = mock(FileChange.class);
        HtmlDiffResult expectedResult = new HtmlDiffResult("some changes", "some required skins");
        when(this.diffCacheManager.getRenderedDiff(fileChange)).thenReturn(Optional.of(expectedResult));

        assertEquals(expectedResult, this.diffManager.getHtmlDiff(fileChange));
        verifyNoInteractions(xmlDiffManager);

        when(this.diffCacheManager.getRenderedDiff(fileChange)).thenReturn(Optional.empty());

        when(fileChange.getType()).thenReturn(FileChange.FileChangeType.EDITION);
        XWikiDocument modifiedDoc = mock(XWikiDocument.class, "modifiedDoc");
        when(this.fileChangeStorageManager.getModifiedDocumentFromFileChange(fileChange)).thenReturn(modifiedDoc);
        when(this.fileChangeStorageManager.getPreviousDocumentFromFileChange(fileChange)).thenReturn(Optional.empty());

        assertNull(this.diffManager.getHtmlDiff(fileChange));
        verify(this.diffCacheManager, never()).setRenderedDiff(fileChange, null);
        verifyNoInteractions(xmlDiffManager);

        XWikiDocument previousDoc = mock(XWikiDocument.class, "previousDoc");
        when(this.fileChangeStorageManager.getPreviousDocumentFromFileChange(fileChange))
            .thenReturn(Optional.of(previousDoc));

        String modifiedDocHtml = "modified doc html";
        when(this.diffRenderContent.getRenderedContent(modifiedDoc, fileChange)).thenReturn(modifiedDocHtml);

        String previousDocHtml = "previous doc html";
        when(this.diffRenderContent.getRenderedContent(previousDoc, fileChange)).thenReturn(previousDocHtml);

        String expectedDiffResult = "real diff";
        when(this.xmlDiffManager.diff(previousDocHtml, modifiedDocHtml, this.xmlDiffConfiguration))
            .thenReturn(expectedDiffResult);
        String expectedRequiredSkinResult = "skin extensions";
        when(this.requiredSkinExtensionsRecorder.stop()).thenReturn(expectedRequiredSkinResult);
        expectedResult = new HtmlDiffResult(expectedDiffResult, expectedRequiredSkinResult);
        assertEquals(expectedResult, this.diffManager.getHtmlDiff(fileChange));
        verify(this.diffCacheManager).setRenderedDiff(fileChange, expectedResult);
        verify(this.requiredSkinExtensionsRecorder, times(2)).start();
    }

    @Test
    void getHtmlDiffForEditionOtherRenderingComponent(MockitoComponentManager componentManager)
        throws Exception
    {
        when(configuration.getRenderedDiffComponent()).thenReturn("customHint");
        ChangeRequestDiffRenderContent customDiffRenderContent = mock(ChangeRequestDiffRenderContent.class);
        componentManager.registerComponent(ChangeRequestDiffRenderContent.class, "customHint", customDiffRenderContent);
        FileChange fileChange = mock(FileChange.class);
        HtmlDiffResult expectedResult = new HtmlDiffResult("some changes", "some required skins");
        when(this.diffCacheManager.getRenderedDiff(fileChange)).thenReturn(Optional.of(expectedResult));

        assertEquals(expectedResult, this.diffManager.getHtmlDiff(fileChange));
        verifyNoInteractions(xmlDiffManager);

        when(this.diffCacheManager.getRenderedDiff(fileChange)).thenReturn(Optional.empty());

        when(fileChange.getType()).thenReturn(FileChange.FileChangeType.EDITION);
        XWikiDocument modifiedDoc = mock(XWikiDocument.class, "modifiedDoc");
        when(this.fileChangeStorageManager.getModifiedDocumentFromFileChange(fileChange)).thenReturn(modifiedDoc);
        when(this.fileChangeStorageManager.getPreviousDocumentFromFileChange(fileChange)).thenReturn(Optional.empty());

        assertNull(this.diffManager.getHtmlDiff(fileChange));
        verify(this.diffCacheManager, never()).setRenderedDiff(fileChange, null);
        verifyNoInteractions(xmlDiffManager);

        XWikiDocument previousDoc = mock(XWikiDocument.class, "previousDoc");
        when(this.fileChangeStorageManager.getPreviousDocumentFromFileChange(fileChange))
            .thenReturn(Optional.of(previousDoc));

        String modifiedDocHtml = "modified doc html";
        when(customDiffRenderContent.getRenderedContent(modifiedDoc, fileChange)).thenReturn(modifiedDocHtml);

        String previousDocHtml = "previous doc html";
        when(customDiffRenderContent.getRenderedContent(previousDoc, fileChange)).thenReturn(previousDocHtml);

        String expectedDiffResult = "real diff";
        when(this.xmlDiffManager.diff(previousDocHtml, modifiedDocHtml, this.xmlDiffConfiguration))
            .thenReturn(expectedDiffResult);
        String expectedRequiredSkinResult = "skin extensions";
        when(this.requiredSkinExtensionsRecorder.stop()).thenReturn(expectedRequiredSkinResult);
        expectedResult = new HtmlDiffResult(expectedDiffResult, expectedRequiredSkinResult);
        assertEquals(expectedResult, this.diffManager.getHtmlDiff(fileChange));
        verify(this.diffCacheManager).setRenderedDiff(fileChange, expectedResult);
        verifyNoInteractions(this.diffRenderContent);
        verify(this.requiredSkinExtensionsRecorder, times(2)).start();
    }

    @Test
    void getHtmlDiffForCreation() throws ChangeRequestException, XWikiException, DiffException
    {
        FileChange fileChange = mock(FileChange.class);
        HtmlDiffResult expectedResult = new HtmlDiffResult("some changes", "some required skins");
        when(this.diffCacheManager.getRenderedDiff(fileChange)).thenReturn(Optional.of(expectedResult));

        assertEquals(expectedResult, this.diffManager.getHtmlDiff(fileChange));
        verifyNoInteractions(xmlDiffManager);

        when(this.diffCacheManager.getRenderedDiff(fileChange)).thenReturn(Optional.empty());

        when(fileChange.getType()).thenReturn(FileChange.FileChangeType.CREATION);
        XWikiDocument modifiedDoc = mock(XWikiDocument.class, "modifiedDoc");
        when(this.fileChangeStorageManager.getModifiedDocumentFromFileChange(fileChange)).thenReturn(modifiedDoc);

        String modifiedDocHtml = "modified doc html";
        when(this.diffRenderContent.getRenderedContent(modifiedDoc, fileChange)).thenReturn(modifiedDocHtml);

        String expectedDiffResult = "real diff";
        when(this.xmlDiffManager.diff("", modifiedDocHtml, this.xmlDiffConfiguration))
            .thenReturn(expectedDiffResult);
        String expectedRequiredSkinResult = "skin extensions";
        when(this.requiredSkinExtensionsRecorder.stop()).thenReturn(expectedRequiredSkinResult);
        expectedResult = new HtmlDiffResult(expectedDiffResult, expectedRequiredSkinResult);

        assertEquals(expectedResult, this.diffManager.getHtmlDiff(fileChange));
        verify(this.diffCacheManager).setRenderedDiff(fileChange, expectedResult);
        verify(this.requiredSkinExtensionsRecorder).start();
    }

    @Test
    void getHtmlDiffForDeletion() throws ChangeRequestException, XWikiException, DiffException
    {
        FileChange fileChange = mock(FileChange.class);
        HtmlDiffResult expectedResult = new HtmlDiffResult("some changes", "some required skins");
        when(this.diffCacheManager.getRenderedDiff(fileChange)).thenReturn(Optional.of(expectedResult));

        assertEquals(expectedResult, this.diffManager.getHtmlDiff(fileChange));
        verifyNoInteractions(xmlDiffManager);

        when(this.diffCacheManager.getRenderedDiff(fileChange)).thenReturn(Optional.empty());

        when(fileChange.getType()).thenReturn(FileChange.FileChangeType.DELETION);
        when(this.fileChangeStorageManager.getPreviousDocumentFromFileChange(fileChange)).thenReturn(Optional.empty());

        assertNull(this.diffManager.getHtmlDiff(fileChange));
        verify(this.diffCacheManager, never()).setRenderedDiff(fileChange, null);
        verifyNoInteractions(xmlDiffManager);

        XWikiDocument previousDoc = mock(XWikiDocument.class, "previousDoc");
        when(this.fileChangeStorageManager.getPreviousDocumentFromFileChange(fileChange))
            .thenReturn(Optional.of(previousDoc));

        String previousDocHtml = "previous doc html";
        when(this.diffRenderContent.getRenderedContent(previousDoc, fileChange)).thenReturn(previousDocHtml);

        String expectedDiffResult = "real diff";
        when(this.xmlDiffManager.diff(previousDocHtml, "", this.xmlDiffConfiguration)).thenReturn(expectedDiffResult);
        String expectedRequiredSkinResult = "skin extensions";
        when(this.requiredSkinExtensionsRecorder.stop()).thenReturn(expectedRequiredSkinResult);
        expectedResult = new HtmlDiffResult(expectedDiffResult, expectedRequiredSkinResult);


        assertEquals(expectedResult, this.diffManager.getHtmlDiff(fileChange));
        verify(this.diffCacheManager).setRenderedDiff(fileChange, expectedResult);
        verify(this.requiredSkinExtensionsRecorder, times(2)).start();
    }

    @Test
    void getHtmlDiffForNoChange() throws ChangeRequestException
    {
        FileChange fileChange = mock(FileChange.class);
        HtmlDiffResult expectedResult = new HtmlDiffResult("some changes", "some required skins");
        when(this.diffCacheManager.getRenderedDiff(fileChange)).thenReturn(Optional.of(expectedResult));

        assertEquals(expectedResult, this.diffManager.getHtmlDiff(fileChange));
        verifyNoInteractions(xmlDiffManager);

        when(this.diffCacheManager.getRenderedDiff(fileChange)).thenReturn(Optional.empty());

        when(fileChange.getType()).thenReturn(FileChange.FileChangeType.NO_CHANGE);

        when(this.requiredSkinExtensionsRecorder.stop()).thenReturn("");
        expectedResult = new HtmlDiffResult("", "");
        assertEquals(expectedResult, this.diffManager.getHtmlDiff(fileChange));
        verify(this.diffCacheManager).setRenderedDiff(fileChange, expectedResult);
        verify(requiredSkinExtensionsRecorder).start();
    }
}