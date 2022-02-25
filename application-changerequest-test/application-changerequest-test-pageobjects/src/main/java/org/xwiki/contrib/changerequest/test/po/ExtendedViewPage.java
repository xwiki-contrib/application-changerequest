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
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.WYSIWYGEditPage;

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
    public ExtendedEditPage<WYSIWYGEditPage> clickStandardEdit()
    {
        super.edit();
        ExtendedEditPage<WYSIWYGEditPage> extendedEditPage = new ExtendedEditPage<>(new WYSIWYGEditPage());
        extendedEditPage.getEditor().waitUntilPageIsLoaded();
        return extendedEditPage;
    }

    /**
     * Click on the change request edit button and returns the extended edit page that has been opened.
     *
     * @return a new instance of extended edit page.
     */
    public ExtendedEditPage<WYSIWYGEditPage> clickChangeRequestEdit()
    {
        getDriver().findElement(By.id(CR_EDIT_ID)).findElement(By.className("btn-default")).click();
        ExtendedEditPage<WYSIWYGEditPage> extendedEditPage = new ExtendedEditPage<>(new WYSIWYGEditPage());
        extendedEditPage.getEditor().waitUntilPageIsLoaded();
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
}
