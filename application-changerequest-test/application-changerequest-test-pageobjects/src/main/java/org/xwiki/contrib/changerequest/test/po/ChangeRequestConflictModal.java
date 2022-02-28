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
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;
import org.xwiki.test.ui.po.BaseModal;
import org.xwiki.test.ui.po.diff.EntityDiff;

/**
 * Page object representing the conflict modal opened to fix a conflict.
 * This page object is greatly inspired from {@code EditConflictModal} since the modal UI is inspired from the
 * actual XWiki Standard edition conflict modal.
 *
 * @version $Id$
 * @since 0.10
 */
public class ChangeRequestConflictModal extends BaseModal
{
    /**
     * The different choices available to resolve the conflict.
     */
    public enum ResolutionChoice
    {
        /**
         * Choice to keep the change request version.
         */
        KEEP_CHANGE_REQUEST,

        /**
         * Choice to keep the published version.
         */
        KEEP_PUBLISHED,

        /**
         * Custom conflict fix choice.
         */
        CUSTOM
    }

    /**
     * The available versions for performing a diff.
     */
    public enum AvailableDiffVersions
    {
        /**
         * Version when the user started to edit.
         */
        PREVIOUS,

        /**
         * Version with current changes.
         */
        CURRENT,

        /**
         * Latest version saved.
         */
        NEXT,

        /**
         * Version with merge of latest saved changes and current changes.
         */
        MERGED
    }

    private static final String MODAL_ID = "changeRequestConflictModal";

    private static final String ATTRIBUTE_VALUE = "value";

    @FindBy(id = "actionForceSaveRadio")
    private WebElement forceSaveChoice;

    @FindBy(id = "actionMergeRadio")
    private WebElement mergeChoice;

    @FindBy(id = "actionCustomRadio")
    private WebElement customChoice;

    @FindBy(id = "changeRequestConflictCancelButton")
    private WebElement cancelButton;

    @FindBy(id = "changeRequestConflictSubmitButton")
    private WebElement submitButton;

    @FindBy(id = "changescontent")
    private WebElement diffContainer;

    @FindBy(css = "select[name=original]")
    private WebElement originalSelect;

    @FindBy(css = "select[name=revised]")
    private WebElement revisedSelect;

    @FindBy(id = "previewDiffChangeDiff")
    private WebElement viewDiff;

    @FindBy(css = "input[name=warningConflictAction]:checked")
    private WebElement currentlySelectedOption;

    /**
     * Default constructor, wait for the modal to be visible.
     */
    public ChangeRequestConflictModal()
    {
        super(By.id(MODAL_ID));
    }

    /**
     * Cancel the modal and wait for it to be closed.
     */
    public void cancelModal()
    {
        this.cancelButton.click();
        try {
            this.waitForClosed();
        } catch (StaleElementReferenceException e) {
            // the JS remove the modal so the element might be stale
        }
    }

    /**
     * Chose an option among the conflict resolution options.
     * @param choice the choice to make.
     * @return a new EditConflictModal since it reloads the diff.
     */
    public ChangeRequestConflictModal makeChoice(ResolutionChoice choice)
    {
        switch (choice) {
            case KEEP_CHANGE_REQUEST:
                this.mergeChoice.click();
                break;

            case KEEP_PUBLISHED:
                this.forceSaveChoice.click();
                break;

            case CUSTOM:
                this.customChoice.click();
                break;

            default:
                return this;
        }
        try {
            this.waitForClosed();
        } catch (StaleElementReferenceException e) {
        }

        return new ChangeRequestConflictModal();
    }

    /**
     * @param choice the choice for which to check if the option is available.
     * @return {@code true} if the option is available.
     */
    public boolean isOptionAvailable(ResolutionChoice choice)
    {
        try {
            switch (choice) {
                case KEEP_CHANGE_REQUEST:
                    return this.mergeChoice.isDisplayed();

                case KEEP_PUBLISHED:
                    return this.forceSaveChoice.isDisplayed();

                case CUSTOM:
                    return this.customChoice.isDisplayed();

                default:
                    return false;
            }
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    @Override
    public void close()
    {
        try {
            super.close();
        } catch (StaleElementReferenceException e) {
            // the JS remove the modal so the element might be stale
        }
    }

    /**
     * @return the {@link EntityDiff} corresponding to the diff contained in the modal.
     */
    public EntityDiff getDiff()
    {
        return new EntityDiff(this.diffContainer);
    }

    /**
     * @return the previous version used for displaying the diff.
     */
    public AvailableDiffVersions getOriginalDiffVersion()
    {
        Select select = new Select(this.originalSelect);
        return AvailableDiffVersions.valueOf(select.getFirstSelectedOption().getAttribute(ATTRIBUTE_VALUE));
    }

    /**
     * @return the next version used for displaying the diff.
     */
    public AvailableDiffVersions getRevisedDiffVersion()
    {
        Select select = new Select(this.revisedSelect);
        return AvailableDiffVersions.valueOf(select.getFirstSelectedOption().getAttribute(ATTRIBUTE_VALUE));
    }

    /**
     * Perform a diff between two custom versions.
     * @param previous the previous version to compare with.
     * @param next the next version to compare with.
     * @return a new EditConflictModal with the appropriate diff.
     */
    public ChangeRequestConflictModal changeDiff(AvailableDiffVersions previous, AvailableDiffVersions next)
    {
        Select selectOriginal = new Select(this.originalSelect);
        Select selectRevised = new Select(this.revisedSelect);
        selectOriginal.selectByValue(previous.name());
        selectRevised.selectByValue(next.name());
        this.viewDiff.click();
        try {
            this.waitForClosed();
        } catch (StaleElementReferenceException e) {
        }

        return new ChangeRequestConflictModal();
    }

    /**
     * @return the currently selected option.
     */
    public ResolutionChoice getCurrentChoice()
    {
        String attribute = this.currentlySelectedOption.getAttribute(ATTRIBUTE_VALUE);
        ResolutionChoice result;
        if (StringUtils.equalsIgnoreCase("keepchangerequest", attribute)) {
            result = ResolutionChoice.KEEP_CHANGE_REQUEST;
        } else if (StringUtils.equalsIgnoreCase("keeppublished", attribute)) {
            result = ResolutionChoice.KEEP_PUBLISHED;
        } else if (StringUtils.equalsIgnoreCase("custom", attribute)) {
            result = ResolutionChoice.CUSTOM;
        } else {
            throw new IllegalArgumentException(String.format("Unknown choice type: [%s]", attribute));
        }
        return result;
    }

    /**
     * Submit the current choice and wait for the page to be reloaded.
     *
     * @return a new instance of the {@link ChangeRequestPage} since it's reloaded.
     */
    public ChangeRequestPage submitCurrentChoice()
    {
        getDriver().addPageNotYetReloadedMarker();
        this.submitButton.click();
        getDriver().waitUntilPageIsReloaded();
        return new ChangeRequestPage();
    }

    /**
     * @return {@code true} if the options to change the diff is available.
     */
    public boolean isPreviewDiffOptionsAvailable()
    {
        return getDriver().hasElementWithoutWaiting(By.className("previewdiff-diff-options"));
    }
}
