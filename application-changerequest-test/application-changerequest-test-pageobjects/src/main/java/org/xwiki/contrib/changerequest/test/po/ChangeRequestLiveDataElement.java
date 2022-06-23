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

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.xwiki.livedata.test.po.LiveDataElement;
import org.xwiki.livedata.test.po.TableLayoutElement;

/**
 * Represents the livedata containing list of change requests.
 *
 * @version $Id$
 * @since 0.14
 */
public class ChangeRequestLiveDataElement
{
    private static final String TITLE_COLUMN = "Title";
    private final TableLayoutElement tableLayoutElement;

    /**
     * Represents a row in the live data.
     *
     * @version $Id$
     * @since 0.14
     */
    public class ChangeRequestRowElement
    {
        private final int rowNumber;

        ChangeRequestRowElement(int rowNumber)
        {
            this.rowNumber = rowNumber;
        }

        /**
         * @return the title of the change request.
         */
        public String getTitle()
        {
            return ChangeRequestLiveDataElement.this.tableLayoutElement.getCell(TITLE_COLUMN, this.rowNumber).getText();
        }

        /**
         * @return the status of the change request.
         */
        public String getStatus()
        {
            return ChangeRequestLiveDataElement.this.tableLayoutElement.getCell("Status", this.rowNumber).getText();
        }

        /**
         * Click on the link of the change request displayed on the title.
         *
         * @return the change request page after browsing to it.
         */
        public ChangeRequestPage gotoChangeRequest()
        {
            ChangeRequestLiveDataElement.this.tableLayoutElement.getCell(TITLE_COLUMN, this.rowNumber).findElement(
                By.tagName("a")).click();
            return new ChangeRequestPage();
        }
    }

    /**
     * Default constructor.
     *
     * @param liveDataId the id of the livedata.
     */
    public ChangeRequestLiveDataElement(String liveDataId)
    {
        this.tableLayoutElement = new LiveDataElement(liveDataId).getTableLayout();
        this.tableLayoutElement.waitUntilReady();
    }

    /**
     * @return the number of rows.
     */
    public int countRows()
    {
        return this.tableLayoutElement.countRows();
    }

    /**
     * @return all the rows of the livedata.
     */
    public List<ChangeRequestRowElement> getChangeRequests()
    {
        List<ChangeRequestRowElement> result = new ArrayList<>();
        for (int i = 1; i <= this.countRows(); i++)
        {
            result.add(new ChangeRequestRowElement(i));
        }
        return result;
    }
}
