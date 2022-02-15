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
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ChangeRequestStatus;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.MandatoryDocumentInitializer;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Component responsible to initialize the change request xclass.
 *
 * @version $Id$
 * @since 0.5
 */
@Component
@Singleton
@Named("org.xwiki.contrib.changerequest.internal.storage.ChangeRequestXClassInitializer")
public class ChangeRequestXClassInitializer implements MandatoryDocumentInitializer
{
    static final List<String> CHANGE_REQUEST_SPACE = Arrays.asList("ChangeRequest", "Code");

    static final LocalDocumentReference CHANGE_REQUEST_XCLASS =
        new LocalDocumentReference(CHANGE_REQUEST_SPACE, "ChangeRequestClass");
    static final String STATUS_FIELD = "status";
    static final String CHANGED_DOCUMENTS_FIELD = "changedDocuments";
    static final String AUTHORS_FIELD = "authors";
    static final String STALE_DATE_FIELD = "staleDate";

    private static final LocalDocumentReference CLASS_SHEET_BINDING_XCLASS =
        new LocalDocumentReference("XWiki", "ClassSheetBinding");

    private static final LocalDocumentReference SHEET_REFERENCE =
        new LocalDocumentReference(CHANGE_REQUEST_SPACE, "ChangeRequestSheet");

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Override
    public EntityReference getDocumentReference()
    {
        // we use main wiki since the module is supposed to be installed on farm.
        return new DocumentReference(CHANGE_REQUEST_XCLASS,
            new WikiReference(this.contextProvider.get().getMainXWiki()));
    }

    @Override
    public boolean updateDocument(XWikiDocument document)
    {
        boolean result = false;
        if (document.isNew()) {
            XWikiContext context = contextProvider.get();
            BaseObject xObject = document.getXObject(CLASS_SHEET_BINDING_XCLASS, true, context);
            xObject.setStringValue("sheet", this.entityReferenceSerializer.serialize(SHEET_REFERENCE));
            document.setHidden(true);

            DocumentReference userReference = context.getUserReference();
            document.setCreatorReference(userReference);
            document.setAuthorReference(userReference);
            result = true;
        }

        BaseClass xClass = document.getXClass();
        result |= xClass.addStaticListField(STATUS_FIELD, STATUS_FIELD,
            Arrays.stream(ChangeRequestStatus.values())
                .map(item -> item.toString().toLowerCase(Locale.ROOT))
                .collect(Collectors.joining("|")),
            ChangeRequestStatus.DRAFT.name().toLowerCase(Locale.ROOT));

        result |= xClass.addPageField(CHANGED_DOCUMENTS_FIELD, CHANGED_DOCUMENTS_FIELD, 1, true);
        result |= xClass.addUsersField(AUTHORS_FIELD, AUTHORS_FIELD, true);
        result |= xClass.addDateField(STALE_DATE_FIELD, STALE_DATE_FIELD);

        return result;
    }
}
