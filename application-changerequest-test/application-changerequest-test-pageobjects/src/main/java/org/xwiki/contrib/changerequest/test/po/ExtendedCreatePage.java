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

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.CreatePagePage;
import org.xwiki.test.ui.po.editor.WYSIWYGEditPage;

/**
 * Page object to represents the create page template with some button possibly injected by change request.
 *
 * @version $Id$
 * @since 0.11
 */
public class ExtendedCreatePage extends CreatePagePage
{
    private static final String CR_CREATE_BUTTON_ID = "create_changerequest";

    /**
     * Default constructor.
     */
    public ExtendedCreatePage()
    {
    }

    /**
     * @return {@code true} if the standard create button is displayed.
     */
    public boolean hasStandardCreateButton()
    {
        WebElement formElement = getDriver().findElementWithoutWaiting(By.id("create"));
        WebElement submitButton =
            getDriver().findElementWithoutWaiting(formElement, By.cssSelector("input[type=submit]"));
        return !StringUtils.equals(submitButton.getAttribute("id"), CR_CREATE_BUTTON_ID);
    }

    /**
     * @return {@code true} if the change request create button is displayed.
     */
    public boolean hasChangeRequestCreateButton()
    {
        try {
            WebElement button = getDriver().findElementWithoutWaiting(By.id(CR_CREATE_BUTTON_ID));
            return button.isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    /**
     * Explicitely click on the change request create button and returns the opened editor.
     *
     * @return the wysiwyg editor opened after clicking on the create button.
     */
    public ExtendedEditPage<WYSIWYGEditPage> clickChangeRequestCreateButton()
    {
        WebElement button = getDriver().findElementWithoutWaiting(By.id(CR_CREATE_BUTTON_ID));
        button.click();
        ExtendedEditPage<WYSIWYGEditPage> extendedEditPage = new ExtendedEditPage<>(new WYSIWYGEditPage());
        extendedEditPage.getEditor().waitUntilPageIsLoaded();
        return extendedEditPage;
    }
}
