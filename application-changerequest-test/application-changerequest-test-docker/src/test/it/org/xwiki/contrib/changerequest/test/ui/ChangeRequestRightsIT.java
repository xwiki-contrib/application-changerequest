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

import java.util.Arrays;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.contrib.changerequest.test.po.ExtendedEditPage;
import org.xwiki.contrib.changerequest.test.po.ExtendedViewPage;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.editor.WYSIWYGEditPage;

import liquibase.pro.packaged.D;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UI tests for checking UI behaviour with CR-related rights.
 *
 * @version $Id$
 * @since 0.9
 */
@UITest
class ChangeRequestRightsIT
{
    @BeforeAll
    void setup(TestUtils testUtils)
    {
        // generic fixture:
        // CR strategy: acceptall
        // so that we don't need to review CR to check the rights for merging
        testUtils.loginAsSuperAdmin();
        testUtils.updateObject(Arrays.asList("ChangeRequest", "Code"), "Configuration",
            "ChangeRequest.Code.ConfigurationClass", 0,
            "minimumApprovers", 0,
            "approvalStrategy","acceptall");
    }

    @Test
    @Order(1)
    void checkCRUserRights(TestUtils testUtils)
    {
        // Fixture:
        // User: CRUser
        // Rights at wiki level
        // Edit right: denied for CRUser
        // Create CR right: not set (allowed by default)
        // Approve CR right: not set (denied by default)
        testUtils.createUser("CRUser", "CRUser", null);
        testUtils.setGlobalRights("", "CRUser", "edit", false);

        DocumentReference noCRPage = new DocumentReference("xwiki", "NoCRInPage", "WebHome");

        // Create a page and deny change request right in that space
        testUtils.createPage(noCRPage, "There won't be any CR done for pages in there.", "");
        testUtils.setRights(noCRPage, "", "CRUser", "changerequest", false);

        testUtils.login("CRUser", "CRUser");

        // go to a standard page: editing with CR should be enabled
        testUtils.gotoPage("Some", "Page");
        ExtendedViewPage extendedViewPage = new ExtendedViewPage();
        assertTrue(extendedViewPage.hasChangeRequestEditButton());
        assertFalse(extendedViewPage.hasStandardEditButton());
        ExtendedEditPage<WYSIWYGEditPage> editPage = extendedViewPage.clickChangeRequestEdit();
        assertTrue(editPage.hasSaveAsChangeRequestButton());
        assertFalse(editPage.hasStandardSaveButton());

        // go to the page where CR is denied
        testUtils.gotoPage(noCRPage);
        extendedViewPage = new ExtendedViewPage();
        assertFalse(extendedViewPage.hasChangeRequestEditButton());
        assertFalse(extendedViewPage.hasStandardEditButton());

        // Create an editor user
        // Edit rights: allowed at wiki level
        // Create CR Rights: denied also for the NoCRInPage page
        testUtils.loginAsSuperAdmin();
        testUtils.createUser("EditorUser", "EditorUser", null);
        testUtils.setGlobalRights("", "EditorUser", "edit", true);
        testUtils.setRights(noCRPage, "", "EditorUser", "changerequest", false);

        // remove lock on the page
        testUtils.gotoPage(noCRPage);

        testUtils.login("EditorUser", "EditorUser");
        testUtils.gotoPage("Some", "Page");
        extendedViewPage = new ExtendedViewPage();
        assertFalse(extendedViewPage.hasChangeRequestEditButton());
        assertTrue(extendedViewPage.hasStandardEditButton());

        editPage = extendedViewPage.clickStandardEdit();
        assertTrue(editPage.hasSaveAsChangeRequestButton());
        assertTrue(editPage.hasStandardSaveButton());

        testUtils.gotoPage(noCRPage);
        extendedViewPage = new ExtendedViewPage();
        assertFalse(extendedViewPage.hasChangeRequestEditButton());
        assertTrue(extendedViewPage.hasStandardEditButton());

        editPage = extendedViewPage.clickStandardEdit();
        assertFalse(editPage.hasSaveAsChangeRequestButton());
        assertTrue(editPage.hasStandardSaveButton());
    }
}
