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

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
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
    private static final String SAVE_BUTTON_ID = "saveChangeRequest";
    private static final String APPROVERS_SELECTION_CONTAINER_ID = "approversSelection";

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
     * @return the selected element
     */
    public SuggestInputElement.SuggestionElement selectExistingChangeRequest(String changeRequestName)
    {
        SuggestInputElement suggestInputElement =
            new SuggestInputElement(this.getDriver().findElement(By.id("existingCRSelector")));
        List<SuggestInputElement.SuggestionElement> selectedSuggestions = suggestInputElement
            .sendKeys(changeRequestName)
            .waitForSuggestions()
            .sendKeys(Keys.ENTER)
            .getSelectedSuggestions();
        assert selectedSuggestions.size() == 1;
        return selectedSuggestions.get(0);
    }

    /**
     * Save the changes.
     * @return a new change request page.
     */
    public ChangeRequestPage clickSave()
    {
        getDriver().addPageNotYetReloadedMarker();
        getDriver().findElement(By.id(SAVE_BUTTON_ID)).click();
        getDriver().waitUntilPageIsReloaded();
        return new ChangeRequestPage();
    }

    /**
     * Click on save button but expect some kind of failure: e.g. the form not properly filled or a check preventing to
     * save.
     *
     * @param async {@code true} if the expectation is that the failure happens after an async request.
     */
    public void clickSaveExpectFailure(boolean async)
    {
        getDriver().findElement(By.id(SAVE_BUTTON_ID)).click();
    }

    /**
     * Check if the approvers selection is displayed in the modal.
     * @return {@code true} iff the modal displays field to select approvers.
     * @since 1.2
     */
    public boolean isApproversSelectionDisplayed()
    {
        try {
            return getDriver()
                .findElementWithoutWaiting(this.container, By.id(APPROVERS_SELECTION_CONTAINER_ID))
                .isDisplayed();
        } catch (NoSuchElementException e)
        {
            return false;
        }
    }

    /**
     * @return the {@link SuggestInputElement} allowing to manipulate the list of users approvers.
     */
    public SuggestInputElement getUsersApproverSelector()
    {
        WebElement approversSelectionContainer =
            getDriver().findElementWithoutWaiting(this.container, By.id(APPROVERS_SELECTION_CONTAINER_ID));
        for (WebElement input : getDriver().findElementsWithoutWaiting(approversSelectionContainer,
            By.tagName("select"))) {
            if (input.getAttribute("name").contains("user")) {
                return new SuggestInputElement(input);
            }
        }
        throw new NoSuchElementException("Can't find input element for user approvers selection");
    }

    private WebElement getErrorContainer()
    {
        return getDriver().findElementWithoutWaiting(this.container, By.id("form-validation-error"));
    }

    /**
     * Check if any error has been displayed.
     *
     * @return {@code true} if an error is displayed in the modal.
     */
    public boolean hasErrorDisplayed()
    {
        for (WebElement errorElement : getDriver().findElementsWithoutWaiting(getErrorContainer(),
            By.cssSelector(".text-danger"))) {
            if (errorElement.isDisplayed()) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return {@code true} if the specific error related to minimum number of approvers is displayed.
     */
    public boolean isMinimumApproverErrorDisplayed()
    {
        return getDriver().findElementWithoutWaiting(getErrorContainer(), By.id("minimumApprovers")).isDisplayed();
    }
}
