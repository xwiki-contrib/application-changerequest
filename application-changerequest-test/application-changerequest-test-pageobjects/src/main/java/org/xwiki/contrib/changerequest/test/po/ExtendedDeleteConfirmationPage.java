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
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.DeletePageConfirmationPage;

/**
 * A page object extending the {@link DeletePageConfirmationPage} to represent the injected buttons by change request
 * to request for deletion.
 *
 * @version $Id$
 * @since 0.10
 */
public class ExtendedDeleteConfirmationPage extends DeletePageConfirmationPage
{
    private static final String CR_DELETE_BUTTON_ID = "delete_changerequest";

    /**
     * Default constructor.
     */
    public ExtendedDeleteConfirmationPage()
    {
    }

    /**
     * @return {@code true} if the standard delete confirmation button is displayed.
     */
    public boolean hasDeleteButton()
    {
        try {
            WebElement confirm = getDriver().findElementWithoutWaiting(By.cssSelector("button.btn-danger.confirm"));
            return confirm.isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    /**
     * @return {@code true} if the request for deletion confirmation button is displayed.
     */
    public boolean hasChangeRequestDeleteButton()
    {
        try {
            WebElement confirm = getDriver().findElementWithoutWaiting(By.id(CR_DELETE_BUTTON_ID));
            return confirm.isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    /**
     * Click on the request for deletion confirmation button.
     *
     * @return a new instance of {@link ChangeRequestSaveModal} since this one is supposed to open in such case.
     * @see #hasChangeRequestDeleteButton()
     */
    public ChangeRequestSaveModal clickChangeRequestDelete()
    {
        WebElement crDeleteButton = getDriver().findElementWithoutWaiting(By.id(CR_DELETE_BUTTON_ID));
        crDeleteButton.click();
        return new ChangeRequestSaveModal();
    }
}
