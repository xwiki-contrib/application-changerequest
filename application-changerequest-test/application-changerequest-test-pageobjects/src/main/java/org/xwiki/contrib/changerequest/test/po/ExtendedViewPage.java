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
package org.xwiki.contrib.changerequest.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.xwiki.ckeditor.test.po.CKEditor;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.WikiEditPage;

/**
 * Represents a view page on a wiki where Change Request is installed.
 *
 * @version $Id$
 * @since 0.5
 */
public class ExtendedViewPage extends ViewPage
{
    private static final String CR_EDIT_ID = "crEdit";
    private static final String STANDARD_EDIT_ID = "tmEdit";
    private static final String STANDARD_DELETE_ID = "tmDelete";
    private static final String CR_DELETE_ID = "deletecr";
    private static final String STANDARD_CREATE_ID = "tmCreate";
    private static final String CR_CREATE_ID = "crCreate";

    private static final String CR_TAB_ID = "org.xwiki.contrib.changerequest.docextratab";

    /**
     * Check if the current view has a change request edit button: this button is normally visible only when users does
     * not have edit right, but has right to create a change request.
     * @return {@code true} if the change request edit button is visible, {@code false} otherwise.
     */
    public boolean hasChangeRequestEditButton()
    {
        return getDriver().hasElementWithoutWaiting(By.id(CR_EDIT_ID));
    }

    /**
     * Check if the current view has the standard edit button.
     * @return {@code true} if the standard edit button is available.
     */
    public boolean hasStandardEditButton()
    {
        return getDriver().hasElementWithoutWaiting(By.id(STANDARD_EDIT_ID))
            && getDriver().findElementWithoutWaiting(By.id(STANDARD_EDIT_ID)).isDisplayed();
    }

    /**
     * Click on the standard edit button and returns the extended edit page that has been opened.
     *
     * @return a new instance of extended edit page.
     */
    public ExtendedEditPage<WikiEditPage> clickStandardEdit()
    {
        return this.clickStandardEdit(false);
    }

    /**
     * Click on the standard edit button and returns the extended edit page that has been opened.
     *
     * @param isWysiwyg {@code true} if the wysiwyg editor should be used.
     * @return a new instance of extended edit page.
     */
    public ExtendedEditPage clickStandardEdit(boolean isWysiwyg)
    {
        super.edit();
        ExtendedEditPage extendedEditPage;
        if (isWysiwyg) {
            CKEditor editor = new CKEditor("content");
            editor.waitToLoad();
            extendedEditPage = new ExtendedEditPage<>(editor);
        } else {
            WikiEditPage wikiEditPage = new WikiEditPage();
            extendedEditPage = new ExtendedEditPage<>(wikiEditPage);
            wikiEditPage.waitUntilPageIsReady();
        }
        return extendedEditPage;
    }

    /**
     * Click on the change request edit button and returns the extended edit page that has been opened.
     *
     * @return a new instance of extended edit page.
     */
    public ExtendedEditPage<WikiEditPage> clickChangeRequestEdit()
    {
        getDriver().findElement(By.id(CR_EDIT_ID)).findElement(By.className("btn-default")).click();
        ExtendedEditPage<WikiEditPage> extendedEditPage = new ExtendedEditPage<>(new WikiEditPage());
        extendedEditPage.getWrappedEditor().waitUntilPageIsReady();
        return extendedEditPage;
    }

    private boolean hasMoreActionMenuEntry(String entryId)
    {
        boolean result;
        this.toggleActionMenu();
        try {
            WebElement deleteButton = getDriver().findElementWithoutWaiting(By.id(entryId));
            result = deleteButton.isDisplayed();
        } catch (NoSuchElementException e) {
            result = false;
        }
        this.toggleActionMenu();
        return result;
    }

    /**
     * @return {@code true} if the more action menu entry has the standard "delete" entry.
     */
    public boolean hasStandardDeleteMenuEntry()
    {
        return this.hasMoreActionMenuEntry(STANDARD_DELETE_ID);
    }

    /**
     * @return {@code true} if the more action menu entry has an entry for "request for deletion".
     */
    public boolean hasRequestForDeletionMenuEntry()
    {
        return this.hasMoreActionMenuEntry(CR_DELETE_ID);
    }

    /**
     * Click on the standard delete menu entry, and return the page to confirm the deletion.
     *
     * @return a new instance of {@link ExtendedDeleteConfirmationPage} to confirm the deletion.
     * @see #hasStandardDeleteMenuEntry()
     */
    public ExtendedDeleteConfirmationPage clickStandardDelete()
    {
        toggleActionMenu();
        getDriver().findElementWithoutWaiting(By.id(STANDARD_DELETE_ID)).click();
        return new ExtendedDeleteConfirmationPage();
    }

    /**
     * Click on the request for deletion menu entry, and return the page to confirm the deletion.
     *
     * @return a new instance of {@link ExtendedDeleteConfirmationPage} to confirm the deletion.
     * @see #hasRequestForDeletionMenuEntry()
     */
    public ExtendedDeleteConfirmationPage clickRequestForDeletion()
    {
        toggleActionMenu();
        getDriver().findElementWithoutWaiting(By.id(CR_DELETE_ID)).click();
        return new ExtendedDeleteConfirmationPage();
    }

    /**
     * @return {@code true} if the standard create button is displayed.
     */
    public boolean hasStandardCreate()
    {
        try {
            WebElement createButton = getDriver().findElementWithoutWaiting(By.id(STANDARD_CREATE_ID));
            return createButton.isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    /**
     * @return {@code true} if the change request create button is displayed.
     */
    public boolean hasChangeRequestCreate()
    {
        try {
            WebElement createButton = getDriver().findElementWithoutWaiting(By.id(CR_CREATE_ID));
            return createButton.isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    /**
     * Click on the standard create button and display the create template.
     *
     * @return the {@link ExtendedCreatePage} to manipulate the create template.
     * @see #hasStandardCreate()
     */
    public ExtendedCreatePage clickStandardCreate()
    {
        WebElement createButton = getDriver().findElementWithoutWaiting(By.id(STANDARD_CREATE_ID));
        createButton.click();
        return new ExtendedCreatePage();
    }

    /**
     * Click on the change request create button and display the create template.
     *
     * @return the {@link ExtendedCreatePage} to manipulate the create template.
     * @see #hasChangeRequestCreate()
     */
    public ExtendedCreatePage clickChangeRequestCreate()
    {
        WebElement createButton = getDriver().findElementWithoutWaiting(By.id(CR_CREATE_ID));
        createButton.click();
        return new ExtendedCreatePage();
    }

    /**
     * Open the doc tab on the bottom of the page containing the list of change requests for that page.
     * @return the {@link ChangeRequestLiveDataElement} containing in the tab.
     */
    public ChangeRequestLiveDataElement openChangeRequestTab()
    {
        WebElement tab = getDriver().findElementWithoutWaiting(By.id(CR_TAB_ID));
        tab.click();
        return new ChangeRequestLiveDataElement("changerequest-livetable");
    }

    /**
     * Click on the manage approvers link and expect to have the save modal opened.
     *
     * @return an instance of the save modal to edit the approvers and save it as a change request change.
     */
    public ChangeRequestSaveModal clickManageApproversWithoutEditRight()
    {
        this.clickMoreActionsSubMenuEntry("manageapprovers");
        return new ChangeRequestSaveModal();
    }
}
