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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.MandatoryDocumentInitializer;
import com.xpn.xwiki.doc.XWikiDocument;
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
public class ReviewXClassInitializer implements MandatoryDocumentInitializer
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

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Override
    public EntityReference getDocumentReference()
    {
        return new DocumentReference(REVIEW_XCLASS, this.contextProvider.get().getWikiReference());
    }

    @Override
    public boolean updateDocument(XWikiDocument document)
    {
        boolean result = false;
        if (document.isNew()) {
            document.setHidden(true);
            DocumentReference userReference = this.contextProvider.get().getUserReference();
            document.setCreatorReference(userReference);
            document.setAuthorReference(userReference);
            result = true;
        }
        BaseClass xClass = document.getXClass();
        result |= xClass.addBooleanField(APPROVED_PROPERTY, APPROVED_PROPERTY);
        result |= xClass.addBooleanField(VALID_PROPERTY, VALID_PROPERTY);
        result |= xClass.addUsersField(AUTHOR_PROPERTY, AUTHOR_PROPERTY);
        result |= xClass.addDateField(DATE_PROPERTY, DATE_PROPERTY);
        result |= xClass.addUsersField(ORIGINAL_APPROVER_PROPERTY, ORIGINAL_APPROVER_PROPERTY);


        return result;
    }
}
