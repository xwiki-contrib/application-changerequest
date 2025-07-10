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
import java.util.Locale;
import java.util.stream.Collectors;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.doc.AbstractMandatoryClassInitializer;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Component responsible to initialize the file change xclass.
 *
 * @version $Id$
 * @since 0.5
 */
@Component
@Singleton
@Named("ChangeRequest.Code.FileChangeClass")
public class FileChangeXClassInitializer extends AbstractMandatoryClassInitializer
{
    /**
     * Reference of the xclass.
     */
    public static final LocalDocumentReference FILECHANGE_XCLASS =
        new LocalDocumentReference(ChangeRequestXClassInitializer.CHANGE_REQUEST_SPACE, "FileChangeClass");

    /**
     * Name of the xobject property containing the change request identifier.
     */
    public static final String CHANGE_REQUEST_ID = "changeRequestId";

    static final String PREVIOUS_VERSION_PROPERTY = "previousVersion";
    static final String PREVIOUS_PUBLISHED_VERSION_PROPERTY = "previousPublishedVersion";
    static final String PREVIOUS_PUBLISHED_VERSION_DATE_PROPERTY = "previousPublishedVersionDate";
    static final String VERSION_PROPERTY = "version";
    static final String FILENAME_PROPERTY = "filename";
    static final String REFERENCE_PROPERTY = "reference";
    static final String REFERENCE_LOCALE_PROPERTY = "referenceLocale";
    static final String TYPE_PROPERTY = "type";
    static final String AUTHOR_PROPERTY = "author";
    static final String CREATION_DATE_PROPERTY = "creationDate";

    /**
     * Default constructor.
     */
    public FileChangeXClassInitializer()
    {
        super(FILECHANGE_XCLASS);
    }

    @Override
    protected void createClass(BaseClass xClass)
    {
        xClass.addUsersField(AUTHOR_PROPERTY, AUTHOR_PROPERTY);
        xClass.addDateField(CREATION_DATE_PROPERTY, CREATION_DATE_PROPERTY);
        xClass.addTextField(FILENAME_PROPERTY, FILENAME_PROPERTY, 30);
        xClass.addTextField(PREVIOUS_VERSION_PROPERTY, PREVIOUS_VERSION_PROPERTY, 30);
        xClass
            .addDateField(PREVIOUS_PUBLISHED_VERSION_DATE_PROPERTY, PREVIOUS_PUBLISHED_VERSION_DATE_PROPERTY);
        xClass.addTextField(PREVIOUS_PUBLISHED_VERSION_PROPERTY, PREVIOUS_PUBLISHED_VERSION_PROPERTY, 30);
        xClass.addTextField(VERSION_PROPERTY, VERSION_PROPERTY, 30);
        xClass.addPageField(REFERENCE_PROPERTY, REFERENCE_PROPERTY, 1);
        xClass.addTextField(REFERENCE_LOCALE_PROPERTY, REFERENCE_LOCALE_PROPERTY, 10);
        xClass.addStaticListField(TYPE_PROPERTY, TYPE_PROPERTY,
            Arrays.stream(FileChange.FileChangeType.values())
                .map(value -> value.name().toLowerCase(Locale.ROOT))
                .collect(Collectors.joining()));
        xClass.addTextField(CHANGE_REQUEST_ID, CHANGE_REQUEST_ID, 100);
    }
}
