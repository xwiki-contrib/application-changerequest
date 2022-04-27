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

import org.openqa.selenium.By;
import org.openqa.selenium.NotFoundException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.xwiki.ckeditor.test.po.CKEditor;
import org.xwiki.stability.Unstable;
import org.xwiki.test.ui.po.BaseModal;

/**
 * Represents the modal that opens to post a review.
 *
 * @version $Id$
 * @since 0.8
 */
@Unstable
public class ReviewModal extends BaseModal
{
    private static final String SELECT_ORIGINAL_APPROVER_ID = "originalApprover";

    private final CKEditor commentEditor;

    /**
     * Default constructor.
     */
    public ReviewModal()
    {
        super(By.id("reviewModal"));
        this.commentEditor = new CKEditor("content").waitToLoad();
    }

    /**
     * Click on the approve radio button.
     */
    public void selectApprove()
    {
        this.container.findElement(By.id("approveReview")).click();
    }

    /**
     * Click on the request for change radio button.
     */
    public void selectRequestChanges()
    {
        this.container.findElement(By.id("requestChangeReview")).click();
    }

    /**
     * Set a comment for the review.
     *
     * @param comment the comment of the review.
     */
    public void setComment(String comment)
    {
        this.commentEditor.getRichTextArea().clear();
        this.commentEditor.getRichTextArea().setContent(comment);
    }

    /**
     * Click on the cancel button and wait for the modal to be closed.
     */
    public void cancel()
    {
        this.container.findElement(By.className("review-cancel")).click();
        this.waitForClosed();
    }

    private WebElement getSaveButton()
    {
        return this.container.findElement(By.id("submitReview"));
    }

    /**
     * Check if the save button is enabled.
     *
     * @return {@code true} if the button is enabled.
     */
    public boolean isSaveEnabled()
    {
        return this.getSaveButton().isEnabled();
    }

    /**
     * Click on the save button and wait for the page to be reloaded.
     */
    public void save()
    {
        // Right now the page is reloaded when a review is posted.
        getDriver().addPageNotYetReloadedMarker();
        this.getSaveButton().click();
        getDriver().waitUntilPageIsReloaded();
    }

    /**
     * @return {@code true} if the select allowing to chose on behalf of whom to review is displayed.
     * @since 0.13
     */
    public boolean isSelectOnBehalfDisplayed()
    {
        try {
            WebElement selectElement =
                getDriver().findElementWithoutWaiting(By.id(SELECT_ORIGINAL_APPROVER_ID));
            return selectElement.isDisplayed();
        } catch (NotFoundException e) {
            return false;
        }
    }

    /**
     * @return the select allowing to chose on behalf of whom to review
     */
    public Select getOriginalApproverSelector()
    {
        WebElement selectElement =
            getDriver().findElementWithoutWaiting(By.id(SELECT_ORIGINAL_APPROVER_ID));
        return new Select(selectElement);
    }
}
