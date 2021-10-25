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

    private void openCollapsed(WebElement collapseLink)
    {
        if (collapseLink.getAttribute(CLASS).contains(COLLAPSED)) {
            collapseLink.click();
            getDriver().waitUntilCondition(condition -> collapseLink.getAttribute(CLASS).contains(COLLAPSED));
        }
    }

    /**
     * Open the collapse containing inputs for creating a new change request.
     */
    public void openCreateChangeRequestCollapse()
    {
        WebElement collapseLink = getDriver().findElement(By.linkText("Create new change request"));
        this.openCollapsed(collapseLink);
    }

    /**
     * Open the collapse containing inputs for adding changes to an existing change request.
     */
    public void openAddChangesToExistingChangeRequestCollapse()
    {
        WebElement collapseLink = getDriver().findElement(By.linkText("Add changes to existing change request"));
        this.openCollapsed(collapseLink);
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
        suggestInputElement.selectTypedText();
    }

    /**
     * Save the changes.
     * @return a new change request page.
     */
    public ChangeRequestPage clickSave()
    {
        getDriver().findElement(By.id("saveChangeRequest")).click();
        ChangeRequestPage changeRequestPage = new ChangeRequestPage();
        changeRequestPage.waitUntilPageIsLoaded();
        return changeRequestPage;
    }
}
