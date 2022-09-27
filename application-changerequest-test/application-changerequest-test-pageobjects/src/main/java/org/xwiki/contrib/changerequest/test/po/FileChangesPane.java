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
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.xwiki.livedata.test.po.LiveDataElement;
import org.xwiki.livedata.test.po.TableLayoutElement;
import org.xwiki.test.ui.po.BaseElement;
import org.xwiki.test.ui.po.diff.DocumentDiffSummary;
import org.xwiki.test.ui.po.diff.EntityDiff;
import org.xwiki.test.ui.po.editor.WYSIWYGEditPage;

/**
 * Represents the pane displaying the file changes in a change request.
 *
 * @version $Id$
 * @since 0.5
 */
public class FileChangesPane extends BaseElement
{
    private static final String FIX_CONFLICT_ACTION_CLASS = "action_fixconflict";
    private static final String REBASE_ACTION_CLASS = "action_rebase";

    private static final String EDIT_ACTION_CLASS = "action_edit";

    private static final String EDIT_APPROVERS_ACTION_CLASS = "action_editapprovers";
    private static final String LOCATION_COLUMN_NAME = "Location";

    private final WebElement container;

    /**
     * Represents the different change type displayed in the filechange live data.
     */
    public enum ChangeType
    {
        /**
         * When the change is a page creation.
         */
        CREATION,

        /**
         * When the change is a page edition.
         */
        EDITION,

        /**
         * When the change is a page deletion.
         */
        DELETION,

        /**
         * When there's no more change for the page.
         * Note that the UI is displaying "No change", so be careful to use {@link #relaxedValueOf(String)}.
         */
        NO_CHANGE;

        /**
         * An improved version of {@link #valueOf(String)} which automatically use an uppercase version of the argument
         * and which perform custom check for {@link #NO_CHANGE}.
         *
         * @param value the value for which to find a {@link ChangeType}.
         * @return a change type matching the given value.
         */
        public static ChangeType relaxedValueOf(String value)
        {
            String upperCaseValue = value.toUpperCase();
            if (StringUtils.equals("NO CHANGE", upperCaseValue)) {
                return NO_CHANGE;
            } else {
                return ChangeType.valueOf(upperCaseValue);
            }
        }
    }

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
        return new LiveDataElement("changerequest-filechanges");
    }

    private WebElement getDiffHeading(String serializedReference)
    {
        String cssSelector = String.format("div.panel-heading[data-documentreference='%s']", serializedReference);
        return getDriver().findElementWithoutWaiting(this.container, By.cssSelector(cssSelector));
    }

    private WebElement getDiffContainer(String serializedReference)
    {
        WebElement heading = getDiffHeading(serializedReference);
        WebElement link = getDriver().findElementWithoutWaiting(heading, By.tagName("a"));
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
     * @param serializedReference the reference of the page for which to display the summary.
     * @return the diff summary of the given page.
     */
    public DocumentDiffSummary getDiffSummary(String serializedReference)
    {
        WebElement diffContainer = this.getDiffContainer(serializedReference);
        return new DocumentDiffSummary(getDriver().findElementWithoutWaiting(diffContainer,
            By.className("diff-summary")));
    }

    /**
     * Get a specific entity diff of a given page.
     *
     * @param serializedReference the name of a page for which to get the diff.
     * @param label the label of the entity to get the diff.
     * @return the diff of the given entity.
     */
    public EntityDiff getEntityDiff(String serializedReference, String label)
    {
        WebElement diffContainer = this.getDiffContainer(serializedReference);
        return new EntityDiff(diffContainer.findElement(By
            .xpath("//dd[parent::dl[@class = 'diff-group'] and preceding-sibling::dt[normalize-space(.) = '" + label
                + "']]")));
    }

    private boolean isDiffLabelDisplayed(String serializedReference, String className)
    {
        WebElement diffHeading = getDiffHeading(serializedReference);
        try {
            WebElement labelOutdated =
                getDriver().findElementWithoutWaiting(diffHeading, By.className(className));
            return labelOutdated.isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    /**
     * Check if the diff corresponding to the given reference is outdated.
     *
     * @param serializedReference a compact serialization of a page reference.
     * @return {@code true} if the outdated label is displayed on the diff.
     */
    public boolean isDiffOutdated(String serializedReference)
    {
        return this.isDiffLabelDisplayed(serializedReference, "label-diff-outdated");
    }

    /**
     * Check if there's conflict between the changes of the change request concerned by the given reference, and the
     * published version of the same page.
     *
     * @param serializedReference a compact serialization of a page reference.
     * @return {@code true} if the conflict label is displayed on the diff.
     */
    public boolean isConflictLabelDisplayed(String serializedReference)
    {
        return this.isDiffLabelDisplayed(serializedReference, "label-diff-conflict");
    }

    private Optional<WebElement> getActionLink(String serializedReference, String actionClass)
    {
        LiveDataElement fileChangesListLiveData = this.getFileChangesListLiveData();
        TableLayoutElement tableLayout = fileChangesListLiveData.getTableLayout();
        tableLayout.filterColumn(LOCATION_COLUMN_NAME, serializedReference);
        WebElement actionsCell = tableLayout.getCell("Actions", 1);
        try {
            return Optional.of(getDriver().findElementWithoutWaiting(actionsCell, By.className(actionClass)));
        } catch (NoSuchElementException e) {
            return Optional.empty();
        }
    }

    /**
     * Check if the fix conflict link is displayed in the live data table for the given reference.
     *
     * @param serializedReference a compact serialization of a page reference.
     * @return {@code true} if the conflict link is displayed on the row of the reference.
     */
    public boolean isFixConflictActionAvailable(String serializedReference)
    {
        return this.getActionLink(serializedReference, FIX_CONFLICT_ACTION_CLASS).isPresent();
    }

    /**
     * Click on the fix conflict link of the given reference.
     *
     * @param serializedReference a compact serialization of a page reference.
     * @return an instance of the {@link ChangeRequestConflictModal} to manipulate the modal that should be opened.
     * @see #isFixConflictActionAvailable(String)
     */
    public ChangeRequestConflictModal clickFixConflict(String serializedReference)
    {
        Optional<WebElement> optionalActionLink = this.getActionLink(serializedReference, FIX_CONFLICT_ACTION_CLASS);
        if (optionalActionLink.isPresent()) {
            optionalActionLink.get().click();
            return new ChangeRequestConflictModal();
        } else {
            throw new NoSuchElementException(
                String.format("The fix conflict link is not available for [%s]", serializedReference));
        }
    }

    /**
     * Check if the refresh content link is displayed in the live data table for the given reference.
     *
     * @param serializedReference a compact serialization of a page reference.
     * @return {@code true} if the refresh content link is displayed on the row of the reference.
     */
    public boolean isRefreshActionAvailable(String serializedReference)
    {
        return this.getActionLink(serializedReference, REBASE_ACTION_CLASS).isPresent();
    }

    /**
     * Click on the refresh content link of the given reference, and wait for the reload of the page.
     *
     * @param serializedReference a compact serialization of a page reference.
     * @return a new instance of the {@link ChangeRequestPage} since this action reload the page.
     * @see #isRefreshActionAvailable(String)
     */
    public ChangeRequestPage clickRefresh(String serializedReference)
    {
        Optional<WebElement> optionalActionLink = this.getActionLink(serializedReference, REBASE_ACTION_CLASS);
        if (optionalActionLink.isPresent()) {
            getDriver().addPageNotYetReloadedMarker();
            optionalActionLink.get().click();
            getDriver().waitUntilPageIsReloaded();
            return new ChangeRequestPage();
        } else {
            throw new NoSuchElementException(
                String.format("The refresh content link is not available for [%s]", serializedReference));
        }
    }

    /**
     * Check if the edit link is displayed in the live data table for the given reference.
     *
     * @param serializedReference a compact serialization of a page reference.
     * @return {@code true} if the edit link is displayed on the row of the reference.
     */
    public boolean isEditActionAvailable(String serializedReference)
    {
        return this.getActionLink(serializedReference, EDIT_ACTION_CLASS).isPresent();
    }

    /**
     * Check if the edit approvers link is displayed.
     *
     * @param serializedReference the reference of the page for which to check if the link is there
     * @return {@code true} if there is a link to edit approvers.
     * @since 1.2
     */
    public boolean isEditApproversActionAvailable(String serializedReference)
    {
        return this.getActionLink(serializedReference, EDIT_APPROVERS_ACTION_CLASS).isPresent();
    }

    /**
     * Click on the refresh content link of the given reference, and wait for the reload of the page.
     *
     * @param serializedReference a compact serialization of a page reference.
     * @return a new instance of the {@link ChangeRequestPage} since this action reload the page.
     * @see #isRefreshActionAvailable(String)
     */
    public ExtendedEditPage<WYSIWYGEditPage> clickEdit(String serializedReference)
    {
        Optional<WebElement> optionalActionLink = this.getActionLink(serializedReference, EDIT_ACTION_CLASS);
        if (optionalActionLink.isPresent()) {
            optionalActionLink.get().click();
            ExtendedEditPage<WYSIWYGEditPage> extendedEditPage = new ExtendedEditPage<>(new WYSIWYGEditPage());
            extendedEditPage.getEditor().waitUntilPageIsReady();
            return extendedEditPage;
        } else {
            throw new NoSuchElementException(
                String.format("The edit link is not available for [%s]", serializedReference));
        }
    }

    /**
     * Click on the edit approvers link of a page, and wait for the save modal to open which will allow to manipulate
     * the list of approvers.
     *
     * @param serializedReference the concerned page for which  to click of edit approvers link
     * @return a save modal allowing to edit the approvers
     * @since 1.2
     */
    public ChangeRequestSaveModal clickEditApprovers(String serializedReference)
    {
        Optional<WebElement> optionalActionLink = this.getActionLink(serializedReference, EDIT_APPROVERS_ACTION_CLASS);
        if (optionalActionLink.isPresent()) {
            WebElement actionLink = optionalActionLink.get();
            // We need to ensure that the action link has been augmented to use the modal.
            getDriver().waitUntilCondition(driver -> actionLink.getAttribute("class").contains("js-augmented"));
            actionLink.click();
            return new ChangeRequestSaveModal();
        } else {
            throw new NoSuchElementException(
                String.format("The edit approvers link is not available for [%s]", serializedReference));
        }
    }

    /**
     * Retrieve the change type value displayed in the live data for the given reference.
     *
     * @param serializedReference a compact serialization of a page reference.
     * @return the {@link ChangeType} displayed in the live data.
     */
    public ChangeType getChangeType(String serializedReference)
    {
        LiveDataElement fileChangesListLiveData = this.getFileChangesListLiveData();
        TableLayoutElement tableLayout = fileChangesListLiveData.getTableLayout();
        tableLayout.filterColumn(LOCATION_COLUMN_NAME, serializedReference);
        WebElement changeType = tableLayout.getCell("Change type", 1);
        return ChangeType.relaxedValueOf(changeType.getText());
    }

    /**
     * @return the list of modified documents.
     */
    public List<String> getListOfChangedFiles()
    {
        TableLayoutElement tableLayout = this.getFileChangesListLiveData().getTableLayout();
        int rows = tableLayout.countRows();
        List<String> result = new ArrayList<>();
        for (int i = 1; i <= rows; i++) {
            result.add(tableLayout.getCell("Title", i).getText());
        }
        return result;
    }
}
