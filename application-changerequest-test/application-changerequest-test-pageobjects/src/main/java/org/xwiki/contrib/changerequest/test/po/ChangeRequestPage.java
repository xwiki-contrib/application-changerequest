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
import org.xwiki.contrib.changerequest.test.po.checks.ChecksPane;
import org.xwiki.contrib.changerequest.test.po.description.DescriptionPane;
import org.xwiki.contrib.changerequest.test.po.reviews.ReviewsPane;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents a change request page.
 *
 * @version $Id$
 * @since 0.5
 */
public class ChangeRequestPage extends ViewPage
{
    /**
     * @return the actual label describing the status of the change request.
     */
    public String getStatusLabel()
    {
        return getDriver().findElement(By.className("document-info")).findElement(By.className("label")).getText();
    }

    private WebElement openTab(String tabname)
    {
        WebElement tab = getDriver().findElement(By.id(tabname));
        if (!tab.getAttribute("class").contains("active")) {
            getDriver().findElement(By.cssSelector(String.format("a[aria-controls=%s]", tabname))).click();
        }
        return tab;
    }

    /**
     * Open the description tab and returns it.
     *
     * @return a {@link DescriptionPane} to see the description, timeline and comments.
     */
    public DescriptionPane openDescription()
    {
        WebElement description = this.openTab("home");
        return new DescriptionPane(description);
    }

    /**
     * Open the file changes tab and returns it.
     *
     * @return a {@link FileChangesPane} to navigate in the file changes.
     */
    public FileChangesPane openFileChanges()
    {
        WebElement filechanges = this.openTab("filechanges");
        return new FileChangesPane(filechanges);
    }

    /**
     * Open the reviews tab and returns it.
     *
     * @return a {@link ReviewsPane} to list approvers and reviews.
     */
    public ReviewsPane openReviewsPane()
    {
        WebElement reviews = this.openTab("reviews");
        return new ReviewsPane(reviews);
    }

    /**
     * Open the checks tab and returns it.
     *
     * @return a {@link ChecksPane} to allow verifying the checks.
     */
    public ChecksPane openChecksPane()
    {
        WebElement checks = this.openTab("checks");
        return new ChecksPane(checks);
    }


}
