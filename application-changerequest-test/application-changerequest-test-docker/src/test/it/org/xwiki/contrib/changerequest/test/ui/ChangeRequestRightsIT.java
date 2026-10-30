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
import org.xwiki.contrib.changerequest.test.po.ChangeRequestPage;
import org.xwiki.contrib.changerequest.test.po.ChangeRequestSaveModal;
import org.xwiki.contrib.changerequest.test.po.ExtendedEditPage;
import org.xwiki.contrib.changerequest.test.po.ExtendedViewPage;
import org.xwiki.contrib.changerequest.test.po.description.DescriptionEditPage;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.editor.WikiEditPage;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
    private static final String TEST_USER_PREFIX = "ChangeRequestRightsIT";

    private static final String CR_USER = TEST_USER_PREFIX + "CRUser";
    private static final String EDITOR_USER = TEST_USER_PREFIX + "EditorUser";

    private static final String TITLE_ERROR =
        "The title of a change request cannot contain a word starting with # or $ such as #name or $name.";
    
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
            "mergeUser", "",
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
        testUtils.createUser(CR_USER, CR_USER, null, "usertype", "Advanced");
        testUtils.setGlobalRights("", CR_USER, "edit", false);

        DocumentReference noCRPage = new DocumentReference("xwiki", "NoCRInPage", "WebHome");

        // Create a page and deny change request right in that space
        testUtils.createPage(noCRPage, "There won't be any CR done for pages in there.", "");
        testUtils.setRights(noCRPage, "", CR_USER, "changerequest", false);

        testUtils.login(CR_USER, CR_USER);

        // go to a standard page: editing with CR should be enabled
        testUtils.gotoPage("Some", "Page");
        ExtendedViewPage extendedViewPage = new ExtendedViewPage();
        assertTrue(extendedViewPage.hasChangeRequestEditButton());
        assertFalse(extendedViewPage.hasStandardEditButton());
        ExtendedEditPage<WikiEditPage> editPage = extendedViewPage.clickChangeRequestEdit();
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
        testUtils.createUser(EDITOR_USER, EDITOR_USER, null, "usertype", "Advanced");
        testUtils.setGlobalRights("", EDITOR_USER, "edit", true);
        testUtils.setRights(noCRPage, "", EDITOR_USER, "changerequest", false);

        // remove lock on the page
        testUtils.gotoPage(noCRPage);

        testUtils.login(EDITOR_USER, EDITOR_USER);
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

    @Test
    @Order(2)
    void editChangeRequestTitleAndDescription(TestUtils testUtils, TestReference testReference)
    {
        // Fixture: a page that CRUser (without edit right) can only change through a change request.
        testUtils.loginAsSuperAdmin();
        testUtils.createPage(testReference, "Some content", "");

        testUtils.login(CR_USER, CR_USER);
        testUtils.gotoPage(testReference);
        ExtendedEditPage<WikiEditPage> editPage = new ExtendedViewPage().clickChangeRequestEdit();
        editPage.getWrappedEditor().setContent("Some other content");
        ChangeRequestSaveModal saveModal = editPage.clickSaveAsChangeRequest();

        // Titles that Velocity would interpret are refused.
        saveModal.setChangeRequestTitle("Title $name");
        assertThat(saveModal.clickSaveExpectOtherError(), containsString(TITLE_ERROR));
        saveModal.setChangeRequestTitle("Fix #123 for $5");
        ChangeRequestPage changeRequestPage = saveModal.clickSave();
        assertEquals("Fix #123 for $5", changeRequestPage.getChangeRequestTitle());

        // The description is displayed without executing scripts.
        DescriptionEditPage descriptionEditPage = changeRequestPage.openDescription().clickEditDescription();
        descriptionEditPage.setDescription("Some description {{velocity}}#set ($x = 'EXEC')${x}UTED{{/velocity}}");
        changeRequestPage = descriptionEditPage.saveDescription();
        String description = changeRequestPage.openDescription().getDescription();
        assertThat(description, containsString("Some description"));
        assertThat(description, not(containsString("EXECUTED")));

        // The same applies when editing the title of the change request.
        changeRequestPage.setChangeRequestTitleExpectError("Title #set ($x = 1)", TITLE_ERROR);
        changeRequestPage.reloadPage();
        changeRequestPage = new ChangeRequestPage();
        assertEquals("Fix #123 for $5", changeRequestPage.getChangeRequestTitle());
        changeRequestPage = changeRequestPage.setChangeRequestTitle("Another title");
        assertEquals("Another title", changeRequestPage.getChangeRequestTitle());
    }
}
