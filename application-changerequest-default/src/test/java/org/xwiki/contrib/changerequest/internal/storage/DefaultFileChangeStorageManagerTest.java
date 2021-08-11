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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.suigeneris.jrcs.rcs.Version;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.internal.FileChangeVersionManager;
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
import org.xwiki.test.LogLevel;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.LogCaptureExtension;
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
import com.xpn.xwiki.objects.BaseObject;
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

    @MockComponent
    private FileChangeVersionManager fileChangeVersionManager;

    @RegisterExtension
    LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

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
        when(fileChange.getPreviousPublishedVersion()).thenReturn("4.3");
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

        when(fileChange.getCreationDate()).thenReturn(new Date(42));
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        when(fileChange.getChangeRequest()).thenReturn(changeRequest);

        DocumentReference changeRequestDocReference = new DocumentReference("xwiki", "ChangeRequest", "Doc");
        when(this.changeRequestDocumentReferenceResolver.resolve(changeRequest)).thenReturn(changeRequestDocReference);
        when(modifiedDoc.getDocumentReferenceWithLocale())
            .thenReturn(new DocumentReference("xwiki", "Space", "Doc", Locale.FRENCH));

        DocumentReference fileStorageDocRef = new DocumentReference("5:xwiki5:Space3:Doc2:fr",
            changeRequestDocReference.getLastSpaceReference());
        XWikiDocument fileChangeDoc = mock(XWikiDocument.class);
        when(this.xWiki.getDocument(fileStorageDocRef, this.context)).thenReturn(fileChangeDoc);

        String version = "filechange-3.3";
        when(this.fileChangeVersionManager.getDocumentVersion(version)).thenReturn(new Version("3.3"));
        when(modifiedDoc.getId()).thenReturn(4895L);
        String expectedId = version + "-4895";

        when(fileChange.getId()).thenReturn(expectedId);
        XWikiAttachmentStoreInterface storeInterface = mock(XWikiAttachmentStoreInterface.class);
        when(this.xWiki.getDefaultAttachmentContentStore()).thenReturn(storeInterface);
        when(storeInterface.getHint()).thenReturn("storeHint");
        when(fileChangeDoc.setAttachment(any())).then(invocationOnMock -> {
            XWikiAttachment attachment = invocationOnMock.getArgument(0);
            assertEquals(expectedId + ".xml", attachment.getFilename());
            assertEquals("storeHint", attachment.getContentStore());

            return attachment;
        });
        when(fileChangeDoc.createXObject(DefaultFileChangeStorageManager.FILECHANGE_XCLASS, this.context))
            .thenReturn(3);
        BaseObject fileChangeObj = mock(BaseObject.class);
        when(fileChangeDoc.getXObject(DefaultFileChangeStorageManager.FILECHANGE_XCLASS, 3)).thenReturn(fileChangeObj);
        when(fileChange.getVersion()).thenReturn(version);
        when(fileChange.getPreviousVersion()).thenReturn("filechange-3.2");
        when(fileChange.getPreviousPublishedVersion()).thenReturn("2.13");

        this.fileChangeStorageManager.save(fileChange);
        verify(fileChange).setSaved(true);
        verify(this.xWiki).saveDocument(fileChangeDoc, this.context);
        verify(modifiedDoc).toXML(any(OutputStream.class), eq(true), eq(true), eq(true), eq(false), eq(this.context));
        verify(fileChangeDoc).setAttachment(any());
        verify(fileChangeDoc).setHidden(true);
        verify(fileChange).setId(expectedId);
        verify(fileChangeObj).set(DefaultFileChangeStorageManager.FILENAME_PROPERTY, expectedId + ".xml", this.context);
        verify(fileChangeObj).set(DefaultFileChangeStorageManager.VERSION_PROPERTY, "filechange-3.3", this.context);
        verify(fileChangeObj)
            .set(DefaultFileChangeStorageManager.PREVIOUS_PUBLISHED_VERSION_PROPERTY, "2.13", this.context);
        verify(fileChangeObj)
            .set(DefaultFileChangeStorageManager.PREVIOUS_VERSION_PROPERTY, "filechange-3.2", this.context);
        verify(fileChangeObj).set(DefaultFileChangeStorageManager.REFERENCE_PROPERTY, "xwiki:Space.Doc", this.context);
        verify(fileChangeObj)
            .set(DefaultFileChangeStorageManager.REFERENCE_LOCALE_PROPERTY, Locale.FRENCH, this.context);
        verify(modifiedDoc).setRCSVersion(new Version("3.3"));
    }

    @Test
    void load() throws Exception
    {
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        DocumentReference changedDocument = new DocumentReference("xwiki", "Space", "Document");

        DocumentReference changeRequestDocReference = new DocumentReference("xwiki", "ChangeRequest", "Doc");
        when(this.changeRequestDocumentReferenceResolver.resolve(changeRequest)).thenReturn(changeRequestDocReference);
        String uidSerialization = "5:xwiki5:Space8:Document0:";

        DocumentReference expectedFileStorageReference = new DocumentReference(uidSerialization,
            changeRequestDocReference.getLastSpaceReference());

        XWikiDocument fileStorageDoc = mock(XWikiDocument.class);
        when(this.xWiki.getDocument(expectedFileStorageReference, this.context)).thenReturn(fileStorageDoc);

        when(fileStorageDoc.isNew()).thenReturn(true);
        assertEquals(new ArrayList<>(), this.fileChangeStorageManager.load(changeRequest, changedDocument));
        assertEquals(1, this.logCapture.size());
        assertEquals("No file change found in [null].", this.logCapture.getMessage(0));
        DocumentReference targetEntity = new DocumentReference("xwiki", "SomePage", "WebHome", Locale.GERMAN);
        String serializedTargetEntity = "xwiki:SomePage.WebHome";
        when(fileStorageDoc.isNew()).thenReturn(false);
        BaseObject fileChangeObj1 = mock(BaseObject.class);
        BaseObject fileChangeObj2 = mock(BaseObject.class);
        when(fileStorageDoc.getXObjects(DefaultFileChangeStorageManager.FILECHANGE_XCLASS))
            .thenReturn(Arrays.asList(fileChangeObj1, fileChangeObj2));

        String filename1 = "file1.xml";
        String filename2 = "file2.xml";
        when(fileChangeObj1.getStringValue(DefaultFileChangeStorageManager.REFERENCE_PROPERTY))
            .thenReturn(serializedTargetEntity);
        when(fileChangeObj1.getStringValue(DefaultFileChangeStorageManager.FILENAME_PROPERTY))
            .thenReturn(filename1);
        when(fileChangeObj1.getStringValue(DefaultFileChangeStorageManager.PREVIOUS_PUBLISHED_VERSION_PROPERTY))
            .thenReturn("2.3");
        when(fileChangeObj1.getStringValue(DefaultFileChangeStorageManager.PREVIOUS_VERSION_PROPERTY))
            .thenReturn("2.3");
        when(fileChangeObj1.getStringValue(DefaultFileChangeStorageManager.VERSION_PROPERTY))
            .thenReturn("filechange-3.1");
        when(fileChangeObj1.getStringValue(DefaultFileChangeStorageManager.REFERENCE_LOCALE_PROPERTY))
            .thenReturn(Locale.GERMAN.toString());

        when(fileChangeObj2.getStringValue(DefaultFileChangeStorageManager.REFERENCE_PROPERTY))
            .thenReturn(serializedTargetEntity);
        when(fileChangeObj2.getStringValue(DefaultFileChangeStorageManager.FILENAME_PROPERTY))
            .thenReturn(filename2);
        when(fileChangeObj2.getStringValue(DefaultFileChangeStorageManager.PREVIOUS_PUBLISHED_VERSION_PROPERTY))
            .thenReturn("2.3");
        when(fileChangeObj2.getStringValue(DefaultFileChangeStorageManager.PREVIOUS_VERSION_PROPERTY))
            .thenReturn("filechange-3.1");
        when(fileChangeObj2.getStringValue(DefaultFileChangeStorageManager.VERSION_PROPERTY))
            .thenReturn("filechange-3.2");
        when(fileChangeObj2.getStringValue(DefaultFileChangeStorageManager.REFERENCE_LOCALE_PROPERTY))
            .thenReturn(Locale.GERMAN.toString());

        XWikiAttachment attachment1 = mock(XWikiAttachment.class);
        XWikiAttachment attachment2 = mock(XWikiAttachment.class);

        when(fileStorageDoc.getAttachment(filename1)).thenReturn(attachment1);
        when(fileStorageDoc.getAttachment(filename2)).thenReturn(attachment2);

        when(attachment1.getContentInputStream(this.context))
            .thenReturn(getClass().getClassLoader().getResourceAsStream("filechange.xml"));
        when(attachment2.getContentInputStream(this.context))
            .thenReturn(getClass().getClassLoader().getResourceAsStream("filechange2.xml"));

        DocumentReference authorDocReference = new DocumentReference("xwiki", "XWiki", "surli");
        UserReference authorReference = mock(UserReference.class);
        when(this.userReferenceResolver.resolve(authorDocReference)).thenReturn(authorReference);

        FileChange expected1 = new FileChange(changeRequest)
            .setCreationDate(new Date(1624961293000L))
            .setAuthor(authorReference)
            .setSaved(true)
            .setId("file1")
            .setTargetEntity(targetEntity)
            .setPreviousVersion("2.3")
            .setPreviousPublishedVersion("2.3")
            .setVersion("filechange-3.1");

        FileChange expected2 = new FileChange(changeRequest)
            .setCreationDate(new Date(1624961293000L))
            .setAuthor(authorReference)
            .setSaved(true)
            .setId("file2")
            .setTargetEntity(targetEntity)
            .setPreviousVersion("filechange-3.1")
            .setPreviousPublishedVersion("2.3")
            .setVersion("filechange-3.2");

        ArrayList<FileChange> expected = new ArrayList<>(Arrays.asList(expected1, expected2));

        List<FileChange> fileChanges = this.fileChangeStorageManager.load(changeRequest, changedDocument);
        assertEquals(2, fileChanges.size());

        expected1.setModifiedDocument(fileChanges.get(0).getModifiedDocument());
        expected2.setModifiedDocument(fileChanges.get(1).getModifiedDocument());
        assertEquals(expected, fileChanges);
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
        when(fileChange.getPreviousPublishedVersion()).thenReturn("1.3");
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
