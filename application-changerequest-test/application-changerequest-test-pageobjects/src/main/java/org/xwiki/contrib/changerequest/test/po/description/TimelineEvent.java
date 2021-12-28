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

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
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
        String date = this.eventElement.findElement(By.className("date")).getAttribute("data-date");
        long dateLong = Long.parseLong(date);
        return new Date(dateLong);
    }

    /**
     * @return the content of the event.
     */
    public WebElement getContent()
    {
        return this.eventElement.findElement(By.className("content"));
    }
}
