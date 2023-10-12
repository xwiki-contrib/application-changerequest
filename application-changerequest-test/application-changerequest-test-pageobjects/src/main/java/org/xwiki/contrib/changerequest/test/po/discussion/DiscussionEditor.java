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
package org.xwiki.contrib.changerequest.test.po.discussion;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.ckeditor.test.po.CKEditor;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Represents the edition UI for adding a new message.
 *
 * @version $Id$
 * @since 1.5
 */
public class DiscussionEditor extends BaseElement
{
    private final WebElement container;
    private final CKEditor editor;

    /**
     * Default constructor.
     * @param container the container where the editor is displayed
     */
    public DiscussionEditor(WebElement container)
    {
        this.container = container;
        this.editor = new CKEditor("content");
        this.editor.waitToLoad();
    }

    /**
     * Set the content of the message to save.
     * @param content the content of the message
     */
    public void setContent(String content)
    {
        this.editor.getRichTextArea().setContent(content);
    }

    /**
     * Click on the save button to submit the message.
     * Note that this method DOES NOT perform a wait: consumer of the API should call the proper wait which might
     * depend on the context (e.g. it could be a refresh of the timeline, or a reload of the page).
     */
    public void clickSave()
    {
        getDriver().findElementWithoutWaiting(this.container, By.className("comment-button")).click();
        this.waitForNotificationSuccessMessage("Message submitted with success.");
    }

    /**
     * Click on the cancel button.
     * When clicked the dom elements are removed, so users of the API should be careful to not use this instance
     * anymore.
     */
    public void clickCancel()
    {
        getDriver().findElementWithoutWaiting(this.container, By.className("cancel-button")).click();
    }
}
