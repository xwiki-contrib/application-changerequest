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
package org.xwiki.contrib.changerequest.test.po.filechanges;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.xwiki.contrib.changerequest.test.po.ChangeRequestConflictModal;
import org.xwiki.contrib.changerequest.test.po.ChangeRequestPage;
import org.xwiki.contrib.changerequest.test.po.ChangeRequestSaveModal;
import org.xwiki.contrib.changerequest.test.po.ExtendedEditPage;
import org.xwiki.livedata.test.po.LiveDataElement;
import org.xwiki.livedata.test.po.TableLayoutElement;
import org.xwiki.test.ui.po.BaseElement;
import org.xwiki.test.ui.po.editor.WikiEditPage;

/**
 * Page object allowing to manipulate the live data of file changes.
 *
 * @version $Id$
 * @since 1.4.1
 */
public class FilechangesLiveDataElement extends BaseElement
{
    private static final String FIX_CONFLICT_ACTION_CLASS = "action_fixconflict";
    private static final String REBASE_ACTION_CLASS = "action_rebase";
    private static final String EDIT_ACTION_CLASS = "action_edit";
    private static final String EDIT_APPROVERS_ACTION_CLASS = "action_editapprovers";

    private static final String LOCATION_COLUMN = "Location";

    private final TableLayoutElement tableLayoutElement;

    /**
     * Page object representing a row in the live data of file changes.
     *
     * @version $Id$
     * @since 1.4.1
     */
    public class FilechangesRowElement
    {
        private final int rowNumber;

        FilechangesRowElement(int rowNumber)
        {
            this.rowNumber = rowNumber;
        }

        private WebElement getCell(String columnLabel)
        {
            return FilechangesLiveDataElement.this.tableLayoutElement.getCell(columnLabel, this.rowNumber);
        }

        private Optional<WebElement> getActionLink(String actionClass)
        {
            WebElement actionsCell = getCell("Actions");
            try {
                return Optional.of(getDriver().findElementWithoutWaiting(actionsCell, By.className(actionClass)));
            } catch (NoSuchElementException e) {
                return Optional.empty();
            }
        }

        /**
         * @return the title of the page
         */
        public String getTitle()
        {
            return getCell("Title").getText();
        }

        /**
         * @return the type of change
         */
        public ChangeType getChangeType()
        {
            WebElement changeType = getCell("Change type");
            return ChangeType.relaxedValueOf(changeType.getText());
        }

        /**
         * @return the location of the changed document
         */
        public String getLocation()
        {
            return getCell(LOCATION_COLUMN).getText();
        }

        /**
         * @return the serialized reference of the changed document
         */
        public String getReference()
        {
            return getDriver().findElementWithoutWaiting(getCell(LOCATION_COLUMN), By.className("breadcrumb"))
                .getAttribute("data-entity");
        }

        /**
         * @return the current version of the changed document
         */
        public String getVersion()
        {
            return getCell("Version").getText();
        }

        /**
         * @return the version of the published document
         */
        public String getPublishedDocumentVersion()
        {
            return getCell("Published document version").getText();
        }

        /**
         * Check if the fix conflict link is displayed in the live data table for the current row.
         *
         * @return {@code true} if the conflict link is displayed on the row of the reference.
         */
        public boolean isFixConflictActionAvailable()
        {
            return this.getActionLink(FIX_CONFLICT_ACTION_CLASS).isPresent();
        }

        /**
         * Click on the fix conflict link of the current row.
         *
         * @return an instance of the {@link ChangeRequestConflictModal} to manipulate the modal that should be opened.
         * @see #isFixConflictActionAvailable()
         */
        public ChangeRequestConflictModal clickFixConflict()
        {
            Optional<WebElement> optionalActionLink = this.getActionLink(FIX_CONFLICT_ACTION_CLASS);
            if (optionalActionLink.isPresent()) {
                optionalActionLink.get().click();
                return new ChangeRequestConflictModal();
            } else {
                throw new NoSuchElementException(
                    String.format("The fix conflict link is not available for row [%s]", this.rowNumber));
            }
        }

        /**
         * Check if the refresh content link is displayed in the live data table for the current row.
         *
         * @return {@code true} if the refresh content link is displayed on the row of the reference.
         */
        public boolean isRefreshActionAvailable()
        {
            return this.getActionLink(REBASE_ACTION_CLASS).isPresent();
        }

        /**
         * Click on the refresh content link of the current row, and wait for the reload of the page.
         *
         * @return a new instance of the {@link ChangeRequestPage} since this action reload the page.
         * @see #isRefreshActionAvailable()
         */
        public ChangeRequestPage clickRefresh()
        {
            Optional<WebElement> optionalActionLink = this.getActionLink(REBASE_ACTION_CLASS);
            if (optionalActionLink.isPresent()) {
                getDriver().addPageNotYetReloadedMarker();
                optionalActionLink.get().click();
                getDriver().waitUntilPageIsReloaded();
                return new ChangeRequestPage();
            } else {
                throw new NoSuchElementException(
                    String.format("The refresh content link is not available for row [%s]", this.rowNumber));
            }
        }

        /**
         * Check if the edit link is displayed in the live data table for the current row.
         *
         * @return {@code true} if the edit link is displayed on the row of the reference.
         */
        public boolean isEditActionAvailable()
        {
            return this.getActionLink(EDIT_ACTION_CLASS).isPresent();
        }

        /**
         * Check if the edit approvers link is displayed.
         *
         * @return {@code true} if there is a link to edit approvers.
         */
        public boolean isEditApproversActionAvailable()
        {
            return this.getActionLink(EDIT_APPROVERS_ACTION_CLASS).isPresent();
        }

        /**
         * Click on the refresh content link of the current row, and wait for the reload of the page.
         *
         * @return a new instance of the {@link ChangeRequestPage} since this action reload the page.
         * @see #isRefreshActionAvailable()
         */
        public ExtendedEditPage<WikiEditPage> clickEdit()
        {
            Optional<WebElement> optionalActionLink = this.getActionLink(EDIT_ACTION_CLASS);
            if (optionalActionLink.isPresent()) {
                optionalActionLink.get().click();
                ExtendedEditPage<WikiEditPage> extendedEditPage = new ExtendedEditPage<>(new WikiEditPage());
                extendedEditPage.getWrappedEditor().waitUntilPageIsReady();
                return extendedEditPage;
            } else {
                throw new NoSuchElementException(
                    String.format("The edit link is not available for row [%s]", this.rowNumber));
            }
        }

        /**
         * Click on the edit approvers link of a page, and wait for the save modal to open which will allow to
         * manipulate the list of approvers.
         *
         * @return a save modal allowing to edit the approvers
         */
        public ChangeRequestSaveModal clickEditApprovers()
        {
            Optional<WebElement> optionalActionLink = this.getActionLink(EDIT_APPROVERS_ACTION_CLASS);
            if (optionalActionLink.isPresent()) {
                WebElement actionLink = optionalActionLink.get();
                // We need to ensure that the action link has been augmented to use the modal.
                getDriver().waitUntilCondition(driver -> actionLink.getAttribute("class").contains("js-augmented"));
                actionLink.click();
                return new ChangeRequestSaveModal();
            } else {
                throw new NoSuchElementException(
                    String.format("The edit approvers link is not available for row [%s]"));
            }
        }
    }

    /**
     * Default constructor.
     *
     * @param liveDataId the identifier of the live data.
     */
    public FilechangesLiveDataElement(String liveDataId)
    {
        this.tableLayoutElement = new LiveDataElement(liveDataId).getTableLayout();
        this.tableLayoutElement.waitUntilReady();
    }

    /**
     * @return the number of rows of the live data.
     */
    public int countRows()
    {
        return this.tableLayoutElement.countRows();
    }

    /**
     * @return the list of all file changes.
     */
    public List<FilechangesRowElement> getFileChanges()
    {
        List<FilechangesRowElement> result = new ArrayList<>();
        for (int i = 1; i <= this.countRows(); i++) {
            result.add(new FilechangesRowElement(i));
        }
        return result;
    }

    /**
     * Retrieve the {@link FilechangesRowElement} for the given reference. If there's no match then the method will
     * throw a {@link NoSuchElementException}.
     *
     * @param serializedReference the reference to look for in the live data
     * @return the row corresponding to the searched reference
     */
    public FilechangesRowElement getFileChangeWithReference(String serializedReference)
    {
        Optional<FilechangesRowElement> result =
            getFileChanges().stream().filter(item -> serializedReference.equals(item.getReference())).findAny();
        if (result.isEmpty()) {
            throw new NoSuchElementException(String.format("Cannot find row for [%s]", serializedReference));
        } else {
            return result.get();
        }
    }
}
