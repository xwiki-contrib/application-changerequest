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
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.xwiki.contrib.changerequest.test.po.checks.ChecksPane;
import org.xwiki.contrib.changerequest.test.po.description.DescriptionPane;
import org.xwiki.contrib.changerequest.test.po.filechanges.FileChangesPane;
import org.xwiki.contrib.changerequest.test.po.reviews.ReviewContainer;
import org.xwiki.contrib.changerequest.test.po.reviews.ReviewsPane;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents a change request page.
 *
 * @version $Id$
 * @since 0.5
 */
public class ChangeRequestPage extends ViewPage
{
    private static final String CLASS_ATTRIBUTE = "class";

    /**
     * @return the actual label describing the status of the change request.
     */
    public String getStatusLabel()
    {
        return getDriver().findElement(By.className("document-info")).findElement(By.className("label")).getText();
    }

    private WebElement openTab(String tabname)
    {
        WebElement tab = getDriver().findElement(By.id(tabname));
        if (!tab.getAttribute(CLASS_ATTRIBUTE).contains("active")) {
            getDriver().findElement(By.cssSelector(String.format("a[aria-controls=%s]", tabname))).click();
        }
        return tab;
    }

    /**
     * Open the description tab and returns it.
     *
     * @return a {@link DescriptionPane} to see the description, timeline and comments.
     */
    public DescriptionPane openDescription()
    {
        WebElement description = this.openTab("home");
        return new DescriptionPane(description);
    }

    /**
     * Open the file changes tab and returns it.
     *
     * @return a {@link FileChangesPane} to navigate in the file changes.
     */
    public FileChangesPane openFileChanges()
    {
        WebElement filechanges = this.openTab("filechanges");
        return new FileChangesPane(filechanges);
    }

    /**
     * Open the reviews tab and returns it.
     *
     * @return a {@link ReviewsPane} to list approvers and reviews.
     */
    public ReviewsPane openReviewsPane()
    {
        WebElement reviews = this.openTab("reviews");
        return new ReviewsPane(reviews);
    }

    /**
     * Open the checks tab and returns it.
     *
     * @return a {@link ChecksPane} to allow verifying the checks.
     */
    public ChecksPane openChecksPane()
    {
        WebElement checks = this.openTab("checks");
        return new ChecksPane(checks);
    }

    private WebElement getMenuButtonsContainer()
    {
        return getDriver().findElementWithoutWaiting(By.id("changeRequestButtons"));
    }

    private WebElement getReviewButton()
    {
        return getDriver().findElementWithoutWaiting(getMenuButtonsContainer(), By.id("addReview"));
    }

    /**
     * @return {@code true} if the review button is displayed.
     */
    public boolean isReviewButtonDisplayed()
    {
        try {
            WebElement reviewButton = getReviewButton();
            return reviewButton.isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    /**
     * @return {@code true} if the review button is displayed and enabled.
     * @throws ElementNotInteractableException if the review button is not displayed
     * @see #isReviewButtonDisplayed()
     */
    public boolean isReviewButtonEnabled()
    {
        if (!isReviewButtonDisplayed()) {
            throw new ElementNotInteractableException("The review button is not displayed.");
        } else {
            WebElement reviewButton = getReviewButton();
            return isElementEnabled(reviewButton);
        }
    }

    /**
     * Click on the review button and wait for the review modal to be displayed before returning it.
     * Note that this method does not perform any check on the review button.
     *
     * @return a review modal.
     */
    public ReviewContainer clickReviewButton()
    {
        getReviewButton().click();
        ReviewContainer reviewContainer = new ReviewContainer();
        getDriver().waitUntilCondition(driver -> reviewContainer.isDisplayed());
        return reviewContainer;
    }

    private WebElement getReadyForReviewButton()
    {
        return getDriver().findElementWithoutWaiting(getMenuButtonsContainer(), By.className("cr-ready-for-review"));
    }

    /**
     * @return {@code true} if any change request primary button is displayed.
     */
    public boolean isAnyChangeRequestButtonDisplayed()
    {
        try {
            return getMenuButtonsContainer().isDisplayed();
        } catch (NoSuchElementException e)
        {
            return false;
        }
    }

    /**
     * @return {@code true} if the ready for review button is displayed.
     */
    public boolean isReadyForReviewButtonDisplayed()
    {
        try {
            WebElement readyForReviewButton = getReadyForReviewButton();
            return readyForReviewButton.isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    /**
     * @return {@code true} if the ready for review button is displayed and enabled.
     * @throws ElementNotInteractableException if the ready for review button is not displayed
     * @see #isReadyForReviewButtonDisplayed()
     */
    public boolean isReadyForReviewButtonEnabled()
    {
        if (!isReadyForReviewButtonDisplayed()) {
            throw new ElementNotInteractableException("The ready for review button is not displayed.");
        } else {
            WebElement readyForReviewButton = getReadyForReviewButton();
            return isElementEnabled(readyForReviewButton);
        }
    }

    /**
     * Click on the ready for review button and wait for the page to be reloaded.
     * Note that this method does not perform any check on the button.
     *
     * @return a new instance after the page reload.
     */
    public ChangeRequestPage clickReadyForReviewButton()
    {
        getDriver().addPageNotYetReloadedMarker();
        getReadyForReviewButton().click();
        getDriver().waitUntilPageIsReloaded();
        return new ChangeRequestPage();
    }

    /**
     * Open the actions menu of the change request main button.
     */
    public void toggleChangeRequestActionsMenu()
    {
        getDriver().findElementWithoutWaiting(getMenuButtonsContainer(), By.className("dropdown-toggle")).click();
    }

    private WebElement getConvertToDraftButton()
    {
        return getDriver().findElementWithoutWaiting(getMenuButtonsContainer(), By.className("cr-convert-to-draft"));
    }

    /**
     * Check if the convert to draft action exists and is enabled.
     *
     * @return {@code true} if the action exists and is enabled.
     */
    public boolean isConvertToDraftEnabled()
    {
        try {
            WebElement convertToDraft = getConvertToDraftButton();
            return isElementEnabled(convertToDraft);
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    /**
     * Click on the "convert to draft" action.
     * Note that this method does not perform any check if the button exists or not.
     * However it does open the menu to click on the action item, if it's not opened yet.
     * @return a new instance of change request page after the page is reloaded.
     */
    public ChangeRequestPage clickConvertToDraft()
    {
        WebElement convertToDraft = getConvertToDraftButton();
        if (!convertToDraft.isDisplayed()) {
            toggleChangeRequestActionsMenu();
        }
        getDriver().addPageNotYetReloadedMarker();
        convertToDraft.click();
        getDriver().waitUntilPageIsReloaded();
        return new ChangeRequestPage();
    }

    private WebElement getCloseButton()
    {
        return getDriver().findElementWithoutWaiting(getMenuButtonsContainer(), By.className("cr-close"));
    }

    private boolean isElementEnabled(WebElement element)
    {
        boolean result = false;
        if (element.isEnabled()) {
            result = !element.getAttribute(CLASS_ATTRIBUTE).contains("disabled");
        }
        return result;
    }

    /**
     * Check if the close action exists and is enabled.
     *
     * @return {@code true} if the action exists and is enabled.
     */
    public boolean isCloseEnabled()
    {
        try {
            WebElement closeButton = getCloseButton();
            return isElementEnabled(closeButton);
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    /**
     * Click on the "Close" action.
     * Note that this method does not perform any check if the button exists or not.
     * However it does open the menu to click on the action item, if it's not opened yet.
     * @return a new instance of change request page after the page is reloaded.
     */
    public ChangeRequestPage clickClose()
    {
        WebElement closeButton = getCloseButton();
        if (!closeButton.isDisplayed()) {
            toggleChangeRequestActionsMenu();
        }
        getDriver().addPageNotYetReloadedMarker();
        closeButton.click();
        getDriver().waitUntilPageIsReloaded();
        return new ChangeRequestPage();
    }

    private WebElement getOpenButton()
    {
        return getDriver().findElementWithoutWaiting(getMenuButtonsContainer(), By.className("cr-open"));
    }

    /**
     * @return {@code true} if the open button is displayed.
     */
    public boolean isOpenButtonDisplayed()
    {
        try {
            WebElement openButton = getOpenButton();
            return openButton.isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    /**
     * @return {@code true} if the open button is displayed and enabled.
     * @throws ElementNotInteractableException if the open button is not displayed
     * @see #isOpenButtonDisplayed()
     */
    public boolean isOpenButtonEnabled()
    {
        if (!isOpenButtonDisplayed()) {
            throw new ElementNotInteractableException("The open button is not displayed.");
        } else {
            WebElement openButton = getOpenButton();
            return isElementEnabled(openButton);
        }
    }

    /**
     * Click on the open change request action.
     * @return a new instance of the change request page, after the page reload.
     */
    public ChangeRequestPage clickOpen()
    {
        getDriver().addPageNotYetReloadedMarker();
        getOpenButton().click();
        getDriver().waitUntilPageIsReloaded();
        return new ChangeRequestPage();
    }

    private WebElement getOpenAsDraftButton()
    {
        return getMenuButtonsContainer().findElement(By.className("cr-open-as-draft"));
    }

    /**
     * Check if the open as draft action exists and is enabled.
     *
     * @return {@code true} if the action exists and is enabled.
     */
    public boolean isOpenAsDraftEnabled()
    {
        try {
            WebElement openAsDraftButton = getOpenAsDraftButton();
            return isElementEnabled(openAsDraftButton);
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    /**
     * Click on the "Open as draft" action.
     * Note that this method does not perform any check if the button exists or not.
     * However it does open the menu to click on the action item, if it's not opened yet.
     * @return a new instance of change request page after the page is reloaded.
     */
    public ChangeRequestPage clickOpenAsDraft()
    {
        WebElement openAsDraftButton = getOpenAsDraftButton();
        if (!openAsDraftButton.isDisplayed()) {
            toggleChangeRequestActionsMenu();
        }
        getDriver().addPageNotYetReloadedMarker();
        openAsDraftButton.click();
        getDriver().waitUntilPageIsReloaded();
        return new ChangeRequestPage();
    }

    private WebElement getMergeButton()
    {
        return getDriver().findElementWithoutWaiting(getMenuButtonsContainer(), By.className("cr-merge"));
    }

    /**
     * @return {@code true} if the merge button is displayed.
     */
    public boolean isMergeButtonDisplayed()
    {
        try {
            WebElement mergeButton = getMergeButton();
            return mergeButton.isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    /**
     * @return {@code true} if the merge button is displayed and enabled.
     * @throws ElementNotInteractableException if the merge button is not displayed
     * @see #isMergeButtonDisplayed()
     */
    public boolean isMergeButtonEnabled()
    {
        if (!isMergeButtonDisplayed()) {
            throw new ElementNotInteractableException("The merge button is not displayed.");
        } else {
            WebElement mergeButton = getMergeButton();
            return isElementEnabled(mergeButton);
        }
    }

    /**
     * Click on the merge button and wait for the change request page to be reloaded.
     *
     * @return a new instance of the change request page after reload.
     */
    public ChangeRequestPage clickMergeButton()
    {
        getDriver().addPageNotYetReloadedMarker();
        getMergeButton().click();
        getDriver().waitUntilPageIsReloaded();
        return new ChangeRequestPage();
    }
}
