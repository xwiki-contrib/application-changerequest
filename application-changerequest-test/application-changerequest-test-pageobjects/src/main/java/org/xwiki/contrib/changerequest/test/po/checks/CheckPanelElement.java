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

import org.openqa.selenium.WebElement;
import org.xwiki.contrib.changerequest.test.po.BootstrapCollapsedPanelElement;
import org.xwiki.stability.Unstable;

/**
 * Represents the panels used in the check tab.
 *
 * @version $Id$
 * @since 0.8
 */
@Unstable
public class CheckPanelElement extends BootstrapCollapsedPanelElement
{
    /**
     * Default constructor.
     *
     * @param checkPanelElement the element representing the panel.
     */
    public CheckPanelElement(WebElement checkPanelElement)
    {
        super(checkPanelElement);
    }

    /**
     * Verify if the check is successful or not.
     *
     * @return {@code true} if the panel is using info or success class.
     */
    public boolean isReady()
    {
        String classNames = this.getPanel().getAttribute("class");
        return classNames.contains("panel-info") || classNames.contains("panel-success");
    }
}
