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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.contrib.changerequest.test.po.ChangeRequestPage;
import org.xwiki.contrib.changerequest.test.po.ChangeRequestSaveModal;
import org.xwiki.contrib.changerequest.test.po.ExtendedDeleteConfirmationPage;
import org.xwiki.contrib.changerequest.test.po.ExtendedViewPage;
import org.xwiki.contrib.changerequest.test.po.FileChangesPane;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests related to the rebase and conflicts handling operations.
 *
 * @version $Id$
 * @since 0.10
 */
@UITest(
    properties = {
        "xwikiDbHbmCommonExtraMappings=notification-filter-preferences.hbm.xml"
    },
    extraJARs = {
        // It's currently not possible to install a JAR contributing a Hibernate mapping file as an Extension. Thus
        // we need to provide the JAR inside WEB-INF/lib. See https://jira.xwiki.org/browse/XWIKI-8271
        "org.xwiki.platform:xwiki-platform-notifications-filters-default",
        // It's currently not possible to install a JAR contributing a Hibernate mapping file as an Extension. Thus
        // we need to provide the JAR inside WEB-INF/lib. See https://jira.xwiki.org/browse/XWIKI-8271
        "org.xwiki.platform:xwiki-platform-eventstream-store-hibernate",
        // The Solr store is not ready yet to be installed as extension
        "org.xwiki.platform:xwiki-platform-eventstream-store-solr"
    },
    resolveExtraJARs = true
)
class ChangeRequestConflictsIT
{
    private static final String CR_USER = "CRConflictTest";

    @BeforeAll
    void beforeAll(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
        setup.createUser(CR_USER, CR_USER, null);
        setup.setGlobalRights("", CR_USER, "edit", false);
        setup.setGlobalRights("", CR_USER, "delete", false);
    }

    @Test
    @Order(1)
    void deleteConflict(TestUtils testUtils, TestReference testReference) throws Exception
    {
        String serializedReference = testReference.getLocalDocumentReference().toString();
        testUtils.createPage(testReference, "Some content");
        testUtils.login(CR_USER, CR_USER);
        testUtils.gotoPage(testReference);
        ExtendedViewPage extendedViewPage = new ExtendedViewPage();

        assertFalse(extendedViewPage.hasStandardDeleteMenuEntry());
        assertTrue(extendedViewPage.hasRequestForDeletionMenuEntry());

        ExtendedDeleteConfirmationPage extendedDeleteConfirmationPage = extendedViewPage.clickRequestForDeletion();
        assertFalse(extendedDeleteConfirmationPage.hasDeleteButton());
        assertTrue(extendedDeleteConfirmationPage.hasChangeRequestDeleteButton());

        ChangeRequestSaveModal saveModal = extendedDeleteConfirmationPage.clickChangeRequestDelete();
        saveModal.setChangeRequestTitle("CRDelete");
        ChangeRequestPage changeRequestPage = saveModal.clickSave();
        String pageURL = changeRequestPage.getPageURL();

        FileChangesPane fileChangesPane = changeRequestPage.openFileChanges();
        assertEquals(FileChangesPane.ChangeType.DELETION, fileChangesPane.getChangeType(serializedReference));
        assertFalse(fileChangesPane.isDiffOutdated(serializedReference));
        assertFalse(fileChangesPane.isConflictLabelDisplayed(serializedReference));
        assertFalse(fileChangesPane.isRefreshActionAvailable(serializedReference));
        assertFalse(fileChangesPane.isFixConflictActionAvailable(serializedReference));

        testUtils.loginAsSuperAdmin();
        testUtils.gotoPage(testReference);
        testUtils.rest().savePage(testReference, "Some new content", "New title");
        testUtils.login(CR_USER, CR_USER);
        testUtils.gotoPage(pageURL);

        changeRequestPage = new ChangeRequestPage();
        fileChangesPane = changeRequestPage.openFileChanges();
        assertEquals(FileChangesPane.ChangeType.DELETION, fileChangesPane.getChangeType(serializedReference));
        assertTrue(fileChangesPane.isDiffOutdated(serializedReference));
        assertFalse(fileChangesPane.isConflictLabelDisplayed(serializedReference));
        assertTrue(fileChangesPane.isRefreshActionAvailable(serializedReference));
        assertFalse(fileChangesPane.isFixConflictActionAvailable(serializedReference));

        changeRequestPage = fileChangesPane.clickRefresh(serializedReference);
        fileChangesPane = changeRequestPage.openFileChanges();
        assertEquals(FileChangesPane.ChangeType.DELETION, fileChangesPane.getChangeType(serializedReference));
        assertFalse(fileChangesPane.isDiffOutdated(serializedReference));
        assertFalse(fileChangesPane.isConflictLabelDisplayed(serializedReference));
        assertFalse(fileChangesPane.isRefreshActionAvailable(serializedReference));
        assertFalse(fileChangesPane.isFixConflictActionAvailable(serializedReference));

        testUtils.loginAsSuperAdmin();
        testUtils.gotoPage(testReference);
        testUtils.rest().delete(testReference);

        testUtils.login(CR_USER, CR_USER);
        testUtils.gotoPage(pageURL);
        changeRequestPage = new ChangeRequestPage();
        fileChangesPane = changeRequestPage.openFileChanges();
        assertEquals(FileChangesPane.ChangeType.DELETION, fileChangesPane.getChangeType(serializedReference));
        assertTrue(fileChangesPane.isDiffOutdated(serializedReference));
        assertFalse(fileChangesPane.isConflictLabelDisplayed(serializedReference));
        assertTrue(fileChangesPane.isRefreshActionAvailable(serializedReference));
        assertFalse(fileChangesPane.isFixConflictActionAvailable(serializedReference));

        changeRequestPage = fileChangesPane.clickRefresh(serializedReference);
        fileChangesPane = changeRequestPage.openFileChanges();
        assertEquals(FileChangesPane.ChangeType.NO_CHANGE, fileChangesPane.getChangeType(serializedReference));
        assertFalse(fileChangesPane.isDiffOutdated(serializedReference));
        assertFalse(fileChangesPane.isConflictLabelDisplayed(serializedReference));
        assertFalse(fileChangesPane.isRefreshActionAvailable(serializedReference));
        assertFalse(fileChangesPane.isFixConflictActionAvailable(serializedReference));
    }
}
