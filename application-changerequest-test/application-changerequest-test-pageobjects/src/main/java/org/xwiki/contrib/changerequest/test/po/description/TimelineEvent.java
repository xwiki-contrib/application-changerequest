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
package org.xwiki.contrib.changerequest.test.po.description;

import java.util.Date;

import org.codehaus.plexus.util.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.xwiki.contrib.changerequest.test.po.discussion.DiffMessageElement;
import org.xwiki.contrib.changerequest.test.po.discussion.MessageElement;
import org.xwiki.contrib.changerequest.test.po.discussion.ReviewDiscussion;
import org.xwiki.stability.Unstable;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Represents the various events displayed in the timeline of a change request.
 *
 * @version $Id$
 * @since 0.8
 */
@Unstable
public class TimelineEvent extends BaseElement
{
    private static final String DISCUSSION = "discussion";

    private final WebElement eventElement;

    /**
     * Default constructor.
     *
     * @param eventElement the container element of the event.
     */
    public TimelineEvent(WebElement eventElement)
    {
        this.eventElement = eventElement;
    }

    /**
     * @return the actual date of the event.
     */
    public Date getDate()
    {
        String date = getDriver().findElementWithoutWaiting(this.eventElement, By.className("date"))
            .getAttribute("data-date");
        long dateLong = Long.parseLong(date);
        return new Date(dateLong);
    }

    /**
     * @return the content of the event.
     */
    public WebElement getContent()
    {
        return getDriver().findElementWithoutWaiting(this.eventElement, By.className("content"));
    }

    /**
     * @return the type of the event
     */
    public String getEventType()
    {
        return this.eventElement.getAttribute("data-event-type");
    }

    /**
     * Retrieve the message attached to the event.
     * Note that this method will throw a {@link NoSuchElementException} if the type of the event is not
     * {@code changerequest.discussions} as it's the only case where a single message is attached to an event.
     * For getting a discussion attached to a review see {@link #getReviewDiscussion()}.
     * Also note that this method might return a {@link DiffMessageElement} or a {@link MessageElement} depending if
     * a diff context is attached.
     * @return a {@link MessageElement} or {@link DiffMessageElement} depending if there's a diff context
     */
    public MessageElement getMessage()
    {
        String eventType = getEventType();
        if ("changerequest.discussions".equals(eventType)) {
            WebElement discussionContainer =
                getDriver().findElementWithoutWaiting(this.eventElement, By.className(DISCUSSION));
            WebElement message = getDriver().findElementWithoutWaiting(discussionContainer, By.className("message"));
            if (StringUtils.contains(discussionContainer.getAttribute("class"), "diff")) {
                return new DiffMessageElement(message,
                    getDriver().findElementWithoutWaiting(discussionContainer, By.className("diff-block-metadata")));
            } else {
                return new MessageElement(message);
            }
        } else {
            throw new NoSuchElementException(
                String.format("The event is of type [%s] only discussions events can contain a single message.",
                    eventType));
        }
    }

    /**
     * Retrieve the discussion attached to a review.
     * Note that this method might throw a {@link NoSuchElementException} if the event type is not of
     * {@code changerequest.review.added}.
     * @return the discussion attached to the review mentioned in the event
     */
    public ReviewDiscussion getReviewDiscussion()
    {
        String eventType = getEventType();
        if ("changerequest.review.added".equals(eventType)) {
            return new ReviewDiscussion(getDriver().findElementWithoutWaiting(this.eventElement,
                By.className(DISCUSSION)));
        } else {
            throw new NoSuchElementException(
                String.format("The event is of type [%s] only review events can contain a review discussion.",
                    eventType));
        }
    }
}
