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

import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
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
    private final Map<DocumentReference, Deque<FileChange>> fileChanges;
    private Set<UserReference> authors;
    private List<ChangeRequestReview> reviews;

    /**
     * Default constructor.
     */
    public ChangeRequest()
    {
        this.creationDate = new Date();
        this.status = ChangeRequestStatus.DRAFT;
        this.fileChanges = new HashMap<>();
        this.authors = new HashSet<>();
        this.reviews = new ArrayList<>();
    }

    /**
     * @return the unique identifier of the request change.
     */
    public String getId()
    {
        return id;
    }

    /**
     * @param id the identifier of the change request.
     * @return the current instance.
     */
    public ChangeRequest setId(String id)
    {
        this.id = id;
        return this;
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
     * Insert the given file change in the map of file changes.
     *
     * @param fileChange a file change that have been done as part of this change request.
     * @return the current instance.
     */
    public ChangeRequest addFileChange(FileChange fileChange)
    {
        DocumentReference documentReference = fileChange.getTargetEntity();
        synchronized (this.fileChanges) {
            Deque<FileChange> fileChangeList;
            if (this.fileChanges.containsKey(documentReference)) {
                fileChangeList = this.fileChanges.get(documentReference);
            } else {
                fileChangeList = new LinkedList<>();
                this.fileChanges.put(documentReference, fileChangeList);
            }
            this.authors.add(fileChange.getAuthor());
            fileChangeList.add(fileChange);
        }
        return this;
    }

    /**
     * @return the set of document modified as part of this change request.
     * @since 0.4
     */
    public Set<DocumentReference> getModifiedDocuments()
    {
        return this.fileChanges.keySet();
    }

    /**
     * Retrieve last file changes for any document modified in the change request.
     * @return last file changes for each document.
     * @since 0.4
     */
    public List<FileChange> getLastFileChanges()
    {
        List<FileChange> result = new ArrayList<>();
        for (Map.Entry<DocumentReference, Deque<FileChange>> entry : this.fileChanges.entrySet()) {
            result.add(entry.getValue().getLast());
        }
        return result;
    }

    /**
     * @return all file changes of the current change request.
     */
    public List<FileChange> getAllFileChanges()
    {
        return new ArrayList<>(this.fileChanges
            .values()
            .stream()
            .reduce(new LinkedList<>(), (list1, list2) -> {
                list1.addAll(list2);
                return list1;
            }));
    }

    /**
     * @return all authors who were involved in the current change request.
     */
    public Set<UserReference> getAuthors()
    {
        return authors;
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
    public Map<DocumentReference, Deque<FileChange>> getFileChanges()
    {
        return fileChanges;
    }

    /**
     * Automatically retrieve  the latest {@link FileChange} for the given document reference.
     *
     * @param documentReference the reference for which to retrieve latest file change.
     * @return {@link Optional#empty()} if there's no file change for this reference, else returns an optional with
     *          latest file change in the list.
     * @since 0.4
     */
    public Optional<FileChange> getLatestFileChangeFor(DocumentReference documentReference)
    {
        Optional<FileChange> result = Optional.empty();
        Deque<FileChange> fileChangeList = this.fileChanges.getOrDefault(documentReference, new LinkedList<>());
        if (!fileChangeList.isEmpty()) {
            result = Optional.of(fileChangeList.getLast());
        }
        return result;
    }

    /**
     * @param status the current status of the change request.
     * @return the current instance.
     */
    public ChangeRequest setStatus(ChangeRequestStatus status)
    {
        this.status = status;
        return this;
    }

    /**
     * @return the current status of the change request.
     */
    public ChangeRequestStatus getStatus()
    {
        return status;
    }

    /**
     * @return the date of the creation of the change request.
     */
    public Date getCreationDate()
    {
        return creationDate;
    }

    /**
     * @param creationDate the date of the creation of the change request.
     * @return the current instance.
     */
    public ChangeRequest setCreationDate(Date creationDate)
    {
        this.creationDate = creationDate;
        return this;
    }

    /**
     * @return the reviews related to this change request.
     * @since 0.4
     */
    public List<ChangeRequestReview> getReviews()
    {
        return reviews;
    }

    /**
     * Attach a new review to this change request.
     *
     * @param review the review to be attached to the change request.
     * @return the current instance.
     * @since 0.4
     */
    public ChangeRequest addReview(ChangeRequestReview review)
    {
        this.reviews.add(review);
        return this;
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

        ChangeRequest that = (ChangeRequest) o;

        return new EqualsBuilder()
            .append(id, that.id)
            .append(title, that.title)
            .append(description, that.description)
            .append(creator, that.creator)
            .append(creationDate, that.creationDate)
            .append(status, that.status)
            .append(fileChanges, that.fileChanges)
            .append(reviews, that.reviews)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37)
            .append(id)
            .append(title)
            .append(description)
            .append(creator)
            .append(creationDate)
            .append(status)
            .append(fileChanges)
            .append(reviews)
            .toHashCode();
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this)
            .append("id", id)
            .append("title", title)
            .append("description", description)
            .append("creator", creator)
            .append("creationDate", creationDate)
            .append("status", status)
            .append("fileChanges", fileChanges)
            .append("review", reviews)
            .toString();
    }
}
