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

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Represents a specific {@link MessageElement} augmented to also display a contextual diff.
 *
 * @version $Id$
 * @since 1.5
 */
public class DiffMessageElement extends MessageElement
{
    private static final String DIFF_REFERENCE_TITLE = "diff-reference-title";
    private WebElement diffBlockContainer;

    /**
     * Default constructor.
     *
     * @param messageContainer the dom element containing the message information
     * @param diffBlockContainer the dom element containing the diff information
     */
    public DiffMessageElement(WebElement messageContainer, WebElement diffBlockContainer)
    {
        super(messageContainer);
        this.diffBlockContainer = diffBlockContainer;
    }

    /**
     * @return {@code true} if the diff is displayed
     */
    public boolean isDiffDisplayed()
    {
        return this.diffBlockContainer.isDisplayed();
    }

    /**
     * @return the page title displayed on top of the diff context
     */
    public String getPageTitle()
    {
        return getDriver().findElementWithoutWaiting(this.diffBlockContainer, By.className(DIFF_REFERENCE_TITLE))
            .getText();
    }

    /**
     * @return the full reference of the diff context
     */
    public String getFullReference()
    {
        return getDriver().findElementWithoutWaiting(this.diffBlockContainer, By.className(DIFF_REFERENCE_TITLE))
            .getAttribute("title");
    }

    /**
     * @return {@code true} if the details are expanded
     */
    public boolean isDetailsExpanded()
    {
        return getDriver().findElementWithoutWaiting(this.diffBlockContainer, By.className("diff-reference-details"))
            .isDisplayed();
    }

    /**
     * Click on the toggle button to expand or collapsed the diff details.
     */
    public void clickToggleDetails()
    {
        boolean isExpanded = isDetailsExpanded();
        getDriver().findElementWithoutWaiting(this.diffBlockContainer, By.className("diff-reference-details-switch"))
            .click();
        getDriver().waitUntilCondition(webDriver -> isExpanded != isExpanded());
    }

    /**
     * @return the details of the location displayed in the diff details
     */
    public String getLocationDetails()
    {
        return getDriver().findElementWithoutWaiting(this.diffBlockContainer,
                By.className("diff-reference-detail-location"))
            .getText();
    }

    /**
     * @return the details of the property displayed in the diff details
     */
    public String getPropertyDetails()
    {
        return getDriver().findElementWithoutWaiting(this.diffBlockContainer,
                By.className("diff-reference-detail-property"))
            .getText();
    }

    /**
     * @return the entire diff displayed on top of the message without the details
     */
    public List<String> getDiff()
    {
        List<String> result = new ArrayList<>();
        WebElement tableBody = getDriver().findElementWithoutWaiting(this.diffBlockContainer, By.tagName("tbody"));
        // Code inspired from EntityDiff#getDiff
        for (WebElement diffLine : getDriver().findElementsWithoutWaiting(tableBody, By.xpath(".//td[3]"))) {
            if (getDriver().findElementsWithoutWaiting(diffLine, By.tagName("ins")).size() > 0
                || getDriver().findElementsWithoutWaiting(diffLine, By.tagName("del")).size() > 0) {
                result.add(String.valueOf(getDriver().executeJavascript("return arguments[0].innerHTML", diffLine)));
            } else {
                result.add(diffLine.getText());
            }
        }

        return result;
    }
}
