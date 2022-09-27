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
import java.util.List;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Keys;
import org.xwiki.contrib.changerequest.test.po.ChangeRequestPage;
import org.xwiki.contrib.changerequest.test.po.ChangeRequestSaveModal;
import org.xwiki.contrib.changerequest.test.po.ExtendedCreatePage;
import org.xwiki.contrib.changerequest.test.po.ExtendedEditPage;
import org.xwiki.contrib.changerequest.test.po.ExtendedViewPage;
import org.xwiki.contrib.changerequest.test.po.FileChangesPane;
import org.xwiki.contrib.changerequest.test.po.reviews.ReviewsPane;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.SuggestInputElement;
import org.xwiki.test.ui.po.diff.EntityDiff;
import org.xwiki.test.ui.po.editor.WYSIWYGEditPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests related to using Change request with a minimum approver configuration set.
 *
 * @version $Id$
 * @since 1.2
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
public class MinimumApproversIT
{
    private static final String TEST_USER_PREFIX = "MinimumApprovers";
    private static final String APPROVERS_XLASS = "ChangeRequest.Code.ApproversClass";

    // Fixture:
    // Users:
    //   - ApproverA
    //   - ApproverB
    //   - ApproverC
    //   - Editor

    private static final String APPROVER_A = TEST_USER_PREFIX + "ApproverA";
    private static final String APPROVER_B = TEST_USER_PREFIX + "ApproverB";
    private static final String APPROVER_C = TEST_USER_PREFIX + "ApproverC";
    private static final String EDITOR = TEST_USER_PREFIX + "Editor";

    @Test
    @Order(0)
    void createCRCreatePageWithMinimumApprovers(TestUtils testUtils, TestReference testReference)
    {
        String serializedReference = testReference.getLocalDocumentReference().toString();
        testUtils.loginAsSuperAdmin();
        testUtils.createUser(APPROVER_A, APPROVER_A, null);
        testUtils.createUser(APPROVER_B, APPROVER_B, null);
        testUtils.createUser(APPROVER_C, APPROVER_C, null);
        testUtils.createUser(EDITOR, EDITOR, null);

        testUtils.setGlobalRights("", EDITOR, "edit", false);

        // We force the approver to use WYSIWYG editor, to avoid any problem to display the review modal.
        // This should be removed once https://jira.xwiki.org/browse/XWIKI-19281 is fixed
        testUtils.updateObject("XWiki", APPROVER_A, "XWiki.XWikiUsers", 0, "editor", "Wysiwyg");
        testUtils.updateObject("XWiki", APPROVER_B, "XWiki.XWikiUsers", 0, "editor", "Wysiwyg");
        testUtils.updateObject("XWiki", APPROVER_C, "XWiki.XWikiUsers", 0, "editor", "Wysiwyg");

        testUtils.updateObject(Arrays.asList("ChangeRequest", "Code"), "Configuration",
            "ChangeRequest.Code.ConfigurationClass", 0,
            "minimumApprovers", 2,
            "approvalStrategy", "allApproversNoFallback");

        testUtils.login(EDITOR, EDITOR);
        // Test page creation with CR: the modal should ask for a minimum approvers

        testUtils.gotoPage(testReference);
        ExtendedViewPage extendedViewPage = new ExtendedViewPage();
        assertFalse(extendedViewPage.hasStandardCreate());
        assertTrue(extendedViewPage.hasChangeRequestCreate());
        ExtendedCreatePage extendedCreatePage = extendedViewPage.clickChangeRequestCreate();

        ExtendedEditPage<WYSIWYGEditPage> editPage = extendedCreatePage.clickChangeRequestCreateButton();
        editPage.getEditor().setContent("Some content to the page");
        ChangeRequestSaveModal changeRequestSaveModal = editPage.clickSaveAsChangeRequest();
        assertFalse(changeRequestSaveModal.isMinimumApproverErrorDisplayed());

        assertTrue(changeRequestSaveModal.isApproversSelectionDisplayed());
        changeRequestSaveModal.setChangeRequestTitle("CR1");
        SuggestInputElement usersApproverSelector = changeRequestSaveModal.getUsersApproverSelector();
        usersApproverSelector.sendKeys("XWiki." + APPROVER_A).sendKeys(Keys.ENTER);

        changeRequestSaveModal.clickSaveExpectFailure(false);
        assertTrue(changeRequestSaveModal.isMinimumApproverErrorDisplayed());

        usersApproverSelector.sendKeys("XWiki." + APPROVER_B).sendKeys(Keys.ENTER);
        ChangeRequestPage changeRequestPage = changeRequestSaveModal.clickSave();
        String changeRequestURL = testUtils.getDriver().getCurrentUrl();

        ReviewsPane reviewsPane = changeRequestPage.openReviewsPane();
        assertTrue(reviewsPane.hasListOfApprovers());

        assertEquals(List.of(APPROVER_A, APPROVER_B), reviewsPane.getListOfApprovers());

        FileChangesPane fileChangesPane = changeRequestPage.openFileChanges();
        EntityDiff entityDiff = fileChangesPane.getEntityDiff(serializedReference, APPROVERS_XLASS + "[0]");

        // The label is retrieved from the translation.
        List<String> usersApproversDiff = entityDiff.getDiff("Users who can approve");
        assertEquals(List.of("@@ -1,0 +1,1 @@",
            String.format("+XWiki.%s,XWiki.%s", APPROVER_A, APPROVER_B)),
            usersApproversDiff);

        assertTrue(fileChangesPane.isEditApproversActionAvailable(serializedReference));
        changeRequestSaveModal = fileChangesPane.clickEditApprovers(serializedReference);

        assertTrue(changeRequestSaveModal.isApproversSelectionDisplayed());
        assertFalse(changeRequestSaveModal.isCreateChangeRequestDisplayed());
        usersApproverSelector = changeRequestSaveModal.getUsersApproverSelector();

        assertEquals(List.of("XWiki." + APPROVER_A, "XWiki." + APPROVER_B), usersApproverSelector.getValues());
        usersApproverSelector.sendKeys("XWiki." + APPROVER_C).sendKeys(Keys.ENTER);
        changeRequestPage = changeRequestSaveModal.clickSave();

        reviewsPane = changeRequestPage.openReviewsPane();
        assertTrue(reviewsPane.hasListOfApprovers());

        assertEquals(List.of(APPROVER_A, APPROVER_B, APPROVER_C), reviewsPane.getListOfApprovers());
        fileChangesPane = changeRequestPage.openFileChanges();
        entityDiff = fileChangesPane.getEntityDiff(serializedReference, APPROVERS_XLASS + "[0]");

        usersApproversDiff = entityDiff.getDiff("Users who can approve");

        assertEquals(List.of("@@ -1,0 +1,1 @@",
                String.format("+XWiki.%s,XWiki.%s,XWiki.%s", APPROVER_A, APPROVER_B, APPROVER_C)),
            usersApproversDiff);
    }
}
