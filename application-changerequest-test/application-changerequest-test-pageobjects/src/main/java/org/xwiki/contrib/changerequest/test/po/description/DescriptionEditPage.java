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

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.contrib.changerequest.test.po.ChangeRequestPage;
import org.xwiki.stability.Unstable;

/**
 * Change request page when the description edition is opened.
 *
 * @version $Id$
 * @since 0.8
 */
@Unstable
public class DescriptionEditPage extends ChangeRequestPage
{
    /**
     * Enter the description of the change request.
     * @param description the description to enter.
     */
    public void setDescription(String description)
    {
        WebElement content = getDriver().findElement(By.id("content"));
        content.clear();
        content.sendKeys(description);
    }

    /**
     * Save the description and reload the change request.
     * @return the change request page after it's reloaded.
     */
    public ChangeRequestPage saveDescription()
    {
        getDriver().addPageNotYetReloadedMarker();
        getDriver().findElement(By.id("save-description")).click();
        getDriver().waitUntilPageIsReloaded();
        return new ChangeRequestPage();
    }

    /**
     * Cancel the changes and reload the change request.
     * @return the change request page after it's reloaded.
     */
    public ChangeRequestPage cancelDescription()
    {
        getDriver().addPageNotYetReloadedMarker();
        getDriver().findElement(By.id("cancel-description")).click();
        getDriver().waitUntilPageIsReloaded();
        return new ChangeRequestPage();
    }
}
