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
import org.xwiki.model.reference.WikiReference;

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
@Named("org.xwiki.contrib.changerequest.internal.storage.ReviewXClassInitializer")
public class ReviewXClassInitializer implements MandatoryDocumentInitializer
{
    static final LocalDocumentReference REVIEW_XCLASS =
        new LocalDocumentReference("ChangeRequest", "ChangeRequestReviewClass");

    static final String AUTHOR_PROPERTY = "author";
    static final String APPROVED_PROPERTY = "approved";
    static final String DATE_PROPERTY = "reviewDate";
    static final String COMMENT_PROPERTY = "comment";
    static final String VALID_PROPERTY = "valid";

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Override
    public EntityReference getDocumentReference()
    {
        // we use main wiki since the module is supposed to be installed on farm.
        return new DocumentReference(REVIEW_XCLASS, new WikiReference(this.contextProvider.get().getMainXWiki()));
    }

    @Override
    public boolean updateDocument(XWikiDocument document)
    {
        boolean result = false;
        if (document.isNew()) {
            BaseClass xClass = document.getXClass();
            xClass.addBooleanField(APPROVED_PROPERTY, APPROVED_PROPERTY);
            xClass.addBooleanField(VALID_PROPERTY, VALID_PROPERTY);
            xClass.addUsersField(AUTHOR_PROPERTY, AUTHOR_PROPERTY);
            xClass.addTextAreaField(COMMENT_PROPERTY, COMMENT_PROPERTY, 40, 5);
            xClass.addDateField(DATE_PROPERTY, DATE_PROPERTY);

            document.setHidden(true);
            DocumentReference userReference = this.contextProvider.get().getUserReference();
            document.setCreatorReference(userReference);
            document.setAuthorReference(userReference);
            result = true;
        }
        return result;
    }
}
