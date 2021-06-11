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
import java.util.List;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.stability.Unstable;
import org.xwiki.user.UserReference;

/**
 * Represents a request made by a user to perform a change in an XWiki document.
 *
 * @version $Id$
 * @since 0.1
 */
@Unstable
public class ChangeRequest
{
    private String id;
    private String title;
    private String description;
    private UserReference creator;
    private Date creationDate;
    private ChangeRequestStatus status;
    private List<DocumentReference> impactedFiles;
    private List<FileChange> fileChanges;

    /**
     * Default constructor.
     * @param id the unique identifier of the request change.
     */
    public ChangeRequest(String id)
    {
        this.id = id;
        this.creationDate = new Date();
        this.status = ChangeRequestStatus.DRAFT;
    }

    /**
     * @return the unique identifier of the request change.
     */
    public String getId()
    {
        return id;
    }

    /**
     * @return the user who created the request change in first place.
     */
    public UserReference getCreator()
    {
        return creator;
    }

    /**
     * @param creator the user who created the request change in first place.
     * @return the current instance.
     */
    public ChangeRequest setCreator(UserReference creator)
    {
        this.creator = creator;
        return this;
    }

    /**
     * @param impactedFiles the files that are modified in this change request.
     * @return the current instance.
     */
    public ChangeRequest setImpactedFiles(List<DocumentReference> impactedFiles)
    {
        this.impactedFiles = impactedFiles;
        return this;
    }

    /**
     * @param fileChanges the actual file changes that have been done as part of this change request.
     * @return the current instance.
     */
    public ChangeRequest setFileChanges(List<FileChange> fileChanges)
    {
        this.fileChanges = fileChanges;
        return this;
    }

    /**
     * @return the title of this change request.
     */
    public String getTitle()
    {
        return title;
    }

    /**
     * @param title the title of this change request.
     * @return the current instance.
     */
    public ChangeRequest setTitle(String title)
    {
        this.title = title;
        return this;
    }

    /**
     * @return a description of this change request.
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * @param description a description of this change request.
     * @return the current instance.
     */
    public ChangeRequest setDescription(String description)
    {
        this.description = description;
        return this;
    }

    /**
     * @return the actual file changes that have been done as part of this change request.
     */
    public List<FileChange> getFileChanges()
    {
        return fileChanges;
    }
}
