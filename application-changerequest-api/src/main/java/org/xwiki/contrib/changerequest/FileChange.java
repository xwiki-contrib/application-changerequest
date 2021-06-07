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
package org.xwiki.contrib.changerequest;

import java.util.Date;

import org.xwiki.model.reference.EntityReference;
import org.xwiki.stability.Unstable;
import org.xwiki.user.UserReference;

/**
 * Represents a single change made on a XWiki document.
 * This can be seen as a git commit, or as an XWiki save on a single document.
 *
 * @version $Id$
 * @since 0.1
 */
@Unstable
public class FileChange
{
    private String id;
    private EntityReference targetEntity;
    private String sourceVersion;
    private UserReference author;
    private Date creationDate;
    private String contentChange;

    /**
     * Default constructor.
     * @param id the unique identifier of this file change.
     */
    public FileChange(String id)
    {
        this.id = id;
        this.creationDate = new Date();
    }

    /**
     * @return the unique identifier of this file change.
     */
    public String getId()
    {
        return id;
    }

    /**
     * @return the entity that is subject of the modification made in this file change.
     */
    public EntityReference getTargetEntity()
    {
        return targetEntity;
    }

    /**
     * @param targetEntity the entity that is subject of the modification made in this file change.
     * @return the current instance.
     */
    public FileChange setTargetEntity(EntityReference targetEntity)
    {
        this.targetEntity = targetEntity;
        return this;
    }

    /**
     * @return the version of the document for which the proposed changes have been made.
     */
    public String getSourceVersion()
    {
        return sourceVersion;
    }

    /**
     * @param sourceVersion the version of the document for which the proposed changes have been made.
     * @return the current instance.
     */
    public FileChange setSourceVersion(String sourceVersion)
    {
        this.sourceVersion = sourceVersion;
        return this;
    }

    /**
     * @return the author of the changes made here.
     */
    public UserReference getAuthor()
    {
        return author;
    }

    /**
     * @param author the author of the changes made here.
     * @return the current instance.
     */
    public FileChange setAuthor(UserReference author)
    {
        this.author = author;
        return this;
    }

    /**
     * @return the actual content of the change.
     */
    public String getContentChange()
    {
        return contentChange;
    }

    /**
     * @param contentChange the actual content of the change.
     * @return the current instance.
     */
    public FileChange setContentChange(String contentChange)
    {
        this.contentChange = contentChange;
        return this;
    }
}
