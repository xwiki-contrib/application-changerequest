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

import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ReviewInvalidationReason;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.doc.AbstractMandatoryClassInitializer;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Component responsible to initialize the review xclass.
 *
 * @version $Id$
 * @since 0.5
 */
@Component
@Singleton
@Named("ChangeRequest.Code.ChangeRequestReviewClass")
public class ReviewXClassInitializer extends AbstractMandatoryClassInitializer
{
    /**
     * Reference of the review xclass.
     */
    public static final LocalDocumentReference REVIEW_XCLASS =
        new LocalDocumentReference(ChangeRequestXClassInitializer.CHANGE_REQUEST_SPACE, "ChangeRequestReviewClass");

    static final String AUTHOR_PROPERTY = "author";
    static final String APPROVED_PROPERTY = "approved";
    static final String DATE_PROPERTY = "reviewDate";
    static final String VALID_PROPERTY = "valid";
    static final String ORIGINAL_APPROVER_PROPERTY = "originalApprover";
    static final String INVALIDATION_REASON_PROPERTY = "reviewInvalidationReason";

    /**
     * Default constructor.
     */
    public ReviewXClassInitializer()
    {
        super(REVIEW_XCLASS);
    }

    @Override
    protected void createClass(BaseClass xClass)
    {
        xClass.addBooleanField(APPROVED_PROPERTY, APPROVED_PROPERTY);
        xClass.addBooleanField(VALID_PROPERTY, VALID_PROPERTY);
        xClass.addUsersField(AUTHOR_PROPERTY, AUTHOR_PROPERTY);
        xClass.addDateField(DATE_PROPERTY, DATE_PROPERTY);
        xClass.addUsersField(ORIGINAL_APPROVER_PROPERTY, ORIGINAL_APPROVER_PROPERTY);
        xClass.addStaticListField(INVALIDATION_REASON_PROPERTY, INVALIDATION_REASON_PROPERTY,
            Stream.of(ReviewInvalidationReason.values())
                .map(ReviewInvalidationReason::name)
                .collect(Collectors.joining("|")));
    }
}
