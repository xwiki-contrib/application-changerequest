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
package org.xwiki.contrib.changerequest.internal.storage;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.ChangeRequestReview;
import org.xwiki.contrib.changerequest.internal.UserReferenceConverter;
import org.xwiki.contrib.changerequest.storage.ReviewStorageManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.xwiki.contrib.changerequest.internal.storage.ReviewXClassInitializer.APPROVED_PROPERTY;
import static org.xwiki.contrib.changerequest.internal.storage.ReviewXClassInitializer.AUTHOR_PROPERTY;
import static org.xwiki.contrib.changerequest.internal.storage.ReviewXClassInitializer.DATE_PROPERTY;
import static org.xwiki.contrib.changerequest.internal.storage.ReviewXClassInitializer.ORIGINAL_APPROVER_PROPERTY;
import static org.xwiki.contrib.changerequest.internal.storage.ReviewXClassInitializer.REVIEW_XCLASS;
import static org.xwiki.contrib.changerequest.internal.storage.ReviewXClassInitializer.VALID_PROPERTY;

/**
 * Default implementation of the review storage manager, which stores the reviews in xobjects, directly in the
 * change request page.
 *
 * @version $Id$
 * @since 0.4
 */
@Component
@Singleton
public class DefaultReviewStorageManager implements ReviewStorageManager
{
    static final String ID_FORMAT = "xobject_%s";

    static final String REVIEW_ID_SEPARATOR = "_";

    @Inject
    private DocumentReferenceResolver<ChangeRequest> changeRequestDocumentReferenceResolver;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    @Named("current")
    private UserReferenceResolver<String> userReferenceResolver;

    @Inject
    private UserReferenceConverter userReferenceConverter;

    @Inject
    private Logger logger;

    @Override
    public void save(ChangeRequestReview review) throws ChangeRequestException
    {
        if (!review.isSaved()) {
            ChangeRequest changeRequest = review.getChangeRequest();
            DocumentReference changeRequestDocReference =
                this.changeRequestDocumentReferenceResolver.resolve(changeRequest);

            XWikiContext context = contextProvider.get();
            try {
                XWikiDocument changeRequestDoc = context.getWiki().getDocument(changeRequestDocReference, context);
                BaseObject xObject;
                String saveComment = "Add new review";
                if (StringUtils.isEmpty(review.getId())) {
                    int xObjectNumber = changeRequestDoc.createXObject(REVIEW_XCLASS, context);
                    xObject = changeRequestDoc.getXObject(REVIEW_XCLASS, xObjectNumber);
                    review.setId(String.format(ID_FORMAT, xObjectNumber));
                } else if (review.isNew()) {
                    int xObjectNumber = Integer.parseInt(review.getId().split(REVIEW_ID_SEPARATOR)[1]);
                    xObject = changeRequestDoc.getXObject(REVIEW_XCLASS, xObjectNumber, true, context);
                } else {
                    int xObjectNumber = Integer.parseInt(review.getId().split(REVIEW_ID_SEPARATOR)[1]);
                    xObject = changeRequestDoc.getXObject(REVIEW_XCLASS, xObjectNumber);
                    saveComment = "Update existing review";
                }
                this.fillXObjectValues(xObject, review);
                changeRequestDoc.getAuthors().setOriginalMetadataAuthor(review.getAuthor());

                // Bulletproofing: ensure to not save if there's no change
                if (changeRequestDoc.isMetaDataDirty()) {
                    context.getWiki().saveDocument(changeRequestDoc, saveComment, context);
                } else {
                    this.logger.error("Trying to save a review without performing any change: [{}]",
                        (Object) Thread.currentThread().getStackTrace());
                }
                review.setSaved(true);
                review.setNew(false);
            } catch (XWikiException e) {
                throw new ChangeRequestException("Error while saving review", e);
            }
        }
    }

    private void fillXObjectValues(BaseObject xObject, ChangeRequestReview review)
    {
        XWikiContext context = contextProvider.get();
        int approvedValue = (review.isApproved()) ? 1 : 0;
        DocumentReference userDocReference = this.userReferenceConverter.convert(review.getAuthor());
        xObject.set(APPROVED_PROPERTY, approvedValue, context);
        xObject.set(AUTHOR_PROPERTY, userDocReference, context);
        xObject.set(DATE_PROPERTY, review.getReviewDate(), context);
        int validValue = (review.isValid()) ? 1 : 0;
        xObject.set(VALID_PROPERTY, validValue, context);
        if (review.getOriginalApprover() != null) {
            xObject.set(ORIGINAL_APPROVER_PROPERTY,
                this.userReferenceConverter.convert(review.getOriginalApprover()), context);
        }
    }

    @Override
    public List<ChangeRequestReview> load(ChangeRequest changeRequest) throws ChangeRequestException
    {
        DocumentReference changeRequestDocReference =
            this.changeRequestDocumentReferenceResolver.resolve(changeRequest);
        XWikiContext context = contextProvider.get();
        List<ChangeRequestReview> result;
        try {
            XWikiDocument changeRequestDoc = context.getWiki().getDocument(changeRequestDocReference, context);
            List<BaseObject> xObjects = changeRequestDoc.getXObjects(REVIEW_XCLASS);
            for (BaseObject xObject : xObjects) {
                // Some objects might be null after deletion.
                if (xObject != null) {
                    boolean isApproved = StringUtils.equals(xObject.getStringValue(APPROVED_PROPERTY), "1");
                    UserReference author = this.userReferenceResolver.resolve(xObject.getStringValue(AUTHOR_PROPERTY));
                    Date reviewDate = xObject.getDateValue(DATE_PROPERTY);
                    boolean isValid = StringUtils.equals(xObject.getStringValue(VALID_PROPERTY), "1");
                    String id = String.format(ID_FORMAT, xObject.getNumber());
                    String originalApproverSerialized = xObject.getStringValue(ORIGINAL_APPROVER_PROPERTY);

                    ChangeRequestReview review = new ChangeRequestReview(changeRequest, isApproved, author);
                    review
                        .setValid(isValid)
                        .setId(id)
                        .setReviewDate(reviewDate)
                        .setSaved(true);

                    if (!StringUtils.isEmpty(originalApproverSerialized)) {
                        UserReference originalApprover = this.userReferenceResolver.resolve(originalApproverSerialized);
                        review.setOriginalApprover(originalApprover);
                    }

                    changeRequest.addReview(review);
                }
            }
            result = changeRequest.getReviews();
        } catch (XWikiException e) {
            throw new ChangeRequestException(
                String.format("Error while loading reviews for change request [%s]", changeRequest.getId()), e);
        }
        return result;
    }
}
