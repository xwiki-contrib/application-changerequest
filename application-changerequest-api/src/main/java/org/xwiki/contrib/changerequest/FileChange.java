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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.model.reference.DocumentReference;
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
    private final ChangeRequest changeRequest;
    private DocumentReference targetEntity;
    private String sourceVersion;
    private UserReference author;
    private Date creationDate;
    private DocumentModelBridge modifiedDocument;
    private boolean saved;

    /**
     * Default constructor.
     *
     * @param changeRequest the change request this file change belongs to.
     */
    public FileChange(ChangeRequest changeRequest)
    {
        this.changeRequest = changeRequest;
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
     * @param id the identifier of the file change.
     * @return the current instance.
     */
    public FileChange setId(String id)
    {
        this.id = id;
        return this;
    }

    /**
     * @return the entity that is subject of the modification made in this file change.
     */
    public DocumentReference getTargetEntity()
    {
        return targetEntity;
    }

    /**
     * @param targetEntity the entity that is subject of the modification made in this file change.
     * @return the current instance.
     */
    public FileChange setTargetEntity(DocumentReference targetEntity)
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
     * @return an instance of the document with the changes.
     */
    public DocumentModelBridge getModifiedDocument()
    {
        return modifiedDocument;
    }

    /**
     * @param modifiedDocument an instance of the document with the changes.
     * @return the current instance.
     */
    public FileChange setModifiedDocument(DocumentModelBridge modifiedDocument)
    {
        this.modifiedDocument = modifiedDocument;
        return this;
    }

    /**
     * @return the change request this file change belongs to.
     */
    public ChangeRequest getChangeRequest()
    {
        return changeRequest;
    }

    /**
     * @param creationDate the date of the creation of the file change.
     * @return the current instance.
     */
    public FileChange setCreationDate(Date creationDate)
    {
        this.creationDate = creationDate;
        return this;
    }

    /**
     * @return {@code true} if the changes has already been saved, {@code false otherwise}.
     */
    public boolean isSaved()
    {
        return saved;
    }

    /**
     * @param saved should be set to {@code true} if this file change requires to be saved.
     * @return the current instance.
     */
    public FileChange setSaved(boolean saved)
    {
        this.saved = saved;
        return this;
    }

    /**
     * @return the creation date of this file change.
     */
    public Date getCreationDate()
    {
        return creationDate;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        FileChange that = (FileChange) o;

        return new EqualsBuilder()
            .append(saved, that.saved)
            .append(id, that.id)
            .append(targetEntity, that.targetEntity)
            .append(sourceVersion, that.sourceVersion)
            .append(author, that.author)
            .append(creationDate, that.creationDate)
            .append(modifiedDocument, that.modifiedDocument)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(15, 13)
            .append(id)
            .append(targetEntity)
            .append(sourceVersion)
            .append(author)
            .append(creationDate)
            .append(modifiedDocument)
            .append(saved)
            .toHashCode();
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this)
            .append("id", id)
            .append("targetEntity", targetEntity)
            .append("sourceVersion", sourceVersion)
            .append("author", author)
            .append("creationDate", creationDate)
            .append("modifiedDocument", modifiedDocument)
            .append("saved", saved)
            .toString();
    }
}
