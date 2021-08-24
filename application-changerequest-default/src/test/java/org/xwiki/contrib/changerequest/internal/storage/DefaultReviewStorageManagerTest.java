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

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestReview;
import org.xwiki.contrib.changerequest.internal.UserReferenceConverter;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DefaultReviewStorageManager}.
 *
 * @version $Id$
 * @since 0.4
 */
@ComponentTest
class DefaultReviewStorageManagerTest
{
    @InjectMockComponents
    private DefaultReviewStorageManager storageManager;

    @MockComponent
    private DocumentReferenceResolver<ChangeRequest> changeRequestDocumentReferenceResolver;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private UserReferenceResolver<String> userReferenceResolver;

    @MockComponent
    private UserReferenceConverter userReferenceConverter;

    private XWikiContext context;

    @BeforeEach
    void beforeEach()
    {
        this.context = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(this.context);
    }

    @Test
    void save() throws Exception
    {
        ChangeRequestReview review = mock(ChangeRequestReview.class);

        when(review.isSaved()).thenReturn(false);
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        when(review.getChangeRequest()).thenReturn(changeRequest);

        DocumentReference changeRequestDocRef = mock(DocumentReference.class);
        when(this.changeRequestDocumentReferenceResolver.resolve(changeRequest)).thenReturn(changeRequestDocRef);

        XWikiDocument xWikiDocument = mock(XWikiDocument.class);
        XWiki xWiki = mock(XWiki.class);
        when(this.context.getWiki()).thenReturn(xWiki);
        when(xWiki.getDocument(changeRequestDocRef, this.context)).thenReturn(xWikiDocument);

        when(review.getId()).thenReturn("");
        when(xWikiDocument.createXObject(DefaultReviewStorageManager.REVIEW_XCLASS, this.context)).thenReturn(42);

        BaseObject baseObject = mock(BaseObject.class);
        when(xWikiDocument.getXObject(DefaultReviewStorageManager.REVIEW_XCLASS, 42)).thenReturn(baseObject);

        UserReference userReference = mock(UserReference.class);
        when(review.isApproved()).thenReturn(true);
        when(review.isValid()).thenReturn(true);
        when(review.getReviewDate()).thenReturn(new Date(34));
        when(review.getAuthor()).thenReturn(userReference);
        when(review.getComment()).thenReturn("Some thing");

        DocumentReference authorReference = mock(DocumentReference.class);
        when(this.userReferenceConverter.convert(userReference)).thenReturn(authorReference);

        this.storageManager.save(review);
        verify(review).setId("xobject_42");
        verify(review).setSaved(true);
        verify(baseObject).set(DefaultReviewStorageManager.APPROVED_PROPERTY, 1, this.context);
        verify(baseObject).set(DefaultReviewStorageManager.VALID_PROPERTY, 1, this.context);
        verify(baseObject).set(DefaultReviewStorageManager.AUTHOR_PROPERTY, authorReference, this.context);
        verify(baseObject).set(DefaultReviewStorageManager.DATE_PROPERTY, new Date(34), this.context);
        verify(baseObject).set(DefaultReviewStorageManager.COMMENT_PROPERTY, "Some thing", this.context);
        verify(xWiki).saveDocument(xWikiDocument, "Add new review", this.context);
        verify(xWikiDocument).createXObject(DefaultReviewStorageManager.REVIEW_XCLASS, this.context);

        when(review.isValid()).thenReturn(false);
        when(review.getId()).thenReturn("xobject_42");
        when(review.isSaved()).thenReturn(false);

        this.storageManager.save(review);
        verify(xWikiDocument).createXObject(DefaultReviewStorageManager.REVIEW_XCLASS, this.context);
        verify(xWiki).saveDocument(xWikiDocument, "Update existing review", this.context);
        verify(baseObject).set(DefaultReviewStorageManager.VALID_PROPERTY, 0, this.context);
    }

    @Test
    void load() throws Exception
    {
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        DocumentReference changeRequestDocRef = mock(DocumentReference.class);
        when(this.changeRequestDocumentReferenceResolver.resolve(changeRequest)).thenReturn(changeRequestDocRef);

        XWikiDocument xWikiDocument = mock(XWikiDocument.class);
        XWiki xWiki = mock(XWiki.class);
        when(this.context.getWiki()).thenReturn(xWiki);
        when(xWiki.getDocument(changeRequestDocRef, this.context)).thenReturn(xWikiDocument);

        BaseObject obj1 = mock(BaseObject.class);
        BaseObject obj2 = mock(BaseObject.class);

        when(xWikiDocument.getXObjects(DefaultReviewStorageManager.REVIEW_XCLASS))
            .thenReturn(Arrays.asList(obj1, obj2));

        when(obj1.getStringValue(DefaultReviewStorageManager.APPROVED_PROPERTY)).thenReturn("0");
        when(obj1.getStringValue(DefaultReviewStorageManager.AUTHOR_PROPERTY)).thenReturn("author1");
        when(obj1.getLargeStringValue(DefaultReviewStorageManager.COMMENT_PROPERTY)).thenReturn("Some comment");
        when(obj1.getDateValue(DefaultReviewStorageManager.DATE_PROPERTY)).thenReturn(new Date(45));
        when(obj1.getStringValue(DefaultReviewStorageManager.VALID_PROPERTY)).thenReturn("1");
        when(obj1.getNumber()).thenReturn(13);

        when(obj2.getStringValue(DefaultReviewStorageManager.APPROVED_PROPERTY)).thenReturn("1");
        when(obj2.getStringValue(DefaultReviewStorageManager.AUTHOR_PROPERTY)).thenReturn("author2");
        when(obj2.getLargeStringValue(DefaultReviewStorageManager.COMMENT_PROPERTY)).thenReturn("Some other comment");
        when(obj2.getDateValue(DefaultReviewStorageManager.DATE_PROPERTY)).thenReturn(new Date(16));
        when(obj2.getStringValue(DefaultReviewStorageManager.VALID_PROPERTY)).thenReturn("0");
        when(obj2.getNumber()).thenReturn(48);

        UserReference author1 = mock(UserReference.class);
        when(this.userReferenceResolver.resolve("author1")).thenReturn(author1);
        UserReference author2 = mock(UserReference.class);
        when(this.userReferenceResolver.resolve("author2")).thenReturn(author2);

        ChangeRequestReview review1 = new ChangeRequestReview(changeRequest, false, author1)
            .setReviewDate(new Date(45))
            .setComment("Some comment")
            .setSaved(true)
            .setValid(true)
            .setId("xobject_13");

        ChangeRequestReview review2 = new ChangeRequestReview(changeRequest, true, author2)
            .setReviewDate(new Date(16))
            .setComment("Some other comment")
            .setSaved(true)
            .setValid(false)
            .setId("xobject_48");

        List<ChangeRequestReview> expectedResult = Arrays.asList(
            mock(ChangeRequestReview.class),
            mock(ChangeRequestReview.class),
            mock(ChangeRequestReview.class));

        when(changeRequest.getReviews()).thenReturn(expectedResult);
        assertEquals(expectedResult, this.storageManager.load(changeRequest));
        verify(changeRequest).addReview(review1);
        verify(changeRequest).addReview(review2);
    }
}
