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
import org.xwiki.livedata.test.po.LiveDataElement;
import org.xwiki.test.ui.po.BaseElement;
import org.xwiki.test.ui.po.diff.DocumentDiffSummary;
import org.xwiki.test.ui.po.diff.EntityDiff;

/**
 * Represents the pane displaying the file changes in a change request.
 *
 * @version $Id$
 * @since 0.5
 */
public class FileChangesPane extends BaseElement
{
    private static final String CLASS = "class";
    private final WebElement container;

    /**
     * Default constructor.
     *
     * @param container the global container of the pane.
     */
    public FileChangesPane(WebElement container)
    {
        this.container = container;
    }

    /**
     * @return the live table with the list of file changes.
     */
    public LiveDataElement getFileChangesListLiveData()
    {
        LiveDataElement liveDataElement = new LiveDataElement("changerequest-filechanges");
        liveDataElement.getTableLayout().waitUntilReady();
        return liveDataElement;
    }

    private WebElement getDiffContainer(String pageName)
    {
        // FIXME: relying on the link is very fragile.
        WebElement link = this.container.findElement(By.linkText("Changes of " + pageName));
        String diffId = link.getAttribute("aria-controls");
        WebElement diffContainer = this.container.findElement(By.id(diffId));
        if (!diffContainer.isDisplayed()) {
            link.click();
            getDriver().waitUntilCondition(condition -> diffContainer.isDisplayed());
        }
        return diffContainer;
    }

    /**
     * Get the diff summary of a specific page name.
     * @param pageName the name of the page for which to display the summary.
     * @return the diff summary of the given page.
     */
    public DocumentDiffSummary getDiffSummary(String pageName)
    {
        WebElement diffContainer = this.getDiffContainer(pageName);
        return new DocumentDiffSummary(getDriver().findElementWithoutWaiting(diffContainer,
            By.className("diff-summary")));
    }

    /**
     * Get a specific entity diff of a given page.
     *
     * @param pageName the name of a page for which to get the diff.
     * @param label the label of the entity to get the diff.
     * @return the diff of the given entity.
     */
    public EntityDiff getEntityDiff(String pageName, String label)
    {
        WebElement diffContainer = this.getDiffContainer(pageName);
        return new EntityDiff(diffContainer.findElement(By
            .xpath("//dd[parent::dl[@class = 'diff-group'] and preceding-sibling::dt[normalize-space(.) = '" + label
                + "']]")));
    }
}
