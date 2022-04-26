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
package org.xwiki.contrib.changerequest.internal.approvers;

import java.util.Arrays;

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
 * Initializer of the delegate approvers XClass document.
 *
 * @version $Id$
 * @since 0.13
 */
@Component
@Singleton
@Named("org.xwiki.contrib.changerequest.internal.approvers.DelegateApproversXClassInitializer")
public class DelegateApproversXClassInitializer implements MandatoryDocumentInitializer
{
    /**
     * Reference of the delegate approver xclass document.
     */
    public static final LocalDocumentReference DELEGATE_APPROVERS_XCLASS =
        new LocalDocumentReference(Arrays.asList("ChangeRequest", "Code"), "DelegateApproversClass");

    /**
     * Name of the field containing users list.
     */
    public static final String DELEGATED_USERS_PROPERTY = "delegatedUsers";

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Override
    public EntityReference getDocumentReference()
    {
        return new DocumentReference(DELEGATE_APPROVERS_XCLASS,
            new WikiReference(this.contextProvider.get().getMainXWiki()));
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
        result |= xClass.addUsersField(DELEGATED_USERS_PROPERTY, DELEGATED_USERS_PROPERTY);

        return result;
    }
}
