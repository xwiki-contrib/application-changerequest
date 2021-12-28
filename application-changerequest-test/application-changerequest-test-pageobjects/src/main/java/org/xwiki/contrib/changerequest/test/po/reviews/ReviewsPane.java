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

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.stability.Unstable;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Page object to manipulate the reviews tab of a change request.
 *
 * @version $Id$
 * @since 0.8
 */
@Unstable
public class ReviewsPane extends BaseElement
{
    private final WebElement container;

    /**
     * Default constructor.
     *
     * @param container the main container of the tab.
     */
    public ReviewsPane(WebElement container)
    {
        this.container = container;
    }

    /**
     * Check if there is a list of approvers or if the list is empty.
     *
     * @return {@code true} if there is a defined list of approvers.
     */
    public boolean hasListOfApprovers()
    {
        WebElement approvers = this.container.findElement(By.className("approvers"));
        return getDriver().hasElement(approvers, By.className("approvers-list"));
    }

    /**
     * Retrieve the reviews of the tab.
     *
     * @return the list of reviews of the tab.
     */
    public List<ChangeRequestReviewElement> getReviews()
    {
        WebElement reviewsContainer = this.container.findElement(By.id("reviewsContainer"));
        List<ChangeRequestReviewElement> result = new ArrayList<>();
        List<WebElement> reviews = getDriver().findElementsWithoutWaiting(reviewsContainer, By.className("review"));
        for (WebElement reviewElement : reviews) {
            result.add(new ChangeRequestReviewElement(reviewElement));
        }
        return result;
    }
}
