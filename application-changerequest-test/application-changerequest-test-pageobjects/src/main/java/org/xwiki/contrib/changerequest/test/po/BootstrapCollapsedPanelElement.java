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
import org.openqa.selenium.WebElement;
import org.xwiki.stability.Unstable;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Abstract representation of a boostrap collapsed panel element, which can be used in various context.
 * Note: this class could be moved to XWiki platform.
 *
 * @version $Id$
 * @since 0.8
 */
@Unstable
public class BootstrapCollapsedPanelElement extends BaseElement
{
    private static final String PANEL_HEADING_CLASS = "panel-heading";
    private final WebElement panelElement;

    /**
     * Default constructor.
     *
     * @param panelElement element representing the panel.
     */
    public BootstrapCollapsedPanelElement(WebElement panelElement)
    {
        this.panelElement = panelElement;
    }

    /**
     * Check if the current panel is open.
     * Note that this method returns false when the panel is collapsing during a transition, so be careful when using
     * this method after a click.
     *
     * @return {@code true} if the panel is opened.
     */
    public boolean isOpened()
    {
        WebElement panelBodyParent = panelElement.findElement(By.className("panel-collapse"));
        // We assume that the "in" classname is always at the end of the list of classes: it's always true when
        // manipulating the panels with bootstrap, and it's easier to detect it like that, to avoid false positive
        // with class name containing a "in".
        return panelBodyParent.getAttribute("class").matches("^.*( in)$");
    }

    /**
     * Toggle the panel to open or close it depending on its current state.
     * This method waits for the state change.
     */
    public void togglePanel()
    {
        boolean isOpened = this.isOpened();
        WebElement toggleLink = panelElement.findElement(By.className(PANEL_HEADING_CLASS))
            .findElement(By.cssSelector("a[data-toggle='collapse']"));

        toggleLink.click();
        getDriver().waitUntilCondition(driver -> this.isOpened() != isOpened);
    }

    /**
     * @return the whole panel.
     */
    public WebElement getPanel()
    {
        return this.panelElement;
    }

    /**
     * @return the container element of the panel body.
     */
    public WebElement getBody()
    {
        return this.panelElement.findElement(By.className("panel-body"));
    }

    /**
     * @return the container element of the panel heading.
     */
    public WebElement getHeading()
    {
        return this.panelElement.findElement(By.className(PANEL_HEADING_CLASS));
    }

    /**
     * @return the panel title element.
     */
    public WebElement getTitle()
    {
        return this.panelElement.findElement(By.className("panel-title"));
    }
}
