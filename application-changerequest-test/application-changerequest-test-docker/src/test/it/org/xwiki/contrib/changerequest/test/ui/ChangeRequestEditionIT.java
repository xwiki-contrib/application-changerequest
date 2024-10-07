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
package org.xwiki.contrib.changerequest.test.ui;

import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.ckeditor.test.po.CKEditor;
import org.xwiki.contrib.changerequest.test.po.ChangeRequestPage;
import org.xwiki.contrib.changerequest.test.po.ChangeRequestSaveModal;
import org.xwiki.contrib.changerequest.test.po.ExtendedEditPage;
import org.xwiki.contrib.changerequest.test.po.ExtendedViewPage;
import org.xwiki.contrib.changerequest.test.po.description.DescriptionPane;
import org.xwiki.contrib.changerequest.test.po.description.TimelineEvent;
import org.xwiki.contrib.changerequest.test.po.filechanges.FileChangesPane;
import org.xwiki.contrib.changerequest.test.po.filechanges.FilechangesLiveDataElement;
import org.xwiki.edit.test.po.InplaceEditablePage;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rest.model.jaxb.Object;
import org.xwiki.rest.model.jaxb.Objects;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.diff.DocumentDiffSummary;
import org.xwiki.test.ui.po.diff.EntityDiff;
import org.xwiki.test.ui.po.editor.ObjectEditPage;
import org.xwiki.test.ui.po.editor.ObjectEditPane;
import org.xwiki.test.ui.po.editor.WikiEditPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/***
 * Test performing editions on an already existing change request.
 *
 * @version $Id$
 * @since 1.4.1
 */
@UITest(
    properties = {
        "xwikiDbHbmCommonExtraMappings=notification-filter-preferences.hbm.xml",
        // See https://jira.xwiki.org/browse/XWIKI-22549
        "xwikiCfgPlugins=com.xpn.xwiki.plugin.skinx.JsResourceSkinExtensionPlugin"
    },
    extraJARs = {
        // It's currently not possible to install a JAR contributing a Hibernate mapping file as an Extension. Thus
        // we need to provide the JAR inside WEB-INF/lib. See https://jira.xwiki.org/browse/XWIKI-8271
        "org.xwiki.platform:xwiki-platform-notifications-filters-default",
        // The Solr store is not ready yet to be installed as extension
        "org.xwiki.platform:xwiki-platform-eventstream-store-solr"
    },
    resolveExtraJARs = true
)
public class ChangeRequestEditionIT
{
    private static final String CR_EDITOR = "ChangeRequestEditionIT_Editor";
    private static final String CR_EDITOR_WYSIWYG = "ChangeRequestEditionIT_EditorWysiwyg";

    @BeforeAll
    void beforeAll(TestUtils testUtils)
    {
        testUtils.loginAsSuperAdmin();
        testUtils.createUser(CR_EDITOR, CR_EDITOR, null);
        testUtils.updateObject("XWiki", CR_EDITOR, "XWiki.XWikiUsers", 0, "usertype", "Advanced");
        testUtils.createUser(CR_EDITOR_WYSIWYG, CR_EDITOR_WYSIWYG, null);
        testUtils.updateObject("XWiki", CR_EDITOR_WYSIWYG, "XWiki.XWikiUsers", 0, "editor", "Wysiwyg");
    }

    @Test
    @Order(1)
    void editDocument(TestUtils testUtils, TestReference testReference)
    {
        String serializedReference = testReference.getLocalDocumentReference().toString();
        testUtils.loginAsSuperAdmin();
        testUtils.createPage(testReference, "Some content to the test page.");

        testUtils.login(CR_EDITOR, CR_EDITOR);
        testUtils.gotoPage(testReference);
        ExtendedViewPage extendedViewPage = new ExtendedViewPage();

        ExtendedEditPage<WikiEditPage> extendedEditPage = extendedViewPage.clickStandardEdit();
        extendedEditPage.getWrappedEditor().setContent("Some new content.");
        ChangeRequestSaveModal saveModal = extendedEditPage.clickSaveAsChangeRequest();
        saveModal.setChangeRequestTitle("CR1_editDocument");
        ChangeRequestPage changeRequestPage = saveModal.clickSave();

        Date afterSave = new Date();

        FileChangesPane fileChangesPane = changeRequestPage.openFileChanges();

        DocumentDiffSummary diffSummary = fileChangesPane.getDiffSummary(serializedReference);
        assertEquals("(2 modified, 0 added, 0 removed)", diffSummary.getPagePropertiesSummary());

        EntityDiff contentDiff = fileChangesPane.getEntityDiff(serializedReference, "Page properties");
        List<String> content = contentDiff.getDiff("Content");
        assertEquals(3, content.size());
        assertEquals("-Some content<del> to the test page</del>.", content.get(1));
        assertEquals("+Some <ins>new </ins>content.", content.get(2));

        FilechangesLiveDataElement fileChangesListLiveData = fileChangesPane.getFileChangesListLiveData();
        List<FilechangesLiveDataElement.FilechangesRowElement> fileChanges = fileChangesListLiveData.getFileChanges();
        assertEquals(1, fileChanges.size());

        FilechangesLiveDataElement.FilechangesRowElement rowElement = fileChanges.get(0);
        assertEquals("filechange-2.1", rowElement.getVersion());
        assertEquals("1.1", rowElement.getPublishedDocumentVersion());
        assertTrue(rowElement.isEditActionAvailable());
        ExtendedEditPage<WikiEditPage> wikiEditPageExtendedEditPage = rowElement.clickEdit();
        assertEquals("Some new content.", wikiEditPageExtendedEditPage.getWrappedEditor().getContent());

        wikiEditPageExtendedEditPage.getWrappedEditor().setContent("Second edition");
        changeRequestPage = wikiEditPageExtendedEditPage.clickSaveAsChangeRequestInExistingCR();

        fileChangesPane = changeRequestPage.openFileChanges();

        diffSummary = fileChangesPane.getDiffSummary(serializedReference);
        assertEquals("(2 modified, 0 added, 0 removed)", diffSummary.getPagePropertiesSummary());

        fileChangesPane.getEntityDiff(serializedReference, "Page properties");
        contentDiff = fileChangesPane.getEntityDiff(serializedReference, "Page properties");
        content = contentDiff.getDiff("Content");
        assertEquals(3, content.size());
        assertEquals("-S<del>om</del>e<del> </del>con<del>tent</del> <del>to th</del>e<del> "
            + "</del>t<del>est page.</del>", content.get(1));
        assertEquals("+Secon<ins>d</ins> e<ins>di</ins>t<ins>ion</ins>", content.get(2));

        fileChangesListLiveData = fileChangesPane.getFileChangesListLiveData();
        fileChanges = fileChangesListLiveData.getFileChanges();
        assertEquals(1, fileChanges.size());

        rowElement = fileChanges.get(0);
        assertEquals("filechange-2.2", rowElement.getVersion());
        assertEquals("1.1", rowElement.getPublishedDocumentVersion());

        // Check event
        DescriptionPane descriptionPane = changeRequestPage.openDescription();
        descriptionPane.waitUntilEventsSize(2);

        Date dateAfterNewChange = new Date();
        List<TimelineEvent> events = descriptionPane.getEvents();
        assertEquals(2, events.size());

        TimelineEvent timelineEvent = events.get(1);
        assertTrue(timelineEvent.getDate().after(afterSave));
        assertTrue(timelineEvent.getDate().before(dateAfterNewChange));
        assertEquals(CR_EDITOR + " added a new change for "
                + "xwiki:" + serializedReference,
            timelineEvent.getContent().getText());
    }

    /**
     * This test aims at ensuring there's no regression after fixing CRAPP-224.
     * Note that we cannot make much check in this test as to reproduce the bug the UI must be in french...
     */
    @Test
    @Order(2)
    void editDocumentDifferentLocale(TestUtils testUtils, TestReference testReference) throws Exception
    {
        testUtils.loginAsSuperAdmin();
        testUtils.setWikiPreference("languages", "en,fr");
        testUtils.setWikiPreference("multilingual", "true");
        try {
            DocumentReference frDocReference = new DocumentReference(testReference, Locale.FRENCH);
            DocumentReference enDocReference = new DocumentReference(testReference, Locale.ENGLISH);
            String serializedReference = testReference.getLocalDocumentReference().toString();

            testUtils.createPage(enDocReference, "Some english content to the test page.");
            testUtils.createPage(frDocReference, "Some french content to the test page.");

            testUtils.login(CR_EDITOR, CR_EDITOR);
            testUtils.gotoPage(frDocReference);
            ExtendedViewPage extendedViewPage = new ExtendedViewPage();

            ExtendedEditPage<WikiEditPage> extendedEditPage = extendedViewPage.clickStandardEdit();
            assertEquals("Some french content to the test page.", extendedEditPage.getWrappedEditor().getContent());
            extendedEditPage.getWrappedEditor().setContent("Some new content.");
            ChangeRequestSaveModal saveModal = extendedEditPage.clickSaveAsChangeRequest();
            saveModal.setChangeRequestTitle("CR1_editDocumentwithLocale");
            ChangeRequestPage changeRequestPage = saveModal.clickSave();

            FileChangesPane fileChangesPane = changeRequestPage.openFileChanges();

            FilechangesLiveDataElement fileChangesListLiveData = fileChangesPane.getFileChangesListLiveData();
            List<FilechangesLiveDataElement.FilechangesRowElement> fileChanges =
                fileChangesListLiveData.getFileChanges();
            assertEquals(1, fileChanges.size());

            FilechangesLiveDataElement.FilechangesRowElement rowElement = fileChanges.get(0);
            assertTrue(rowElement.isEditActionAvailable());
            ExtendedEditPage<WikiEditPage> wikiEditPageExtendedEditPage = rowElement.clickEdit();
            assertEquals("Some new content.", wikiEditPageExtendedEditPage.getWrappedEditor().getContent());
        } finally {
            testUtils.loginAsSuperAdmin();
            testUtils.setWikiPreference("languages", "en");
            testUtils.setWikiPreference("multilingual", "false");
        }
    }

    /**
     * Check that inplace editing a page and saving a change request works.
     * This test aims at ensuring CRAPP-347 is properly fixed.
     */
    @Test
    @Order(3)
    void inplaceEditing(TestUtils testUtils, TestReference testReference)
    {
        String serializedReference = testReference.getLocalDocumentReference().toString();
        testUtils.loginAsSuperAdmin();
        testUtils.createPage(testReference, "Some content", "");

        testUtils.login(CR_EDITOR_WYSIWYG, CR_EDITOR_WYSIWYG);
        testUtils.gotoPage(testReference);
        InplaceEditablePage inplaceEditablePage = new InplaceEditablePage();
        inplaceEditablePage.editInplace();
        CKEditor editor = new CKEditor("content");
        ExtendedEditPage<CKEditor> extendedEditPage = new ExtendedEditPage<>(editor);
        inplaceEditablePage.setDocumentTitle("New title");
        ChangeRequestSaveModal changeRequestSaveModal = extendedEditPage.clickSaveAsChangeRequest();
        changeRequestSaveModal.setChangeRequestTitle("Test inplace editing");
        ChangeRequestPage changeRequestPage = changeRequestSaveModal.clickSave();

        FileChangesPane fileChangesPane = changeRequestPage.openFileChanges();

        DocumentDiffSummary diffSummary = fileChangesPane.getDiffSummary(serializedReference);
        assertEquals("(2 modified, 0 added, 0 removed)", diffSummary.getPagePropertiesSummary());

        EntityDiff contentDiff = fileChangesPane.getEntityDiff(serializedReference, "Page properties");
        List<String> content = contentDiff.getDiff("Title");
        assertEquals(2, content.size());
        assertEquals("+New title", content.get(1));
    }

    @Test
    @Order(4)
    void objectEditing(TestUtils testUtils, TestReference testReference) throws Exception
    {
        String serializedReference = testReference.getLocalDocumentReference().toString();
        testUtils.loginAsSuperAdmin();
        testUtils.createPage(testReference, "Some content", "");

        // Add an object
        Page testPage = testUtils.rest().page(testReference);
        testPage.setObjects(new Objects());
        Object styleSheetObject = testUtils.rest().object("XWiki.StyleSheetExtension");
        styleSheetObject.getProperties().add(testUtils.rest().property("code", ".content { color: black; }"));
        testPage.getObjects().getObjectSummaries().add(styleSheetObject);
        testUtils.rest().save(testPage);

        testUtils.login(CR_EDITOR, CR_EDITOR);
        ViewPage viewPage = testUtils.gotoPage(testReference);
        ObjectEditPage objectEditPage = viewPage.editObjects();
        List<ObjectEditPane> styleSheetObjects = objectEditPage.getObjectsOfClass("XWiki.StyleSheetExtension");
        assertEquals(1, styleSheetObjects.size());
        ObjectEditPane objectEditPane = styleSheetObjects.get(0);
        objectEditPane.displayObject();
        assertEquals(".content { color: black; }",
            objectEditPane.getFieldValue(objectEditPane.byPropertyName("code")));
        objectEditPane.setPropertyValue("code", ".content { color: red; }");
        ExtendedEditPage extendedEditPage = new ExtendedEditPage(objectEditPage);
        ChangeRequestSaveModal changeRequestSaveModal = extendedEditPage.clickSaveAsChangeRequest();
        changeRequestSaveModal.setChangeRequestTitle("Object test");
        ChangeRequestPage changeRequestPage = changeRequestSaveModal.clickSave();

        FileChangesPane fileChangesPane = changeRequestPage.openFileChanges();
        DocumentDiffSummary diffSummary = fileChangesPane.getDiffSummary(serializedReference);
        assertEquals("(1 modified, 0 added, 0 removed)", diffSummary.getObjectsSummary());

        EntityDiff contentDiff = fileChangesPane.getEntityDiff(serializedReference, "XWiki.StyleSheetExtension[0]");
        List<String> code = contentDiff.getDiff("Code");
        assertEquals(3, code.size());
        assertEquals("-.content { color: <del>black</del>; }", code.get(1));
        assertEquals("+.content { color: <ins>red</ins>; }", code.get(2));

        FilechangesLiveDataElement.FilechangesRowElement filechangesRowElement =
            fileChangesPane.getFileChangesListLiveData().getFileChangeWithReference(serializedReference);
        assertEquals("filechange-3.1", filechangesRowElement.getVersion());
        filechangesRowElement.clickEdit();
        String currentUrl = testUtils.getDriver().getCurrentUrl();
        // Force object editor
        // FIXME: Should be reachable from the UI
        testUtils.getDriver().get(currentUrl + "&editor=object");

        objectEditPage = new ObjectEditPage();
        styleSheetObjects = objectEditPage.getObjectsOfClass("XWiki.StyleSheetExtension");
        assertEquals(1, styleSheetObjects.size());
        objectEditPane = styleSheetObjects.get(0);
        objectEditPane.displayObject();
        assertEquals(".content { color: red; }",
            objectEditPane.getFieldValue(objectEditPane.byPropertyName("code")));
        objectEditPane.setPropertyValue("code", ".content { color: green; }");

        objectEditPane = objectEditPage.addObject("XWiki.StyleSheetExtension");
        objectEditPane.setPropertyValue("code", ".foo { color: yellow; }");

        extendedEditPage = new ExtendedEditPage(objectEditPage);
        changeRequestPage = extendedEditPage.clickSaveAsChangeRequestInExistingCR();

        fileChangesPane = changeRequestPage.openFileChanges();
        fileChangesPane.getFileChangesListLiveData().getFileChangeWithReference(serializedReference);
        assertEquals("filechange-3.2", filechangesRowElement.getVersion());
        diffSummary = fileChangesPane.getDiffSummary(serializedReference);
        assertEquals("(1 modified, 1 added, 0 removed)", diffSummary.getObjectsSummary());

        contentDiff = fileChangesPane.getEntityDiff(serializedReference, "XWiki.StyleSheetExtension[0]");
        code = contentDiff.getDiff("Code");
        assertEquals(3, code.size());
        assertEquals("-.content { color: <del>black</del>; }", code.get(1));
        assertEquals("+.content { color: <ins>green</ins>; }", code.get(2));

        contentDiff = fileChangesPane.getEntityDiff(serializedReference, "XWiki.StyleSheetExtension[1]");
        code = contentDiff.getDiff("Code");
        assertEquals(2, code.size());
        assertEquals("+.foo { color: yellow; }", code.get(1));
    }
}
