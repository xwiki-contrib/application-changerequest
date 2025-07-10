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
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ChangeRequestStatus;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.sheet.SheetBinder;

import com.xpn.xwiki.doc.AbstractMandatoryClassInitializer;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Component responsible to initialize the change request xclass.
 *
 * @version $Id$
 * @since 0.5
 */
@Component
@Singleton
@Named("ChangeRequest.Code.ChangeRequestClass")
public class ChangeRequestXClassInitializer extends AbstractMandatoryClassInitializer
{
    /**
     * Default location for change request code.
     */
    public static final List<String> CHANGE_REQUEST_SPACE = Arrays.asList("ChangeRequest", "Code");

    /**
     * Reference of the change request XClass.
     */
    public static final LocalDocumentReference CHANGE_REQUEST_XCLASS =
        new LocalDocumentReference(CHANGE_REQUEST_SPACE, "ChangeRequestClass");

    static final String STATUS_FIELD = "status";
    static final String CHANGED_DOCUMENTS_FIELD = "changedDocuments";
    static final String AUTHORS_FIELD = "authors";
    static final String STALE_DATE_FIELD = "staleDate";

    private static final LocalDocumentReference SHEET_REFERENCE =
        new LocalDocumentReference(CHANGE_REQUEST_SPACE, "ChangeRequestSheet");

    /**
     * Used to bind a class to a document sheet.
     */
    @Inject
    @Named("class")
    protected SheetBinder classSheetBinder;

    /**
     * Default constructor.
     */
    public ChangeRequestXClassInitializer()
    {
        super(CHANGE_REQUEST_XCLASS);
    }

    @Override
    protected void createClass(BaseClass xclass)
    {
        xclass.addStaticListField(STATUS_FIELD, STATUS_FIELD,
            Arrays.stream(ChangeRequestStatus.values())
                .map(item -> item.toString().toLowerCase(Locale.ROOT))
                .collect(Collectors.joining("|")),
            ChangeRequestStatus.DRAFT.name().toLowerCase(Locale.ROOT));
        xclass.addPageField(CHANGED_DOCUMENTS_FIELD, CHANGED_DOCUMENTS_FIELD, 1, true);
        xclass.addUsersField(AUTHORS_FIELD, AUTHORS_FIELD, true);
        xclass.addDateField(STALE_DATE_FIELD, STALE_DATE_FIELD);
    }

    @Override
    protected boolean updateDocumentSheet(XWikiDocument document)
    {
        return this.classSheetBinder.bind(document, SHEET_REFERENCE);
    }
}
