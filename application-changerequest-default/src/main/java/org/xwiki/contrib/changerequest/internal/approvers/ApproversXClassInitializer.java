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

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.doc.AbstractMandatoryClassInitializer;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Component responsible to create the approvers xclass.
 *
 * @version $Id$
 * @since 0.5
 */
@Component
@Singleton
@Named("ChangeRequest.Code.ApproversClass")
public class ApproversXClassInitializer extends AbstractMandatoryClassInitializer
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

    /**
     * Name of the field defining if the list of approvers have been manually edited or not.
     */
    public static final String MANUAL_EDITION_PROPERTY = "manualEdition";

    /**
     * Separator used in approvers lists.
     */
    public static final Character SEPARATOR_CHARACTER = ',';

    /**
     * Default constructor.
     */
    public ApproversXClassInitializer()
    {
        super(APPROVERS_XCLASS);
    }

    @Override
    protected void createClass(BaseClass xClass)
    {
        xClass.addUsersField(USERS_APPROVERS_PROPERTY, USERS_APPROVERS_PROPERTY);
        xClass.addGroupsField(GROUPS_APPROVERS_PROPERTY, GROUPS_APPROVERS_PROPERTY);
        xClass.addBooleanField(MANUAL_EDITION_PROPERTY, MANUAL_EDITION_PROPERTY, "checkbox", false);
    }
}
