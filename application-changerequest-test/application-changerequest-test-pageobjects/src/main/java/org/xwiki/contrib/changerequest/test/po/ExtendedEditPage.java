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
import org.xwiki.test.ui.po.BaseElement;

/**
 * Represents an edit page with the capability to save as change request.
 *
 * @param <T> the type of the editor used with change request capability.
 *
 * @version $Id$
 * @since 0.5
 */
public class ExtendedEditPage<T> extends BaseElement
{
    private static final String SAVE_CHANGE_REQUEST_ID = "save_changerequest";
    private static final String STANDARD_SAVE_NAME = "action_save";

    private T editor;

    /**
     * Default constructor.
     * @param editor the actual editor page used.
     */
    public ExtendedEditPage(T editor)
    {
        this.editor = editor;
    }

    /**
     * @return the actual editor used.
     */
    public T getEditor()
    {
        return this.editor;
    }

    /**
     * Check if the button to save as change request is present.
     * @return {@code true} if the button is present.
     */
    public boolean hasSaveAsChangeRequestButton()
    {
        return this.getDriver().hasElementWithoutWaiting(By.id(SAVE_CHANGE_REQUEST_ID));
    }

    /**
     * Check if the standard save button is displayed.
     * @return {@code true} if the button is present and displayed.
     */
    public boolean hasStandardSaveButton()
    {
        return this.getDriver().hasElementWithoutWaiting(By.name(STANDARD_SAVE_NAME))
            && getDriver().findElementWithoutWaiting(By.name(STANDARD_SAVE_NAME)).isDisplayed();
    }

    /**
     * Click on the save as change request button: this will open the change request save modal.
     * @return a new instance of change request save modal.
     */
    public ChangeRequestSaveModal clickSaveAsChangeRequest()
    {
        this.getDriver().findElement(By.id(SAVE_CHANGE_REQUEST_ID)).click();
        return new ChangeRequestSaveModal();
    }
}
