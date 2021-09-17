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
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.WYSIWYGEditPage;

/**
 * Represents a view page on a wiki where Change Request is installed.
 *
 * @version $Id$
 * @since 0.5
 */
public class ExtendedViewPage extends ViewPage
{
    private static final String CR_EDIT_ID = "crEdit";

    /**
     * Check if the current view has a change request edit button: this button is normally visible only when users does
     * not have edit right, but has right to create a change request.
     * @return {@code true} if the change request edit button is visible, {@code false} otherwise.
     */
    public boolean hasChangeRequestEditButton()
    {
        return getDriver().hasElementWithoutWaiting(By.id(CR_EDIT_ID));
    }

    /**
     * Click on the change request edit button and returns the extended edit page that has been opened.
     *
     * @return a new instance of extended edit page.
     */
    public ExtendedEditPage<WYSIWYGEditPage> clickChangeRequestEdit()
    {
        getDriver().findElementById(CR_EDIT_ID).findElement(By.className("btn-default")).click();
        ExtendedEditPage<WYSIWYGEditPage> extendedEditPage = new ExtendedEditPage<>(new WYSIWYGEditPage());
        extendedEditPage.getEditor().waitUntilPageIsLoaded();
        return extendedEditPage;
    }
}
