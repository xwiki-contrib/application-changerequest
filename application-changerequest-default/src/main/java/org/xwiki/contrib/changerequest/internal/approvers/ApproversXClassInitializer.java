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
 * Component responsible to create the approvers xclass.
 *
 * @version $Id$
 * @since 0.5
 */
@Component
@Singleton
@Named("org.xwiki.contrib.changerequest.internal.approvers.ApproversXClassInitializer")
public class ApproversXClassInitializer implements MandatoryDocumentInitializer
{
    /**
     * Reference of approvers xclass.
     */
    public static final LocalDocumentReference APPROVERS_XCLASS =
        new LocalDocumentReference(Arrays.asList("ChangeRequest", "Code"), "ApproversClass");

    /**
     * Name of the field containing users list.
     */
    public static final String USERS_APPROVERS_PROPERTY = "usersApprovers";

    /**
     * Name of the field containing groups list.
     */
    public static final String GROUPS_APPROVERS_PROPERTY = "groupsApprovers";

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Override
    public EntityReference getDocumentReference()
    {
        return new DocumentReference(APPROVERS_XCLASS, new WikiReference(this.contextProvider.get().getMainXWiki()));
    }

    @Override
    public boolean updateDocument(XWikiDocument document)
    {
        boolean result = false;

        if (document.isNew()) {
            BaseClass xClass = document.getXClass();
            xClass.addUsersField(USERS_APPROVERS_PROPERTY, USERS_APPROVERS_PROPERTY);
            xClass.addGroupsField(GROUPS_APPROVERS_PROPERTY, GROUPS_APPROVERS_PROPERTY);

            document.setHidden(true);
            DocumentReference userReference = this.contextProvider.get().getUserReference();
            document.setCreatorReference(userReference);
            document.setAuthorReference(userReference);
            result = true;
        }
        return result;
    }
}
