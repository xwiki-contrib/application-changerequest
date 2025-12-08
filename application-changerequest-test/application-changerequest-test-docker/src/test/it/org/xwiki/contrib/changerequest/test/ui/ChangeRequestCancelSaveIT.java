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

import java.util.List;

import org.apache.commons.httpclient.HttpStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.xwiki.contrib.changerequest.test.po.ChangeRequestPage;
import org.xwiki.contrib.changerequest.test.po.ChangeRequestSaveModal;
import org.xwiki.contrib.changerequest.test.po.ExtendedEditPage;
import org.xwiki.contrib.changerequest.test.po.filechanges.FileChangesPane;
import org.xwiki.contrib.changerequest.test.po.filechanges.FilechangesLiveDataElement;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rest.model.jaxb.Object;
import org.xwiki.rest.model.jaxb.Objects;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.WikiEditPage;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Dedicated test for checking behaviour of cancelling a filechange save.
 *
 * @version $Id$
 * @since 1.21
 */
@UITest(
    properties = {
        "xwikiDbHbmCommonExtraMappings=notification-filter-preferences.hbm.xml"
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
public class ChangeRequestCancelSaveIT
{
    private static final DocumentReference LISTENER_REFERENCE = new DocumentReference("xwiki", "CR",
        "CancelSaveListener");

    @BeforeAll
    void beforeAll(TestUtils setup) throws Exception
    {
        setup.loginAsSuperAdmin();

        // Prepare code of a dedicated listener for cancelling save
        Page page = setup.rest().page(LISTENER_REFERENCE);
        page.setObjects(new Objects());

        Object componentObject = setup.rest().object(LISTENER_REFERENCE, "XWiki.ComponentClass");
        componentObject.getProperties().addAll(List.of(
            TestUtils.RestTestUtils.property("roleType", "org.xwiki.observation.EventListener"),
            TestUtils.RestTestUtils.property("roleHint", "testCRListener"),
            TestUtils.RestTestUtils.property("scope", "wiki")
        ));
        page.getObjects().getObjectSummaries().add(componentObject);

        Object componentMethodObject1 = setup.rest().object(LISTENER_REFERENCE, "XWiki.ComponentMethodClass", 0);
        componentMethodObject1.getProperties().addAll(List.of(
            TestUtils.RestTestUtils.property("name", "getEvents"),
            TestUtils.RestTestUtils.property("code", "{{groovy}}\n"
                + "xcontext.method.output.value = [new org.xwiki.contrib.changerequest.events."
                + "FileChangeDocumentSavingEvent()]\n"
                + "{{/groovy}}")
        ));
        page.getObjects().getObjectSummaries().add(componentMethodObject1);

        Object componentMethodObject2 = setup.rest().object(LISTENER_REFERENCE, "XWiki.ComponentMethodClass", 1);
        componentMethodObject2.getProperties().addAll(List.of(
            TestUtils.RestTestUtils.property("name", "getName"),
            TestUtils.RestTestUtils.property("code", "{{groovy}}\n"
                + "xcontext.method.output.value = \"testListener\"\n"
                + "{{/groovy}}")
        ));

        page.getObjects().getObjectSummaries().add(componentMethodObject2);

        Object componentMethodObject3 = setup.rest().object(LISTENER_REFERENCE, "XWiki.ComponentMethodClass", 2);
        componentMethodObject3.getProperties().addAll(List.of(
            TestUtils.RestTestUtils.property("name", "onEvent"),
            TestUtils.RestTestUtils.property("code", "{{groovy}}\n"
                + "import org.xwiki.contrib.changerequest.FileChange;\n"
                + "def event = xcontext.method.input.get(0)\n"
                + "def fileChange = xcontext.method.input.get(1)\n"
                + "if (!fileChange.isMinorChange() "
                + "&& fileChange.getTargetEntity().toString().contains(\"ChangeRequestCancelSaveIT\")) {\n"
                + "  event.cancel(\"Cancelled by custom CR listener: use minor change to accept it.\");\n"
                + "}\n"
                + "{{/groovy}}")
        ));
        page.getObjects().getObjectSummaries().add(componentMethodObject3);
        page.setTitle("Cancellation save listener");
        setup.rest().save(page, HttpStatus.SC_CREATED);
    }

    @AfterAll
    void afterAll(TestUtils setup) throws Exception
    {
        setup.loginAsSuperAdmin();
        // Clean up the page (note: apparently it's not enough to unregister the listener for other test, hence the
        // entity check in the listener)
        setup.deletePage(LISTENER_REFERENCE);
    }

    @Test
    void createPageAndCancelSave(TestUtils setup, TestReference testReference)
    {
        setup.loginAsSuperAdmin();
        setup.createPage(testReference, "Anything");

        ViewPage viewPage = setup.gotoPage(testReference);
        viewPage.editWiki();
        ExtendedEditPage<WikiEditPage> extendedEditPage = new ExtendedEditPage<>(new WikiEditPage());
        extendedEditPage.getWrappedEditor().setContent("1. Creation");
        ChangeRequestSaveModal changeRequestSaveModal = extendedEditPage.clickSaveAsChangeRequest();
        changeRequestSaveModal.setChangeRequestTitle("CR1");
        changeRequestSaveModal.clickSaveExpectFailure();
        extendedEditPage.waitForNotificationErrorMessage("An error occured while saving: Saving of the filechange has"
            + " been cancelled with following reason: [Cancelled by custom CR listener: use minor change to accept it.]"
            + ".");
        changeRequestSaveModal.close();
        extendedEditPage.getWrappedEditor().setMinorEdit(true);
        changeRequestSaveModal = extendedEditPage.clickSaveAsChangeRequest();
        changeRequestSaveModal.setChangeRequestTitle("CR1");

        ChangeRequestPage changeRequestPage = changeRequestSaveModal.clickSave();

        FileChangesPane fileChangesPane = changeRequestPage.openFileChanges();
        List<FilechangesLiveDataElement.FilechangesRowElement> fileChanges =
            fileChangesPane.getFileChangesListLiveData().getFileChanges();
        assertEquals(1, fileChanges.size());
        extendedEditPage = fileChanges.get(0).clickEdit();

        assertEquals("1. Creation", extendedEditPage.getWrappedEditor().getContent());
        extendedEditPage.getWrappedEditor().setMinorEdit(false);
        extendedEditPage.getWrappedEditor().setContent("1. Creation\n2. Edition");
        extendedEditPage.clickSaveAsChangeRequestInExistingCR(false);
        extendedEditPage.waitForNotificationErrorMessage("An error occured while saving: Saving of the filechange has"
            + " been cancelled with following reason: [Cancelled by custom CR listener: use minor change to accept it.]"
            + ".");
        extendedEditPage.getWrappedEditor().setContent("1. Creation\n2. Edition\n3.Other edition");
        extendedEditPage.getWrappedEditor().setMinorEdit(true);

        changeRequestPage = extendedEditPage.clickSaveAsChangeRequestInExistingCR(true);
        fileChangesPane = changeRequestPage.openFileChanges();
        fileChanges = fileChangesPane.getFileChangesListLiveData().getFileChanges();
        assertEquals(1, fileChanges.size());
        extendedEditPage = fileChanges.get(0).clickEdit();

        assertEquals("1. Creation\n2. Edition\n3.Other edition", extendedEditPage.getWrappedEditor().getContent());
    }
}
