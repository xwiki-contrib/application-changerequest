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
    /**
     * Define the type of modification the change is about.
     *
     * @since 0.5
     */
    public enum FileChangeType
    {
        /**
         * The change concerns the creation of a new document.
         */
        CREATION,

        /**
         * The change concerns the modification of an existing document.
         */
        EDITION,

        /**
         * The change concerns the deletion of a document.
         */
        DELETION
    };

    /**
     * Prefix used for storing the filechange version.
     */
    public static final String FILECHANGE_VERSION_PREFIX = "filechange-";

    private String id;
    private final ChangeRequest changeRequest;
    private DocumentReference targetEntity;
    private String previousVersion;
    private String previousPublishedVersion;
    private String version;
    private UserReference author;
    private Date creationDate;
    private DocumentModelBridge modifiedDocument;
    private boolean saved;
    private FileChangeType type;

    /**
     * Creates a new file change edition related to the given change request.
     *
     * @param changeRequest the change request this file change belongs to.
     */
    public FileChange(ChangeRequest changeRequest)
    {
        this(changeRequest, FileChangeType.EDITION);
    }

    /**
     * Default constructor.
     *
     * @param changeRequest the change request this file change belongs to.
     * @param type the type of change.
     * @since 0.5
     */
    public FileChange(ChangeRequest changeRequest, FileChangeType type)
    {
        this.changeRequest = changeRequest;
        this.creationDate = new Date();
        this.type = type;
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
     * The previous version is the version of the document from which the modifications have been made: it can be
     * either the version of a published document, or the version of a previous file change, in which case the version
     * is prefixed by {@link #FILECHANGE_VERSION_PREFIX}.
     * Note that this version should not be used for performing a merge to publish the file change: for this usecase,
     * the {@link #getPreviousPublishedVersion()} should be used since it guarantees that it's a published version.
     *
     * @return the version from which the changes have been made.
     */
    public String getPreviousVersion()
    {
        return previousVersion;
    }

    /**
     * See {@link #getPreviousVersion()} for details and distinction between this method and
     * {@link #setPreviousPublishedVersion(String)}.
     *
     * @param previousVersion the version from which the changes have been made.
     * @return the current instance.
     */
    public FileChange setPreviousVersion(String previousVersion)
    {
        this.previousVersion = previousVersion;
        return this;
    }

    /**
     * The previous published version is the published version of document for which the changes have been made.
     * This is the version that should be used for merging the changes to publish them.
     *
     * @return the version of the document for which the changes have been made.
     */
    public String getPreviousPublishedVersion()
    {
        return previousPublishedVersion;
    }

    /**
     * See {@link #getPreviousVersion()} and {@link #getPreviousPublishedVersion()}.
     *
     * @param previousPublishedVersion the version of the document for which the changes have been made.
     * @return the current instance.
     */
    public FileChange setPreviousPublishedVersion(String previousPublishedVersion)
    {
        this.previousPublishedVersion = previousPublishedVersion;
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

    /**
     * @return the version of this filechange.
     * @since 0.4
     */
    public String getVersion()
    {
        return version;
    }

    /**
     * Set the version of this filechange.
     * Note that this method only stores the version and does not set the {@link #FILECHANGE_VERSION_PREFIX}.
     * @param version the version to set.
     * @return the current instance.
     * @since 0.4
     */
    public FileChange setVersion(String version)
    {
        this.version = version;
        return this;
    }

    /**
     * @return the type of this file change.
     * @since 0.5
     */
    public FileChangeType getType()
    {
        return type;
    }

    /**
     * Clone the current file change with all data, except the id (see {@link #getId()}).
     * Note that the {@link #isSaved()} flag returns false for the created element of the clone.
     *
     * @return a cloned instance of the file change.
     * @since 0.6
     */
    public FileChange clone()
    {
        return new FileChange(this.changeRequest, this.type)
            .setVersion(this.version)
            .setCreationDate(this.creationDate)
            .setAuthor(this.author)
            .setModifiedDocument(this.modifiedDocument)
            .setTargetEntity(this.targetEntity)
            .setPreviousPublishedVersion(this.previousPublishedVersion)
            .setPreviousVersion(this.previousVersion);
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
            .append(previousVersion, that.previousVersion)
            .append(author, that.author)
            .append(creationDate, that.creationDate)
            .append(modifiedDocument, that.modifiedDocument)
            .append(version, that.version)
            .append(type, that.type)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(15, 13)
            .append(id)
            .append(targetEntity)
            .append(previousVersion)
            .append(author)
            .append(creationDate)
            .append(modifiedDocument)
            .append(saved)
            .append(version)
            .append(type)
            .toHashCode();
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this)
            .append("id", id)
            .append("targetEntity", targetEntity)
            .append("sourceVersion", previousVersion)
            .append("version", version)
            .append("author", author)
            .append("creationDate", creationDate)
            .append("modifiedDocument", modifiedDocument)
            .append("saved", saved)
            .append("type", type)
            .toString();
    }
}
