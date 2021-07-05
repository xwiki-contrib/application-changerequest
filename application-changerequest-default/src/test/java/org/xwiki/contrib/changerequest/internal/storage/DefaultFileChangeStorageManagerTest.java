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
package org.xwiki.contrib.changerequest.internal.storage;

import java.io.File;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.suigeneris.jrcs.rcs.Version;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.internal.UserReferenceConverter;
import org.xwiki.environment.Environment;
import org.xwiki.filter.internal.DefaultFilterDescriptorManager;
import org.xwiki.filter.internal.converter.FilterEventParametersConverter;
import org.xwiki.filter.xar.internal.input.AttachmentReader;
import org.xwiki.filter.xar.internal.input.ClassPropertyReader;
import org.xwiki.filter.xar.internal.input.ClassReader;
import org.xwiki.filter.xar.internal.input.DocumentLocaleReader;
import org.xwiki.filter.xar.internal.input.WikiObjectPropertyReader;
import org.xwiki.filter.xar.internal.input.WikiObjectReader;
import org.xwiki.filter.xar.internal.input.XARInputFilterStream;
import org.xwiki.filter.xar.internal.input.XARInputFilterStreamFactory;
import org.xwiki.filter.xar.internal.output.XAROutputFilterStreamFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.properties.internal.DefaultBeanManager;
import org.xwiki.properties.internal.DefaultConverterManager;
import org.xwiki.properties.internal.converter.ConvertUtilsConverter;
import org.xwiki.properties.internal.converter.EnumConverter;
import org.xwiki.properties.internal.converter.LocaleConverter;
import org.xwiki.rendering.internal.transformation.DefaultRenderingContext;
import org.xwiki.store.merge.MergeDocumentResult;
import org.xwiki.store.merge.MergeManager;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.user.UserReferenceSerializer;
import org.xwiki.xar.internal.XarObjectPropertySerializerManager;
import org.xwiki.xar.internal.property.DefaultXarObjectPropertySerializer;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.DocumentRevisionProvider;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.merge.MergeConfiguration;
import com.xpn.xwiki.internal.ReadOnlyXWikiContextProvider;
import com.xpn.xwiki.internal.filter.XWikiDocumentFilterUtils;
import com.xpn.xwiki.internal.filter.output.BaseClassOutputFilterStream;
import com.xpn.xwiki.internal.filter.output.BaseObjectOutputFilterStream;
import com.xpn.xwiki.internal.filter.output.BasePropertyOutputFilterStream;
import com.xpn.xwiki.internal.filter.output.PropertyClassOutputFilterStream;
import com.xpn.xwiki.internal.filter.output.XWikiAttachmentOutputFilterStream;
import com.xpn.xwiki.internal.filter.output.XWikiDocumentOutputFilterStream;
import com.xpn.xwiki.internal.localization.XWikiLocalizationContext;
import com.xpn.xwiki.store.XWikiAttachmentStoreInterface;
import com.xpn.xwiki.test.reference.ReferenceComponentList;
import com.xpn.xwiki.web.Utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DefaultFileChangeStorageManager}.
 *
 * @version $Id$
 * @since 0.1
 */
@ComponentTest
// All those components are needed for loading the XML file.
@ReferenceComponentList
@ComponentList({
    XWikiDocumentFilterUtils.class,
    XARInputFilterStreamFactory.class,
    XAROutputFilterStreamFactory.class,
    DefaultBeanManager.class,
    DefaultConverterManager.class,
    EnumConverter.class,
    ConvertUtilsConverter.class,
    XWikiDocumentOutputFilterStream.class,
    DefaultFilterDescriptorManager.class,
    DefaultRenderingContext.class,
    XWikiLocalizationContext.class,
    XWikiAttachmentOutputFilterStream.class,
    BaseClassOutputFilterStream.class,
    PropertyClassOutputFilterStream.class,
    BaseObjectOutputFilterStream.class,
    BasePropertyOutputFilterStream.class,
    XARInputFilterStream.class,
    FilterEventParametersConverter.class,
    LocaleConverter.class,
    DocumentLocaleReader.class,
    WikiObjectReader.class,
    ClassReader.class,
    ClassPropertyReader.class,
    WikiObjectPropertyReader.class,
    AttachmentReader.class,
    XarObjectPropertySerializerManager.class,
    DefaultXarObjectPropertySerializer.class,
    ReadOnlyXWikiContextProvider.class
})
public class DefaultFileChangeStorageManagerTest
{
    @InjectMockComponents
    private DefaultFileChangeStorageManager fileChangeStorageManager;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private UserReferenceConverter converter;

    @MockComponent
    private DocumentReferenceResolver<ChangeRequest> changeRequestDocumentReferenceResolver;

    @MockComponent
    private DocumentRevisionProvider documentRevisionProvider;

    @MockComponent
    @Named("document")
    private UserReferenceResolver<DocumentReference> userReferenceResolver;

    @MockComponent
    private UserReferenceConverter userReferenceConverter;

    @MockComponent
    private UserReferenceSerializer<String> userReferenceSerializer;

    @MockComponent
    private Environment environment;

    @MockComponent
    private Execution execution;

    @MockComponent
    private MergeManager mergeManager;

    private XWikiContext context;

    private XWiki xWiki;

    @BeforeComponent
    void beforeComponent(MockitoComponentManager componentManager) throws Exception
    {
        Utils.setComponentManager(componentManager);
        componentManager.registerComponent(ComponentManager.class, "context", componentManager);
    }

    @BeforeEach
    void setup()
    {
        this.context = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(this.context);

        this.xWiki = mock(XWiki.class);
        when(this.context.getWiki()).thenReturn(this.xWiki);

        when(this.environment.getTemporaryDirectory()).thenReturn(new File(System.getProperty("java.io.tmpdir")));
    }

    @Test
    void getModifiedDocumentFromFileChange() throws ChangeRequestException
    {
        FileChange fileChange = mock(FileChange.class);
        XWikiDocument document = mock(XWikiDocument.class);
        when(fileChange.getModifiedDocument()).thenReturn(document);
        when(document.getRCSVersion()).thenReturn(new Version("2.1"));
        assertEquals(document, this.fileChangeStorageManager.getModifiedDocumentFromFileChange(fileChange));
        verify(document).setRCSVersion(new Version("2.1").next());
    }

    @Test
    void getCurrentDocumentFromFileChange() throws Exception
    {
        FileChange fileChange = mock(FileChange.class);
        DocumentReference targetEntity = mock(DocumentReference.class);
        when(fileChange.getTargetEntity()).thenReturn(targetEntity);
        XWikiDocument document = mock(XWikiDocument.class);
        when(this.xWiki.getDocument(targetEntity, this.context)).thenReturn(document);
        assertEquals(document, this.fileChangeStorageManager.getCurrentDocumentFromFileChange(fileChange));
    }

    @Test
    void getOldDocumentFromFileChange() throws Exception
    {
        FileChange fileChange = mock(FileChange.class);
        DocumentReference targetEntity = mock(DocumentReference.class);
        when(fileChange.getTargetEntity()).thenReturn(targetEntity);
        XWikiDocument document = mock(XWikiDocument.class);
        when(fileChange.getSourceVersion()).thenReturn("4.3");
        when(this.documentRevisionProvider.getRevision(targetEntity, "4.3")).thenReturn(document);
        assertEquals(document, this.fileChangeStorageManager.getPreviousDocumentFromFileChange(fileChange));
    }

    @Test
    void save() throws Exception
    {
        FileChange fileChange = mock(FileChange.class);
        when(fileChange.isSaved()).thenReturn(true);
        this.fileChangeStorageManager.save(fileChange);
        verify(this.xWiki, never()).saveDocument(any(), any());

        when(fileChange.isSaved()).thenReturn(false);
        XWikiDocument modifiedDoc = mock(XWikiDocument.class);
        when(fileChange.getModifiedDocument()).thenReturn(modifiedDoc);
        UserReference author = mock(UserReference.class);
        when(fileChange.getAuthor()).thenReturn(author);
        DocumentReference authorDocReference = mock(DocumentReference.class);
        when(this.userReferenceConverter.convert(author)).thenReturn(authorDocReference);

        DocumentReference targetEntity = new DocumentReference("xwiki", "Sandbox", "WebHome");
        when(fileChange.getTargetEntity()).thenReturn(targetEntity);

        String serializedAuthor = "XWiki.Foo";
        when(this.userReferenceSerializer.serialize(author)).thenReturn(serializedAuthor);
        String serializedDoc = "xwiki:Sandbox.WebHome";

        when(fileChange.getCreationDate()).thenReturn(new Date(42));
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        when(fileChange.getChangeRequest()).thenReturn(changeRequest);

        DocumentReference changeRequestDocReference = mock(DocumentReference.class);
        when(this.changeRequestDocumentReferenceResolver.resolve(changeRequest)).thenReturn(changeRequestDocReference);
        XWikiDocument changeRequestDoc = mock(XWikiDocument.class);
        when(this.xWiki.getDocument(changeRequestDocReference, this.context)).thenReturn(changeRequestDoc);

        String expectedFilename = String.format("%s-%s-%s-%s",
            DefaultFileChangeStorageManager.FILE_CHANGE_CONSTANT_NAME,
            serializedDoc,
            serializedAuthor,
            DefaultFileChangeStorageManager.DATE_FORMAT.format(new Date(42)));

        when(fileChange.getId()).thenReturn(expectedFilename);
        XWikiAttachmentStoreInterface storeInterface = mock(XWikiAttachmentStoreInterface.class);
        when(this.xWiki.getDefaultAttachmentContentStore()).thenReturn(storeInterface);
        when(storeInterface.getHint()).thenReturn("storeHint");
        when(changeRequestDoc.setAttachment(any())).then(invocationOnMock -> {
            XWikiAttachment attachment = invocationOnMock.getArgument(0);
            assertEquals(expectedFilename + ".xml", attachment.getFilename());
            assertEquals("storeHint", attachment.getContentStore());

            return attachment;
        });
        this.fileChangeStorageManager.save(fileChange);
        verify(fileChange).setSaved(true);
        verify(this.xWiki).saveDocument(changeRequestDoc, this.context);
        verify(modifiedDoc).toXML(any(OutputStream.class), eq(true), eq(true), eq(true), eq(false), eq(this.context));
        verify(changeRequestDoc).setAttachment(any());
        verify(fileChange).setId(expectedFilename);
    }

    @Test
    void load() throws Exception
    {
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        DocumentReference changeRequestDocReference = mock(DocumentReference.class);
        when(this.changeRequestDocumentReferenceResolver.resolve(changeRequest)).thenReturn(changeRequestDocReference);
        XWikiDocument changeRequestDoc = mock(XWikiDocument.class);
        when(this.xWiki.getDocument(changeRequestDocReference, this.context)).thenReturn(changeRequestDoc);

        assertEquals(Optional.empty(), this.fileChangeStorageManager.load(changeRequest, "someid"));
        verify(this.xWiki, never()).getDocument(changeRequestDocReference, this.context);

        assertEquals(Optional.empty(), this.fileChangeStorageManager.load(changeRequest, "filechange-someid-pagename"));
        verify(this.xWiki, never()).getDocument(changeRequestDocReference, this.context);

        String fileChangeId = DefaultFileChangeStorageManager.FILE_CHANGE_CONSTANT_NAME
            + "-Main.Sandbox-XWiki.Foo-" + DefaultFileChangeStorageManager.DATE_FORMAT.format(new Date(42));
        XWikiAttachment attachment = mock(XWikiAttachment.class);
        when(changeRequestDoc.getAttachment(fileChangeId + ".xml")).thenReturn(attachment);
        when(attachment.getContentInputStream(this.context))
            .thenReturn(getClass().getClassLoader().getResourceAsStream("filechange.xml"));
        DocumentReference authorDocReference = new DocumentReference("xwiki", "XWiki", "surli");
        UserReference authorReference = mock(UserReference.class);
        when(this.userReferenceResolver.resolve(authorDocReference)).thenReturn(authorReference);
        DocumentReference targetEntity = new DocumentReference("xwiki", "SomePage", "WebHome", Locale.ROOT);
        FileChange expected = new FileChange(changeRequest)
            .setCreationDate(new Date(1624961293000L))
            .setAuthor(authorReference)
            .setSaved(true)
            .setId(fileChangeId)
            .setTargetEntity(targetEntity)
            .setSourceVersion("1.2");

        Optional<FileChange> fileChangeOptional = this.fileChangeStorageManager.load(changeRequest, fileChangeId);
        assertTrue(fileChangeOptional.isPresent());
        FileChange fileChange = fileChangeOptional.get();
        expected.setModifiedDocument(fileChange.getModifiedDocument());

        assertEquals(expected, fileChange);
    }

    @Test
    void merge() throws Exception
    {
        FileChange fileChange = mock(FileChange.class);
        XWikiDocument modifiedDocument = mock(XWikiDocument.class);
        when(fileChange.getModifiedDocument()).thenReturn(modifiedDocument);
        when(modifiedDocument.getRCSVersion()).thenReturn(new Version("2.1"));

        DocumentReference targetEntity = mock(DocumentReference.class);
        when(fileChange.getTargetEntity()).thenReturn(targetEntity);
        XWikiDocument previousDocument = mock(XWikiDocument.class);
        when(fileChange.getSourceVersion()).thenReturn("1.3");
        when(this.documentRevisionProvider.getRevision(targetEntity, "1.3")).thenReturn(previousDocument);

        XWikiDocument currentDocument = mock(XWikiDocument.class);
        when(this.xWiki.getDocument(targetEntity, this.context)).thenReturn(currentDocument);

        DocumentReference userDocReference = mock(DocumentReference.class);
        when(context.getUserReference()).thenReturn(userDocReference);
        when(modifiedDocument.getDocumentReference()).thenReturn(targetEntity);

        MergeDocumentResult mergeDocumentResult = mock(MergeDocumentResult.class);
        when(mergeManager.mergeDocument(eq(previousDocument), eq(modifiedDocument), eq(currentDocument), any()))
            .then(invocationOnMock -> {
                MergeConfiguration mergeConfiguration = invocationOnMock.getArgument(3);
                assertEquals(userDocReference, mergeConfiguration.getUserReference());
                assertEquals(targetEntity, mergeConfiguration.getConcernedDocument());
                assertTrue(mergeConfiguration.isProvidedVersionsModifiables());
                return mergeDocumentResult;
            });
        when(mergeDocumentResult.hasConflicts()).thenReturn(true);
        ChangeRequestException changeRequestException =
            assertThrows(ChangeRequestException.class, () -> this.fileChangeStorageManager.merge(fileChange));
        assertEquals("Cannot merge the file change since it has conflicts.", changeRequestException.getMessage());
        verify(this.xWiki, never()).saveDocument(any(XWikiDocument.class), anyString(), any());

        when(mergeDocumentResult.hasConflicts()).thenReturn(false);
        when(mergeDocumentResult.isModified()).thenReturn(false);
        this.fileChangeStorageManager.merge(fileChange);
        verify(this.xWiki, never()).saveDocument(any(XWikiDocument.class), anyString(), any());

        when(mergeDocumentResult.isModified()).thenReturn(true);
        when(mergeDocumentResult.getMergeResult()).thenReturn(currentDocument);
        UserReference author = mock(UserReference.class);
        when(fileChange.getAuthor()).thenReturn(author);
        when(this.userReferenceConverter.convert(author)).thenReturn(userDocReference);
        this.fileChangeStorageManager.merge(fileChange);
        verify(currentDocument).setContentAuthorReference(userDocReference);
        verify(this.xWiki).saveDocument(currentDocument, "Save after change request merge", this.context);
    }
}
