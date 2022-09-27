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
import java.util.Optional;

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
import org.xwiki.contrib.changerequest.ChangeRequestStatus;
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
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.document.DocumentAuthors;
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
import com.xpn.xwiki.XWikiException;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
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
class DefaultFileChangeStorageManagerTest
{
    private static final String SAVE_MESSAGE = "Expected save message";

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
    private UserReferenceSerializer<String> userReferenceSerializer;

    @MockComponent
    private Environment environment;

    @MockComponent
    private Execution execution;

    @MockComponent
    private MergeManager mergeManager;

    @MockComponent
    private FileChangeVersionManager fileChangeVersionManager;

    @MockComponent
    private UserReferenceResolver<String> stringUserReferenceResolver;

    @MockComponent
    private ContextualLocalizationManager contextualLocalizationManager;

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
        when(document.clone()).thenReturn(document);
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
        when(fileChange.getPreviousPublishedVersionDate()).thenReturn(new Date(598));
        when(document.getDate()).thenReturn(new Date(598));
        when(fileChange.getChangeRequest()).thenReturn(mock(ChangeRequest.class));
        assertEquals(Optional.of(document),
            this.fileChangeStorageManager.getPreviousDocumentFromFileChange(fileChange));
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
        DocumentAuthors documentAuthors = mock(DocumentAuthors.class);
        when(modifiedDoc.getAuthors()).thenReturn(documentAuthors);

        DocumentReference targetEntity = new DocumentReference("xwiki", "Sandbox", "WebHome");
        when(fileChange.getTargetEntity()).thenReturn(targetEntity);

        String serializedAuthor = "XWiki.Foo";
        when(this.userReferenceSerializer.serialize(author)).thenReturn(serializedAuthor);

        when(fileChange.getCreationDate()).thenReturn(new Date(42));
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        when(fileChange.getChangeRequest()).thenReturn(changeRequest);

        DocumentReference changeRequestDocReference = new DocumentReference("xwiki", "ChangeRequest", "Doc");
        when(this.changeRequestDocumentReferenceResolver.resolve(changeRequest)).thenReturn(changeRequestDocReference);

        DocumentReference fileStorageDocRef = new DocumentReference("5:xwiki5:Space3:Doc2:fr",
            changeRequestDocReference.getLastSpaceReference());
        XWikiDocument fileChangeDoc = mock(XWikiDocument.class);
        when(this.xWiki.getDocument(fileStorageDocRef, this.context)).thenReturn(fileChangeDoc);
        when(fileChangeDoc.isNew()).thenReturn(true);
        DocumentAuthors fileChangeAuthors = mock(DocumentAuthors.class);
        when(fileChangeDoc.getAuthors()).thenReturn(fileChangeAuthors);

        String version = "filechange-3.3";
        when(this.fileChangeVersionManager.getDocumentVersion(version)).thenReturn(new Version("3.3"));
        when(modifiedDoc.getId()).thenReturn(4895L);
        String expectedId = version + "-102366739979450084";

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
        when(fileChangeDoc.createXObject(FileChangeXClassInitializer.FILECHANGE_XCLASS, this.context))
            .thenReturn(3);
        BaseObject fileChangeObj = mock(BaseObject.class);
        when(fileChangeDoc.getXObject(FileChangeXClassInitializer.FILECHANGE_XCLASS, 3)).thenReturn(fileChangeObj);
        when(fileChange.getVersion()).thenReturn(version);
        when(fileChange.getPreviousVersion()).thenReturn("filechange-3.2");
        when(fileChange.getPreviousPublishedVersion()).thenReturn("2.13");
        when(fileChange.getType()).thenReturn(FileChange.FileChangeType.EDITION);
        when(fileChange.getTargetEntity()).thenReturn(new DocumentReference("xwiki", "Space", "Doc", Locale.FRENCH));

        this.fileChangeStorageManager.save(fileChange);
        verify(fileChange).setSaved(true);
        verify(this.xWiki).saveDocument(fileChangeDoc, this.context);
        verify(documentAuthors).setOriginalMetadataAuthor(author);
        verify(modifiedDoc).toXML(any(OutputStream.class), eq(true), eq(true), eq(true), eq(false), eq(this.context));
        verify(fileChangeDoc).setAttachment(any());
        verify(fileChangeDoc).setHidden(true);
        verify(fileChangeAuthors).setOriginalMetadataAuthor(author);
        verify(fileChange).setId(expectedId);
        verify(fileChangeObj).set(FileChangeXClassInitializer.FILENAME_PROPERTY, expectedId + ".xml", this.context);
        verify(fileChangeObj).set(FileChangeXClassInitializer.VERSION_PROPERTY, "filechange-3.3", this.context);
        verify(fileChangeObj)
            .set(FileChangeXClassInitializer.PREVIOUS_PUBLISHED_VERSION_PROPERTY, "2.13", this.context);
        verify(fileChangeObj)
            .set(FileChangeXClassInitializer.PREVIOUS_VERSION_PROPERTY, "filechange-3.2", this.context);
        verify(fileChangeObj).set(FileChangeXClassInitializer.REFERENCE_PROPERTY, "xwiki:Space.Doc", this.context);
        verify(fileChangeObj)
            .set(FileChangeXClassInitializer.REFERENCE_LOCALE_PROPERTY, Locale.FRENCH, this.context);
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
        BaseObject fileChangeObj3 = mock(BaseObject.class);
        when(fileStorageDoc.getXObjects(FileChangeXClassInitializer.FILECHANGE_XCLASS))
            .thenReturn(Arrays.asList(fileChangeObj1, fileChangeObj2, fileChangeObj3));

        String filename1 = "file1.xml";
        String filename2 = "file2.xml";
        String filename3 = "file3.xml";
        when(fileChangeObj1.getStringValue(FileChangeXClassInitializer.REFERENCE_PROPERTY))
            .thenReturn(serializedTargetEntity);
        when(fileChangeObj1.getStringValue(FileChangeXClassInitializer.FILENAME_PROPERTY))
            .thenReturn(filename1);
        when(fileChangeObj1.getStringValue(FileChangeXClassInitializer.PREVIOUS_PUBLISHED_VERSION_PROPERTY))
            .thenReturn("2.3");
        when(fileChangeObj1.getDateValue(FileChangeXClassInitializer.PREVIOUS_PUBLISHED_VERSION_DATE_PROPERTY))
            .thenReturn(new Date(666));
        when(fileChangeObj1.getStringValue(FileChangeXClassInitializer.PREVIOUS_VERSION_PROPERTY))
            .thenReturn("2.3");
        when(fileChangeObj1.getStringValue(FileChangeXClassInitializer.VERSION_PROPERTY))
            .thenReturn("filechange-3.1");
        when(fileChangeObj1.getStringValue(FileChangeXClassInitializer.REFERENCE_LOCALE_PROPERTY))
            .thenReturn(Locale.GERMAN.toString());
        when(fileChangeObj1.getDateValue(FileChangeXClassInitializer.CREATION_DATE_PROPERTY))
            .thenReturn(new Date(4242));
        when(fileChangeObj1.getStringValue(FileChangeXClassInitializer.AUTHOR_PROPERTY))
            .thenReturn("xwiki:XWiki.surli");
        when(fileChangeObj1.getStringValue(FileChangeXClassInitializer.TYPE_PROPERTY))
            .thenReturn("edition");

        when(fileChangeObj2.getStringValue(FileChangeXClassInitializer.REFERENCE_PROPERTY))
            .thenReturn(serializedTargetEntity);
        when(fileChangeObj2.getStringValue(FileChangeXClassInitializer.FILENAME_PROPERTY))
            .thenReturn(filename2);
        when(fileChangeObj2.getStringValue(FileChangeXClassInitializer.PREVIOUS_PUBLISHED_VERSION_PROPERTY))
            .thenReturn("2.3");
        when(fileChangeObj2.getDateValue(FileChangeXClassInitializer.PREVIOUS_PUBLISHED_VERSION_DATE_PROPERTY))
            .thenReturn(new Date(1234));
        when(fileChangeObj2.getStringValue(FileChangeXClassInitializer.PREVIOUS_VERSION_PROPERTY))
            .thenReturn("filechange-3.1");
        when(fileChangeObj2.getStringValue(FileChangeXClassInitializer.VERSION_PROPERTY))
            .thenReturn("filechange-3.2");
        when(fileChangeObj2.getStringValue(FileChangeXClassInitializer.REFERENCE_LOCALE_PROPERTY))
            .thenReturn(Locale.GERMAN.toString());
        when(fileChangeObj2.getStringValue(FileChangeXClassInitializer.TYPE_PROPERTY))
            .thenReturn("edition");
        when(fileChangeObj2.getDateValue(FileChangeXClassInitializer.CREATION_DATE_PROPERTY))
            .thenReturn(new Date(4343));
        when(fileChangeObj2.getStringValue(FileChangeXClassInitializer.AUTHOR_PROPERTY))
            .thenReturn("xwiki:XWiki.Foo");

        when(fileChangeObj3.getStringValue(FileChangeXClassInitializer.REFERENCE_PROPERTY))
            .thenReturn(serializedTargetEntity);
        when(fileChangeObj3.getStringValue(FileChangeXClassInitializer.FILENAME_PROPERTY))
            .thenReturn(filename3);
        when(fileChangeObj3.getStringValue(FileChangeXClassInitializer.PREVIOUS_PUBLISHED_VERSION_PROPERTY))
            .thenReturn("3.3");
        when(fileChangeObj3.getDateValue(FileChangeXClassInitializer.PREVIOUS_PUBLISHED_VERSION_DATE_PROPERTY))
            .thenReturn(new Date(48));
        when(fileChangeObj3.getStringValue(FileChangeXClassInitializer.PREVIOUS_VERSION_PROPERTY))
            .thenReturn("filechange-3.2");
        when(fileChangeObj3.getStringValue(FileChangeXClassInitializer.VERSION_PROPERTY))
            .thenReturn("filechange-3.3");
        when(fileChangeObj3.getStringValue(FileChangeXClassInitializer.REFERENCE_LOCALE_PROPERTY))
            .thenReturn(Locale.GERMAN.toString());
        when(fileChangeObj3.getStringValue(FileChangeXClassInitializer.TYPE_PROPERTY))
            .thenReturn("deletion");
        when(fileChangeObj3.getDateValue(FileChangeXClassInitializer.CREATION_DATE_PROPERTY))
            .thenReturn(new Date(4444));
        when(fileChangeObj3.getStringValue(FileChangeXClassInitializer.AUTHOR_PROPERTY))
            .thenReturn("xwiki:XWiki.Bar");

        XWikiAttachment attachment1 = mock(XWikiAttachment.class);
        XWikiAttachment attachment2 = mock(XWikiAttachment.class);

        when(fileStorageDoc.getAttachment(filename1)).thenReturn(attachment1);
        when(fileStorageDoc.getAttachment(filename2)).thenReturn(attachment2);

        when(attachment1.getContentInputStream(this.context))
            .thenReturn(getClass().getClassLoader().getResourceAsStream("filechange.xml"));
        when(attachment2.getContentInputStream(this.context))
            .thenReturn(getClass().getClassLoader().getResourceAsStream("filechange2.xml"));

        UserReference authorReference1 = mock(UserReference.class);
        when(this.stringUserReferenceResolver.resolve("xwiki:XWiki.surli")).thenReturn(authorReference1);

        UserReference authorReference2 = mock(UserReference.class);
        when(this.stringUserReferenceResolver.resolve("xwiki:XWiki.Foo")).thenReturn(authorReference2);

        UserReference authorReference3 = mock(UserReference.class);
        when(this.stringUserReferenceResolver.resolve("xwiki:XWiki.Bar")).thenReturn(authorReference3);

        FileChange expected1 = new FileChange(changeRequest)
            .setCreationDate(new Date(4242))
            .setAuthor(authorReference1)
            .setSaved(true)
            .setId("file1")
            .setTargetEntity(targetEntity)
            .setPreviousVersion("2.3")
            .setPreviousPublishedVersion("2.3", new Date(666))
            .setVersion("filechange-3.1");

        FileChange expected2 = new FileChange(changeRequest)
            .setCreationDate(new Date(4343))
            .setAuthor(authorReference2)
            .setSaved(true)
            .setId("file2")
            .setTargetEntity(targetEntity)
            .setPreviousVersion("filechange-3.1")
            .setPreviousPublishedVersion("2.3", new Date(1234))
            .setVersion("filechange-3.2");

        FileChange expected3 = new FileChange(changeRequest, FileChange.FileChangeType.DELETION)
            .setCreationDate(new Date(4444))
            .setAuthor(authorReference3)
            .setSaved(true)
            .setId("file3")
            .setTargetEntity(targetEntity)
            .setPreviousVersion("filechange-3.2")
            .setPreviousPublishedVersion("3.3", new Date(48))
            .setVersion("filechange-3.3");

        ArrayList<FileChange> expected = new ArrayList<>(Arrays.asList(expected1, expected2, expected3));

        List<FileChange> fileChanges = this.fileChangeStorageManager.load(changeRequest, changedDocument);
        assertEquals(3, fileChanges.size());

        expected1.setModifiedDocument(fileChanges.get(0).getModifiedDocument());
        expected2.setModifiedDocument(fileChanges.get(1).getModifiedDocument());
        assertEquals(expected, fileChanges);
    }

    @Test
    void mergeEdition() throws Exception
    {
        FileChange fileChange = mock(FileChange.class);
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        when(fileChange.getChangeRequest()).thenReturn(changeRequest);
        when(fileChange.toString()).thenReturn("my filechange");
        when(fileChange.getType()).thenReturn(FileChange.FileChangeType.EDITION);
        XWikiDocument modifiedDocument = mock(XWikiDocument.class);
        when(fileChange.getModifiedDocument()).thenReturn(modifiedDocument);
        when(modifiedDocument.clone()).thenReturn(modifiedDocument);
        when(modifiedDocument.getRCSVersion()).thenReturn(new Version("2.1"));

        DocumentReference targetEntity = mock(DocumentReference.class);
        when(fileChange.getTargetEntity()).thenReturn(targetEntity);
        XWikiDocument previousDocument = mock(XWikiDocument.class);
        when(fileChange.getPreviousPublishedVersion()).thenReturn("1.3");
        when(this.documentRevisionProvider.getRevision(targetEntity, "1.3")).thenReturn(previousDocument);
        when(fileChange.getPreviousPublishedVersionDate()).thenReturn(new Date(42));
        when(previousDocument.getDate()).thenReturn(new Date(42));

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
        when(changeRequest.getStatus()).thenReturn(ChangeRequestStatus.READY_FOR_MERGING);
        ChangeRequestException changeRequestException =
            assertThrows(ChangeRequestException.class, () -> this.fileChangeStorageManager.merge(fileChange));
        assertEquals("Cannot merge the file change [my filechange] since it has conflicts.",
            changeRequestException.getMessage());
        verify(this.xWiki, never()).saveDocument(any(XWikiDocument.class), anyString(), any());

        when(mergeDocumentResult.hasConflicts()).thenReturn(false);
        when(mergeDocumentResult.isModified()).thenReturn(false);
        this.fileChangeStorageManager.merge(fileChange);
        verify(this.xWiki, never()).saveDocument(any(XWikiDocument.class), anyString(), any());

        when(mergeDocumentResult.isModified()).thenReturn(true);
        when(mergeDocumentResult.getMergeResult()).thenReturn(currentDocument);
        DocumentAuthors documentAuthors = mock(DocumentAuthors.class);
        when(currentDocument.getAuthors()).thenReturn(documentAuthors);

        UserReference author = mock(UserReference.class);
        when(fileChange.getAuthor()).thenReturn(author);
        String crTitle = "Some title";
        String crID = "someId";
        when(fileChange.getChangeRequest()).thenReturn(changeRequest);
        when(changeRequest.getId()).thenReturn(crID);
        when(changeRequest.getTitle()).thenReturn(crTitle);
        when(this.contextualLocalizationManager.getTranslationPlain("changerequest.save.comment", crTitle, crID))
            .thenReturn(SAVE_MESSAGE);

        this.fileChangeStorageManager.merge(fileChange);
        verify(this.xWiki).saveDocument(currentDocument, SAVE_MESSAGE, this.context);
        verify(documentAuthors).setOriginalMetadataAuthor(author);
    }

    @Test
    void mergeDeletion() throws ChangeRequestException, XWikiException
    {
        FileChange fileChange = mock(FileChange.class);
        when(fileChange.getType()).thenReturn(FileChange.FileChangeType.DELETION);
        DocumentReference targetEntity = mock(DocumentReference.class);
        when(fileChange.getTargetEntity()).thenReturn(targetEntity);

        XWikiDocument targetDoc = mock(XWikiDocument.class);
        when(this.xWiki.getDocument(targetEntity, this.context)).thenReturn(targetDoc);

        this.fileChangeStorageManager.merge(fileChange);
        verify(this.xWiki).deleteDocument(targetDoc, this.context);
    }

    @Test
    void mergeCreation() throws XWikiException, ChangeRequestException
    {
        FileChange fileChange = mock(FileChange.class);
        when(fileChange.getType()).thenReturn(FileChange.FileChangeType.CREATION);
        DocumentReference mergerReference = new DocumentReference("xwiki", "XWiki", "Merger");
        when(context.getUserReference()).thenReturn(mergerReference);

        XWikiDocument targetDoc = mock(XWikiDocument.class);
        when(fileChange.getModifiedDocument()).thenReturn(targetDoc);
        when(targetDoc.clone()).thenReturn(targetDoc);
        String crTitle = "Some title";
        String crID = "someId";
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        when(fileChange.getChangeRequest()).thenReturn(changeRequest);
        when(changeRequest.getId()).thenReturn(crID);
        when(changeRequest.getTitle()).thenReturn(crTitle);
        when(this.contextualLocalizationManager.getTranslationPlain("changerequest.save.comment", crTitle, crID))
            .thenReturn(SAVE_MESSAGE);

        this.fileChangeStorageManager.merge(fileChange);
        verify(targetDoc).clone();
        verify(targetDoc).setCreatorReference(mergerReference);
        verify(targetDoc).setVersion("1.1");
        verify(this.xWiki).saveDocument(targetDoc, SAVE_MESSAGE, this.context);
    }

    @Test
    void rebaseEdition() throws ChangeRequestException, XWikiException
    {
        FileChange fileChange = mock(FileChange.class);
        when(fileChange.getType()).thenReturn(FileChange.FileChangeType.EDITION);
        String fileChangeVersion = "filechange-3.2";
        when(fileChange.getVersion()).thenReturn(fileChangeVersion);
        String nextFileChangeVersion = "filechange-4.1";
        when(this.fileChangeVersionManager.getNextFileChangeVersion(fileChangeVersion, true))
            .thenReturn(nextFileChangeVersion);
        String fileChangePreviousPublishedVersion = "3.2";
        when(fileChange.getPreviousPublishedVersion()).thenReturn(fileChangePreviousPublishedVersion);

        DocumentReference contextUserReference = new DocumentReference("xwiki", "XWiki", "User");
        when(this.context.getUserReference()).thenReturn(contextUserReference);

        DocumentReference documentReference = new DocumentReference("xwiki", "Target", "Entity");
        when(fileChange.getTargetEntity()).thenReturn(documentReference);
        XWikiDocument currentDocument = mock(XWikiDocument.class);
        when(this.xWiki.getDocument(documentReference, this.context)).thenReturn(currentDocument);

        // When we rebase but the document has been deleted from the wiki in the meantime, so it's now a creation.
        when(currentDocument.isNew()).thenReturn(true);
        FileChange cloneFileChangeCreation = mock(FileChange.class);
        when(fileChange.cloneWithType(FileChange.FileChangeType.CREATION)).thenReturn(cloneFileChangeCreation);
        // Not the supposed behaviour, but it's easier to test that way.
        when(cloneFileChangeCreation.isSaved()).thenReturn(true);

        this.fileChangeStorageManager.rebase(fileChange);
        verify(cloneFileChangeCreation).setPreviousVersion(fileChangeVersion);
        verify(cloneFileChangeCreation).setPreviousPublishedVersion(eq("1.1"), any());
        verify(cloneFileChangeCreation).setVersion(nextFileChangeVersion);
        verify(cloneFileChangeCreation).isSaved();

        // When we rebase but the document has been updated in the meantime, so we need to perform a merge.
        when(currentDocument.isNew()).thenReturn(false);
        FileChange cloneFileChange = mock(FileChange.class);
        when(fileChange.clone()).thenReturn(cloneFileChange);
        when(cloneFileChange.getChangeRequest()).thenReturn(mock(ChangeRequest.class));
        when(cloneFileChange.getTargetEntity()).thenReturn(documentReference);

        XWikiDocument previousDoc = mock(XWikiDocument.class);
        XWikiDocument modifiedDoc = mock(XWikiDocument.class);
        when(modifiedDoc.getRCSVersion()).thenReturn(new Version("3.4"));
        when(cloneFileChange.getModifiedDocument()).thenReturn(modifiedDoc);
        when(modifiedDoc.clone()).thenReturn(modifiedDoc);
        when(cloneFileChange.getPreviousPublishedVersion()).thenReturn(fileChangePreviousPublishedVersion);
        when(this.documentRevisionProvider.getRevision(documentReference, fileChangePreviousPublishedVersion))
            .thenReturn(previousDoc);
        when(previousDoc.getDate()).thenReturn(new Date(45));
        when(cloneFileChange.getPreviousPublishedVersionDate()).thenReturn(new Date(45));

        MergeDocumentResult mergeDocumentResult = mock(MergeDocumentResult.class);
        when(this.mergeManager.mergeDocument(eq(previousDoc), eq(modifiedDoc), eq(currentDocument), any()))
            .then(invocationOnMock -> {
            MergeConfiguration mergeConfiguration = invocationOnMock.getArgument(3);
            assertEquals(contextUserReference, mergeConfiguration.getUserReference());
            assertFalse(mergeConfiguration.isProvidedVersionsModifiables());
            assertEquals(documentReference, mergeConfiguration.getConcernedDocument());
            return mergeDocumentResult;
        });
        XWikiDocument mergeDocument = mock(XWikiDocument.class);
        when(mergeDocumentResult.getMergeResult()).thenReturn(mergeDocument);
        when(currentDocument.getVersion()).thenReturn("3.8");
        when(currentDocument.getDate()).thenReturn(new Date(485));
        // Not the supposed behaviour, but it's easier to test that way.
        when(cloneFileChange.isSaved()).thenReturn(true);

        this.fileChangeStorageManager.rebase(fileChange);
        verify(mergeDocumentResult).hasConflicts();
        verify(cloneFileChange).setModifiedDocument(mergeDocument);
        verify(cloneFileChange).setPreviousPublishedVersion("3.8", new Date(485));
        verify(cloneFileChange).setPreviousVersion(fileChangeVersion);
        verify(cloneFileChange).setVersion(nextFileChangeVersion);
        verify(cloneFileChange).isSaved();
        verify(modifiedDoc).clone();
    }

    @Test
    void rebaseCreation() throws ChangeRequestException, XWikiException
    {
        FileChange fileChange = mock(FileChange.class);
        when(fileChange.getType()).thenReturn(FileChange.FileChangeType.CREATION);
        String fileChangeVersion = "filechange-3.2";
        when(fileChange.getVersion()).thenReturn(fileChangeVersion);
        String nextFileChangeVersion = "filechange-4.1";
        when(this.fileChangeVersionManager.getNextFileChangeVersion(fileChangeVersion, true))
            .thenReturn(nextFileChangeVersion);
        String fileChangePreviousPublishedVersion = "3.2";
        when(fileChange.getPreviousPublishedVersion()).thenReturn(fileChangePreviousPublishedVersion);

        DocumentReference contextUserReference = new DocumentReference("xwiki", "XWiki", "User");
        when(this.context.getUserReference()).thenReturn(contextUserReference);

        DocumentReference documentReference = new DocumentReference("xwiki", "Target", "Entity");
        when(fileChange.getTargetEntity()).thenReturn(documentReference);
        XWikiDocument currentDocument = mock(XWikiDocument.class);
        when(this.xWiki.getDocument(documentReference, this.context)).thenReturn(currentDocument);

        when(currentDocument.isNew()).thenReturn(true);
        FileChange cloneFileChangeCreation = mock(FileChange.class);
        when(fileChange.clone()).thenReturn(cloneFileChangeCreation);
        // Not the supposed behaviour, but it's easier to test that way.
        when(cloneFileChangeCreation.isSaved()).thenReturn(true);

        this.fileChangeStorageManager.rebase(fileChange);
        verify(cloneFileChangeCreation).setPreviousVersion(fileChangeVersion);
        verify(cloneFileChangeCreation).setVersion(nextFileChangeVersion);
        verify(cloneFileChangeCreation).isSaved();

        // When we rebase but the document has been created in the meantime, so we need to perform a merge.
        when(currentDocument.isNew()).thenReturn(false);
        FileChange cloneFileChange = mock(FileChange.class);
        when(fileChange.cloneWithType(FileChange.FileChangeType.EDITION)).thenReturn(cloneFileChange);
        when(cloneFileChange.getChangeRequest()).thenReturn(mock(ChangeRequest.class));
        when(cloneFileChange.getTargetEntity()).thenReturn(documentReference);

        XWikiDocument previousDoc = mock(XWikiDocument.class);
        XWikiDocument modifiedDoc = mock(XWikiDocument.class);
        when(modifiedDoc.getRCSVersion()).thenReturn(new Version("3.4"));
        when(cloneFileChange.getModifiedDocument()).thenReturn(modifiedDoc);
        when(modifiedDoc.clone()).thenReturn(modifiedDoc);
        when(cloneFileChange.getPreviousPublishedVersion()).thenReturn(fileChangePreviousPublishedVersion);
        when(this.documentRevisionProvider.getRevision(documentReference, fileChangePreviousPublishedVersion))
            .thenReturn(previousDoc);
        when(previousDoc.getDate()).thenReturn(new Date(45));
        when(cloneFileChange.getPreviousPublishedVersionDate()).thenReturn(new Date(45));

        MergeDocumentResult mergeDocumentResult = mock(MergeDocumentResult.class);
        when(this.mergeManager.mergeDocument(eq(previousDoc), eq(modifiedDoc), eq(currentDocument), any()))
            .then(invocationOnMock -> {
                MergeConfiguration mergeConfiguration = invocationOnMock.getArgument(3);
                assertEquals(contextUserReference, mergeConfiguration.getUserReference());
                assertFalse(mergeConfiguration.isProvidedVersionsModifiables());
                assertEquals(documentReference, mergeConfiguration.getConcernedDocument());
                return mergeDocumentResult;
            });
        XWikiDocument mergeDocument = mock(XWikiDocument.class);
        when(mergeDocumentResult.getMergeResult()).thenReturn(mergeDocument);
        when(currentDocument.getVersion()).thenReturn("3.8");
        when(currentDocument.getDate()).thenReturn(new Date(485));
        // Not the supposed behaviour, but it's easier to test that way.
        when(cloneFileChange.isSaved()).thenReturn(true);

        this.fileChangeStorageManager.rebase(fileChange);
        verify(mergeDocumentResult).hasConflicts();
        verify(cloneFileChange).setModifiedDocument(mergeDocument);
        verify(cloneFileChange).setPreviousPublishedVersion("3.8", new Date(485));
        verify(cloneFileChange).setPreviousVersion(fileChangeVersion);
        verify(cloneFileChange).setVersion(nextFileChangeVersion);
        verify(cloneFileChange).isSaved();
        verify(modifiedDoc).clone();
    }

    @Test
    void rebaseDeletion() throws XWikiException, ChangeRequestException
    {
        FileChange fileChange = mock(FileChange.class);
        when(fileChange.getType()).thenReturn(FileChange.FileChangeType.DELETION);
        String fileChangeVersion = "filechange-3.2";
        when(fileChange.getVersion()).thenReturn(fileChangeVersion);
        String nextFileChangeVersion = "filechange-4.1";
        when(this.fileChangeVersionManager.getNextFileChangeVersion(fileChangeVersion, true))
            .thenReturn(nextFileChangeVersion);
        String fileChangePreviousPublishedVersion = "3.2";
        when(fileChange.getPreviousPublishedVersion()).thenReturn(fileChangePreviousPublishedVersion);

        DocumentReference documentReference = new DocumentReference("xwiki", "Target", "Entity");
        when(fileChange.getTargetEntity()).thenReturn(documentReference);
        XWikiDocument currentDocument = mock(XWikiDocument.class);
        when(this.xWiki.getDocument(documentReference, this.context)).thenReturn(currentDocument);
        when(currentDocument.getVersion()).thenReturn("4.8");
        when(currentDocument.getDate()).thenReturn(new Date(888));

        when(currentDocument.isNew()).thenReturn(false);
        FileChange cloneFileChange = mock(FileChange.class);
        when(fileChange.clone()).thenReturn(cloneFileChange);
        // Not the supposed behaviour, but it's easier to test that way.
        when(cloneFileChange.isSaved()).thenReturn(true);
        this.fileChangeStorageManager.rebase(fileChange);

        verify(cloneFileChange).setPreviousPublishedVersion("4.8", new Date(888));
        verify(cloneFileChange).setPreviousVersion(fileChangeVersion);
        verify(cloneFileChange).setVersion(nextFileChangeVersion);
        verify(cloneFileChange).isSaved();

        when(currentDocument.isNew()).thenReturn(true);
        FileChange cloneNoChange = mock(FileChange.class);
        when(fileChange.cloneWithType(FileChange.FileChangeType.NO_CHANGE)).thenReturn(cloneNoChange);
        // Not the supposed behaviour, but it's easier to test that way.
        when(cloneNoChange.isSaved()).thenReturn(true);
        this.fileChangeStorageManager.rebase(fileChange);
        verify(cloneNoChange).setPreviousVersion(fileChangeVersion);
        verify(cloneNoChange).setVersion(nextFileChangeVersion);
        verify(cloneNoChange).isSaved();
    }
}
