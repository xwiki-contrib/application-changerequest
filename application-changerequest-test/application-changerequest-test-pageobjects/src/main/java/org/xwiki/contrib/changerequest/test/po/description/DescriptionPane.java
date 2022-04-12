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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.xwiki.stability.Unstable;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Represents the content of the description tab of a change request.
 *
 * @version $Id$
 * @since 0.8
 */
@Unstable
public class DescriptionPane extends BaseElement
{
    private static final String EDIT_DESCRIPTION_LINK_CLASS = "edit-description";
    private WebElement tabContainer;

    /**
     * Default constructor.
     *
     * @param tabContainer the container of the tab content.
     */
    public DescriptionPane(WebElement tabContainer)
    {
        this.tabContainer = tabContainer;
    }

    /**
     * @return the description of the change request.
     */
    public String getDescription()
    {
        return tabContainer.findElement(By.className("description-content")).getText();
    }

    private WebElement getDescriptionContainer()
    {
        return tabContainer.findElement(By.className("cr-description"));
    }

    /**
     * Check if a custom description is provided.
     * @return {@code true} if a custom description is provided.
     */
    public boolean hasCustomDescription()
    {
        return !getDriver().hasElementWithoutWaiting(getDescriptionContainer(),
            By.className("description-placeholder"));
    }

    /**
     * Check if there's an edit description link.
     * @return {@code true} if there's a link to edit the description.
     */
    public boolean hasEditDescriptionLink()
    {
        return getDriver().hasElement(getDescriptionContainer(), By.className(EDIT_DESCRIPTION_LINK_CLASS));
    }

    /**
     * Click on the edit description link.
     * @return the edit page resulting to the click of the link.
     */
    public DescriptionEditPage clickEditDescription()
    {
        getDriver().addPageNotYetReloadedMarker();
        getDescriptionContainer().findElement(By.className(EDIT_DESCRIPTION_LINK_CLASS)).click();
        getDriver().waitUntilPageIsReloaded();
        return new DescriptionEditPage();
    }

    /**
     * Retrieve the events of the timeline displayed in the description tab.
     *
     * @return the events of the timeline ordered as they are displayed.
     */
    public List<TimelineEvent> getEvents()
    {
        List<TimelineEvent> events = new ArrayList<>();
        List<WebElement> eventList =
            this.tabContainer.findElement(By.className("timeline")).findElements(By.className("event"));
        for (WebElement eventElement : eventList) {
            events.add(new TimelineEvent(eventElement));
        }
        return events;
    }

    /**
     * Wait until the timeline size is reached: this method should be used to avoid flickering behaviour if the events
     * are taking some time to appear.
     * Note that this method perform page reloads for the wait, so be careful to reinit the elements after using it
     * else you could obtain some {@link org.openqa.selenium.StaleElementReferenceException}.
     *
     * @param size the number of expected events.
     * @throws AssertionError if the number of events retrieved is higher than the expected number
     * @throws TimeoutException if after 5 reloads the number of events is still not reached.
     */
    public void waitUntilEventsSize(int size)
    {
        int maxLoop = 20;
        int loop = 0;
        int latestSize = -1;
        String originalUrl = getDriver().getCurrentUrl();
        // strip the anchor
        String anchorSeparator = "#";
        if (originalUrl.contains(anchorSeparator)) {
            originalUrl = originalUrl.substring(0, originalUrl.lastIndexOf(anchorSeparator));
        }
        String reloadUrl = "";
        do {
            List<TimelineEvent> events = getEvents();
            latestSize = events.size();
            if (latestSize > size) {
                TimelineEvent latestEvent = events.get(events.size() - 1);
                throw new AssertionError(String.format("[%s] events expected [%s] obtained. Latest event is [%s].",
                    size, latestSize, latestEvent.getContent().getText()));
            } else if (latestSize < size) {
                getDriver().addPageNotYetReloadedMarker();
                reloadUrl = originalUrl + "?refreshToken=" + RandomStringUtils.randomAlphanumeric(10);
                try {
                    getDriver().navigate().to(new URL(reloadUrl));
                } catch (MalformedURLException e) {
                    throw new RuntimeException(String.format("Error while creating reload URL [%s]", reloadUrl), e);
                }
                getDriver().waitUntilPageIsReloaded();
                this.tabContainer = getDriver().findElement(By.id("home"));
                loop++;
            }
        } while (loop < maxLoop && latestSize != size);
        if (latestSize != size) {
            throw new TimeoutException(
                String.format("Only [%s] events have been obtained on [%s] expected after [%s] reload of the page."
                        + "Latest reload URL: [%s]",
                    latestSize, size, maxLoop, reloadUrl));
        }
    }

    private WebElement getCommentsContainer()
    {
        return this.tabContainer.findElement(By.id("discussion-comments"));
    }

    /**
     * Check if the comment button is displayed.
     *
     * @return {@code true} if the comment button is displayed.
     */
    public boolean hasCommentButton()
    {
        return getDriver().hasElement(this.getCommentsContainer(), By.className("add-comment"));
    }
}
