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
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.ChangeRequestReview;
import org.xwiki.contrib.changerequest.internal.UserReferenceConverter;
import org.xwiki.contrib.changerequest.storage.ReviewStorageManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

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
    static final LocalDocumentReference REVIEW_XCLASS =
        new LocalDocumentReference("ChangeRequest", "ChangeRequestReviewClass");

    static final String AUTHOR_PROPERTY = "author";
    static final String APPROVED_PROPERTY = "approved";
    static final String DATE_PROPERTY = "reviewDate";
    static final String COMMENT_PROPERTY = "comment";

    @Inject
    private DocumentReferenceResolver<ChangeRequest> changeRequestDocumentReferenceResolver;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private UserReferenceResolver<String> userReferenceResolver;

    @Inject
    private UserReferenceConverter userReferenceConverter;

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
                int xObjectNumber = changeRequestDoc.createXObject(REVIEW_XCLASS, context);
                BaseObject xObject = changeRequestDoc.getXObject(REVIEW_XCLASS, xObjectNumber);
                int approvedValue = (review.isApproved()) ? 1 : 0;
                xObject.set(APPROVED_PROPERTY, approvedValue, context);
                xObject.set(AUTHOR_PROPERTY, this.userReferenceConverter.convert(review.getAuthor()), context);
                xObject.set(DATE_PROPERTY, review.getReviewDate(), context);
                xObject.set(COMMENT_PROPERTY, review.getComment(), context);

                context.getWiki().saveDocument(changeRequestDoc, "Add new review", context);
                review.setSaved(true);
            } catch (XWikiException e) {
                throw new ChangeRequestException("Error while saving new review", e);
            }
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
                boolean isApproved = StringUtils.equals(xObject.getStringValue(APPROVED_PROPERTY), "1");
                UserReference author = this.userReferenceResolver.resolve(xObject.getStringValue(AUTHOR_PROPERTY));
                String comment = xObject.getLargeStringValue(COMMENT_PROPERTY);
                Date reviewDate = xObject.getDateValue(DATE_PROPERTY);

                ChangeRequestReview review = new ChangeRequestReview(changeRequest, isApproved, author);
                review.setReviewDate(reviewDate)
                    .setComment(comment)
                    .setSaved(true);
                changeRequest.addReview(review);
            }
            result = changeRequest.getReviews();
        } catch (XWikiException e) {
            throw new ChangeRequestException(
                String.format("Error while loading reviews for change request [%s]", changeRequest.getId()), e);
        }
        return result;
    }
}
