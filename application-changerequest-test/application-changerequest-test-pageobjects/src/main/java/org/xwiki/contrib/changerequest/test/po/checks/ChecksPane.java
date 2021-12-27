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
package org.xwiki.contrib.changerequest.test.po.checks;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.stability.Unstable;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Represents the tab displaying the various checks.
 *
 * @version $Id$
 * @since 0.8
 */
@Unstable
public class ChecksPane extends BaseElement
{
    private final WebElement tabContainer;

    /**
     * Default constructor.
     *
     * @param tabContainer the container of the tab content.
     */
    public ChecksPane(WebElement tabContainer)
    {
        this.tabContainer = tabContainer;
    }

    /**
     * @return the panel concerning the change request status check.
     */
    public CheckPanelElement getStatusCheck()
    {
        WebElement panelElement = this.tabContainer.findElement(By.className("check-panel-status"));
        return new CheckPanelElement(panelElement);
    }

    /**
     * @return the panel concerning the conflict check.
     */
    public CheckPanelElement getConflictCheck()
    {
        WebElement panelElement = this.tabContainer.findElement(By.className("check-panel-conflict"));
        return new CheckPanelElement(panelElement);
    }

    /**
     * @return the panel concerning the strategy check.
     */
    public CheckPanelElement getStrategyCheck()
    {
        WebElement panelElement = this.tabContainer.findElement(By.className("check-panel-strategy"));
        return new CheckPanelElement(panelElement);
    }
}
