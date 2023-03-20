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

import java.util.Date;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Represent a message displayed in the change request timeline.
 *
 * @version $Id$
 * @since 1.5
 */
public class MessageElement extends BaseElement
{
    private static final String MESSAGE_CONTENT = "message-content";

    private final WebElement container;

    /**
     * Default constructor.
     * @param container the dom element representing the message
     */
    public MessageElement(WebElement container)
    {
        this.container = container;
    }

    protected WebElement getContainer()
    {
        return this.container;
    }

    /**
     * @return the author of the message
     */
    public String getAuthor()
    {
        WebElement messageAuthor =
            getDriver().findElementWithoutWaiting(this.container, By.className("message-author"));
        return getDriver().findElementWithoutWaiting(messageAuthor, By.tagName("span")).getAttribute("data-reference");
    }

    /**
     * @return the date of the message
     */
    public Date getDate()
    {
        String timestamp = getDriver().findElementWithoutWaiting(this.container, By.className("message-creationDate"))
            .getAttribute("data-timestamp");
        return new Date(Long.parseLong(timestamp));
    }

    /**
     * @return {@code true} if the message is expanded
     */
    public boolean isExpanded()
    {
        return getDriver().findElementWithoutWaiting(this.container, By.className(MESSAGE_CONTENT)).isDisplayed();
    }

    /**
     * @return the content of the message
     */
    public String getContent()
    {
        return getDriver().findElementWithoutWaiting(this.container, By.className(MESSAGE_CONTENT)).getText();
    }

    /**
     * @return {@code true} if the message is an answer
     */
    public boolean isAnswer()
    {
        return getDriver().hasElement(this.container, By.className("reply-to-header"));
    }

    /**
     * Click on the reply button and returns the opened editor.
     * @return an instance of {@link DiscussionEditor} to manipulate the editor for the reply
     */
    public DiscussionEditor clickReply()
    {
        WebElement replyButton =
            getDriver().findElementWithoutWaiting(this.container, By.className("reply-button"));
        getDriver().waitUntilCondition(driver -> replyButton.getAttribute("class").contains("reply-initialized"));
        replyButton.click();
        WebElement addComment = this.container.findElement(By.xpath("../div/div[@class='add-comment']"));
        return new DiscussionEditor(addComment);
    }
}
