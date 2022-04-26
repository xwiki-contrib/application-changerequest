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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
    private final LinkedList<ChangeRequestReview> reviews;
    private Date staleDate;

    /**
     * Default constructor.
     */
    public ChangeRequest()
    {
        this.creationDate = new Date();
        this.status = ChangeRequestStatus.DRAFT;
        this.fileChanges = new LinkedHashMap<>();
        this.authors = new HashSet<>();
        this.reviews = new LinkedList<>();
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
     * Retrieve the previous version of a filechange.
     *
     * @param fileChange the filechange for which to retrieve the previous version.
     * @return {@link Optional#empty()} if there's no filechange before, or an optional containing the previous
     *         filechange.
     * @since 0.11
     */
    public Optional<FileChange> getFileChangeImmediatelyBefore(FileChange fileChange)
    {
        Optional<FileChange> result = Optional.empty();
        if (this.fileChanges.containsKey(fileChange.getTargetEntity())) {
            Iterator<FileChange> fileChangeIterator =
                this.fileChanges.get(fileChange.getTargetEntity()).descendingIterator();

            while (fileChangeIterator.hasNext()) {
                if (fileChangeIterator.next().equals(fileChange)) {
                    break;
                }
            }
            if (fileChangeIterator.hasNext()) {
                result = Optional.of(fileChangeIterator.next());
            }
        }
        return result;
    }

    /**
     * Retrieve the previous version of a filechange containing an actual change, if the given filechange is of type
     * {@link org.xwiki.contrib.changerequest.FileChange.FileChangeType#NO_CHANGE}.
     *
     * @param fileChange the filechange for which to retrieve the latest previous version with an actual change.
     * @return {@link Optional#empty()} if there's no filechange with a real change before, or an optional containing
     *          the previous filechange. Note that if the given filechange already contains a change, it's returned
     *          immediately.
     * @since 0.11
     */
    public Optional<FileChange> getFileChangeWithChangeBefore(FileChange fileChange)
    {
        Optional<FileChange> result = Optional.empty();
        if (fileChange.getType() == FileChange.FileChangeType.NO_CHANGE) {
            Deque<FileChange> fileChangeDeque = this.fileChanges.get(fileChange.getTargetEntity());
            boolean foundCurrent = false;
            Iterator<FileChange> fileChangeIterator = fileChangeDeque.descendingIterator();
            while (fileChangeIterator.hasNext()) {
                FileChange possibleFileChange = fileChangeIterator.next();
                if (!foundCurrent && possibleFileChange.equals(fileChange)) {
                    foundCurrent = true;
                } else if (foundCurrent && possibleFileChange.getType() != FileChange.FileChangeType.NO_CHANGE) {
                    result = Optional.of(possibleFileChange);
                    break;
                }
            }
        } else {
            result = Optional.of(fileChange);
        }
        return result;
    }

    /**
     * @return all authors who were involved in the current change request.
     */
    public Set<UserReference> getAuthors()
    {
        return authors;
    }

    /**
     * @param authors the authors of the change request.
     * @return the current instance
     * @since 0.7
     */
    public ChangeRequest setAuthors(Set<UserReference> authors)
    {
        this.authors = authors;
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
     * @return the reviews related to this change request sorted in date descending order.
     * @since 0.4
     */
    public List<ChangeRequestReview> getReviews()
    {
        return reviews;
    }

    /**
     * Retrieve the latest review performed by the given author.
     *
     * @param reviewer the author who performs the review.
     * @return an empty optional if the author didn't perform any review, else the latest one.
     * @since 0.8
     */
    public Optional<ChangeRequestReview> getLatestReviewFrom(UserReference reviewer)
    {
        return getReviews().stream().filter(review -> reviewer.equals(review.getAuthor())).findFirst();
    }

    /**
     * Retrieve the latest review either performed by the given author, or on behalf of them.
     *
     * @param reviewer the author or the original approver of the review.
     * @return an empty optional if the author didn't perform any review, else the latest one.
     * @since 0.13
     */
    @Unstable
    public Optional<ChangeRequestReview> getLatestReviewFromOrOnBehalfOf(UserReference reviewer)
    {
        return getReviews().stream().filter(review -> {
            if (review.getOriginalApprover() == null) {
                return reviewer.equals(review.getAuthor());
            } else {
                return reviewer.equals(review.getOriginalApprover());
            }
        }).findFirst();
    }

    /**
     * Attach a new review to this change request. Note that the review are added on the head of the deque.
     * This method also automatically compute {@link ChangeRequestReview#isLastFromAuthor()} based on previous reviews
     * already added.
     *
     * @param review the review to be attached to the change request.
     * @return the current instance.
     * @since 0.4
     */
    public ChangeRequest addReview(ChangeRequestReview review)
    {
        Optional<ChangeRequestReview> optionalPrevious = getLatestReviewFrom(review.getAuthor());
        optionalPrevious.ifPresent(changeRequestReview -> changeRequestReview.setLastFromAuthor(false));
        this.reviews.addFirst(review);
        return this;
    }

    /**
     * @return the date when the change request has been notified as staled, or {@code null}.
     * @since 0.10
     */
    public Date getStaleDate()
    {
        return staleDate;
    }

    /**
     * Specify when the change request is notified as staled.
     * Note that this method should never be used externally, the value stored here won't be saved.
     *
     * @param staleDate the date when the change request has been notified as staled, or {@code null}.
     */
    public void setStaleDate(Date staleDate)
    {
        this.staleDate = staleDate;
    }

    /**
     * Allow to clone the current change request instance, without the file changes information, and with a new
     * creation date.
     *
     * @return a clone of the current instance, without the file changes information.
     * @since 0.7
     */
    public ChangeRequest cloneWithoutFileChanges()
    {
        return new ChangeRequest()
            .setTitle(this.title)
            .setStatus(this.status)
            .setCreator(this.creator)
            .setCreationDate(new Date())
            .setDescription(this.description)
            .setAuthors(this.authors);
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
