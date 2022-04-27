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
package org.xwiki.contrib.changerequest.test.po.reviews;

import java.util.Date;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.xwiki.stability.Unstable;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Represents a review element displayed in the reviews tab.
 *
 * @version $Id$
 * @since 0.8
 */
@Unstable
public class ReviewElement extends BaseElement
{
    private final WebElement reviewElement;

    /**
     * Default constructor.
     *
     * @param reviewElement the element representing a review.
     */
    public ReviewElement(WebElement reviewElement)
    {
        this.reviewElement = reviewElement;
    }

    /**
     * @return {@code true} if the review is an approval.
     */
    public boolean isApproval()
    {
        return Boolean.parseBoolean(this.reviewElement.getAttribute("data-approved"));
    }

    /**
     * @return {@code true} if the review is outdated.
     */
    public boolean isOutdated()
    {
        return !Boolean.parseBoolean(this.reviewElement.getAttribute("data-valid"));
    }

    /**
     * @return the actual date of the review.
     */
    public Date getDate()
    {
        return new Date(Long.parseLong(this.reviewElement.getAttribute("data-date")));
    }

    /**
     * @return the author of the review.
     */
    public String getAuthor()
    {
        return this.reviewElement.getAttribute("data-author");
    }

    /**
     * @return the original approver of the review
     */
    public String getOriginalApprover()
    {
        return this.reviewElement.getAttribute("data-original-approver");
    }

    private WebElement getToggleValidButton()
    {
        return this.reviewElement.findElement(By.className("review-toggle-valid"));
    }

    /**
     * @return {@code true} if the button to mark the review as valid or invalid is enabled.
     */
    public boolean isToggleValidButtonEnabled()
    {
        try {
            WebElement toggleValidButton = getToggleValidButton();
            return toggleValidButton.isEnabled();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    /**
     * Click on the button to mark the review as outdated or valid again, and wait for the page to be reloaded.
     */
    public void clickToggleValidButton()
    {
        getDriver().addPageNotYetReloadedMarker();
        getToggleValidButton().click();
        getDriver().waitUntilPageIsReloaded();
    }
}
