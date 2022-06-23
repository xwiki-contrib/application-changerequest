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
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.BaseModal;
import org.xwiki.test.ui.po.SuggestInputElement;

/**
 * Represents the modal that is opened when save as change request is clicked.
 *
 * @version $Id$
 * @since 0.5
 */
public class ChangeRequestSaveModal extends BaseModal
{
    private static final String CLASS = "class";
    private static final String COLLAPSED = "collapsed";

    /**
     * Default constructor.
     */
    public ChangeRequestSaveModal()
    {
        super(By.id("changeRequestModal"));
    }

    private WebElement getCreateChangeRequestDiv()
    {
        return getDriver().findElementWithoutWaiting(this.container, By.id("newChangeRequest"));
    }

    /**
     * Open the block containing inputs for creating a new change request.
     */
    public void openCreateChangeRequest()
    {
        if (!this.isCreateChangeRequestDisplayed()) {
            WebElement link = getDriver().findElementWithoutWaiting(this.container,
                By.cssSelector("a[aria-controls=newChangeRequest]"));
            link.click();
            getDriver().waitUntilCondition(driver -> this.isCreateChangeRequestDisplayed());
        }
    }

    /**
     * @return {@code true} if the block to create a new change request is collapsed.
     */
    public boolean isCreateChangeRequestDisplayed()
    {
        return getCreateChangeRequestDiv().isDisplayed();
    }

    private WebElement getAddChangesToExistingChangeRequestDiv()
    {
        return getDriver().findElementWithoutWaiting(this.container, By.id("addToExistingChangeRequest"));
    }

    /**
     * Open the collapse containing inputs for adding changes to an existing change request.
     */
    public void openAddChangesToExistingChangeRequestCollapse()
    {
        if (!this.isAddChangesToExistingChangeRequestDisplayed()) {
            WebElement link = getDriver().findElementWithoutWaiting(this.container,
                By.cssSelector("a[aria-controls=addToExistingChangeRequest]"));
            link.click();
            getDriver().waitUntilCondition(driver -> this.isAddChangesToExistingChangeRequestDisplayed());
        }
    }

    /**
     * @return {@code true} if the block to add changes to an existing change request is collapsed.
     */
    public boolean isAddChangesToExistingChangeRequestDisplayed()
    {
        return getAddChangesToExistingChangeRequestDiv().isDisplayed();
    }



    /**
     * Set the title for a new change request.
     * @param title the title of a new change request.
     */
    public void setChangeRequestTitle(String title)
    {
        this.getDriver().findElement(By.id("crTitle")).sendKeys(title);
    }

    /**
     * Check or uncheck the checkbox to create the change request as draft or as ready for review.
     * @param value if {@code true} the checkbox is checked.
     */
    public void setDraftCheckbox(boolean value)
    {
        WebElement crDraft = this.getDriver().findElement(By.id("crDraft"));
        boolean selected = crDraft.isSelected();
        if (selected != value) {
            crDraft.click();
        }
    }

    /**
     * Select a change request based on its name to add changes to an existing change request.
     * @param changeRequestName the name of the change request to add changes to.
     */
    public void selectExistingChangeRequest(String changeRequestName)
    {
        SuggestInputElement suggestInputElement =
            new SuggestInputElement(this.getDriver().findElement(By.id("existingCRSelector")));
        suggestInputElement.sendKeys(changeRequestName);
        suggestInputElement.waitForSuggestions();
        suggestInputElement.selectByVisibleText(changeRequestName);
    }

    /**
     * Save the changes.
     * @return a new change request page.
     */
    public ChangeRequestPage clickSave()
    {
        getDriver().addPageNotYetReloadedMarker();
        getDriver().findElement(By.id("saveChangeRequest")).click();
        getDriver().waitUntilPageIsReloaded();
        ChangeRequestPage changeRequestPage = new ChangeRequestPage();
        changeRequestPage.waitUntilPageIsReady();
        return changeRequestPage;
    }
}
