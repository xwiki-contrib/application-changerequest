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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.contrib.changerequest.test.po.discussion.DiscussionEditor;
import org.xwiki.contrib.changerequest.test.po.discussion.MessageElement;
import org.xwiki.test.ui.po.diff.EntityDiff;

/**
 * Augments the entity diff with the specific features for change request, mainly the capability to add comments.
 *
 * @version $Id$
 * @since 1.12
 */
public class ChangeRequestEntityDiff extends EntityDiff
{
    private static final String MESSAGE = "message";
    private final WebElement diffContainer;

    /**
     * Default constructor.
     * @param container the container of the entity diff.
     */
    public ChangeRequestEntityDiff(WebElement container)
    {
        super(container);

        this.diffContainer = container;
    }

    /**
     * @return {@code true} if this entity diff contains comments.
     */
    public boolean hasMessages()
    {
        return getDriver().hasElementWithoutWaiting(this.diffContainer, By.className(MESSAGE));
    }

    /**
     * @return the total number of comments.
     */
    public int countMessages()
    {
        return getDriver().findElementsWithoutWaiting(this.diffContainer, By.className(MESSAGE)).size();
    }

    /**
     * @return all messages found, organized by the line they are linked to.
     */
    public Map<EntityDiffCoordinate, List<MessageElement>> getAllMessages()
    {
        Map<EntityDiffCoordinate, List<MessageElement>> result = new HashMap<>();
        for (WebElement discussion : getDriver().findElementsWithoutWaiting(this.diffContainer,
            By.className("discussion"))) {

            int lineNumber = Integer.parseInt(discussion.getAttribute("data-linenumber"));
            LineChange lineChange = LineChange.valueOf(discussion.getAttribute("data-linechange"));
            String property = discussion.getAttribute("data-diffblockid");
            EntityDiffCoordinate coordinate = new EntityDiffCoordinate(property, lineChange, lineNumber);

            List<MessageElement> messages = new ArrayList<>();
            for (WebElement message : getDriver().findElementsWithoutWaiting(discussion, By.className(MESSAGE))) {
                messages.add(new MessageElement(message));
            }
            result.put(coordinate, messages);
        }

        return result;
    }

    /**
     * Click on the line column identified by the provided argument in order to add a new comment, and then return the
     * opened discussion editor.
     * Note that this method assumes that there's not an editor already opened.
     *
     * @param property the actual property diff (e.g. Content) where to add the comment
     * @param lineNumber the line number where to add the comment
     * @param lineType the type of line to distinguish the line numbers
     * @return an instance of {@link DiscussionEditor} corresponding to the opened editor to add a comment
     */
    public DiscussionEditor clickAddingDiffComment(String property, long lineNumber, LineChange lineType)
    {
        WebElement diffPropertyContainer = getDiffPropertyContainer(property);
        // XPath is computed like that:
        // It's a <td> element with a class "changerequest-commentable" which contains the given lineNumber
        // and which has a sibling td element containing a class depending on the type:
        //   * diff-line-context if it's context
        //   * diff-line-added if it's addition
        //   * diff-line-deleted if it's deletion

        String wantedClass;
        switch (lineType) {
            case UNCHANGED:
                wantedClass = "diff-line-context";
                break;

            case ADDED:
                wantedClass = "diff-line-added";
                break;

            case REMOVED:
                wantedClass = "diff-line-deleted";
                break;

            default:
                throw new IllegalArgumentException("Only context, addition or deletion type are supported.");
        }
        String xpath = String.format(".//td[@class='diff-line-number changerequest-commentable' "
            + "and normalize-space(.) = '%s' "
            + "and following-sibling::td[@class='diff-line %s']]", lineNumber, wantedClass);
        WebElement lineColumn = diffPropertyContainer.findElement(By.xpath(xpath));
        lineColumn.click();
        String editorXPath = ".//tr[@class='discussion-line discussion-editor']";
        return new DiscussionEditor(diffPropertyContainer.findElement(By.xpath(editorXPath)));
    }

    private WebElement getDiffPropertyContainer(String property)
    {
        String xpath = String.format(".//div[@class = 'diff-container' and parent::dd/preceding-sibling::dt[1][@class "
            + "= 'diff-header' and normalize-space(.) = '%s']]", property);
        return getDriver().findElementWithoutWaiting(this.diffContainer, By.xpath(xpath));
    }
}
