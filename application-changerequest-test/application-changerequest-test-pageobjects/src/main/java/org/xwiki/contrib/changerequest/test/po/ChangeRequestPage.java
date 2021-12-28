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
import org.openqa.selenium.ElementNotVisibleException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.xwiki.contrib.changerequest.test.po.checks.ChecksPane;
import org.xwiki.contrib.changerequest.test.po.description.DescriptionPane;
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
        if (!tab.getAttribute("class").contains("active")) {
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
        return getDriver().findElement(By.id("changeRequestButtons"));
    }

    private WebElement getReviewButton()
    {
        return getMenuButtonsContainer().findElement(By.id("addReview"));
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
     * @throws ElementNotVisibleException if the review button is not displayed
     * @see #isReviewButtonDisplayed()
     */
    public boolean isReviewButtonEnabled()
    {
        if (!isReviewButtonDisplayed()) {
            throw new ElementNotVisibleException("The review button is not displayed.");
        } else {
            WebElement reviewButton = getReviewButton();
            return reviewButton.isEnabled();
        }
    }

    private WebElement getReadyForReviewButton()
    {
        return getMenuButtonsContainer().findElement(By.className("cr-ready-for-review"));
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
     * @throws ElementNotVisibleException if the ready for review button is not displayed
     * @see #isReadyForReviewButtonDisplayed()
     */
    public boolean isReadyForReviewButtonEnabled()
    {
        if (!isReadyForReviewButtonDisplayed()) {
            throw new ElementNotVisibleException("The ready for review button is not displayed.");
        } else {
            WebElement readyForReviewButton = getReadyForReviewButton();
            return readyForReviewButton.isEnabled();
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
        getMenuButtonsContainer().findElement(By.className("dropdown-toggle")).click();
    }

    private WebElement getConvertToDraft()
    {
        return getMenuButtonsContainer().findElement(By.cssSelector("li[class='cr-convert-to-draft']"));
    }

    /**
     * Check if the convert to draft action exists and is enabled.
     *
     * @return {@code true} if the action exists and is enabled.
     */
    public boolean isConvertToDraftEnabled()
    {
        try {
            WebElement convertToDraft = getConvertToDraft();
            return convertToDraft.isEnabled();
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
        WebElement convertToDraft = getConvertToDraft();
        if (!convertToDraft.isDisplayed()) {
            toggleChangeRequestActionsMenu();
        }
        getDriver().addPageNotYetReloadedMarker();
        convertToDraft.click();
        getDriver().waitUntilPageIsReloaded();
        return new ChangeRequestPage();
    }
}
