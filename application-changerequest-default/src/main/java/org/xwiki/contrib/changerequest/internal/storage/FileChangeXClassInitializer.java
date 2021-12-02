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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.MandatoryDocumentInitializer;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Component responsible to initialize the file change xclass.
 *
 * @version $Id$
 * @since 0.5
 */
@Component
@Singleton
@Named("org.xwiki.contrib.changerequest.internal.storage.FileChangeXClassInitializer")
public class FileChangeXClassInitializer implements MandatoryDocumentInitializer
{
    static final LocalDocumentReference FILECHANGE_XCLASS =
        new LocalDocumentReference(ChangeRequestXClassInitializer.CHANGE_REQUEST_SPACE, "FileChangeClass");

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

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Override
    public EntityReference getDocumentReference()
    {
        return new DocumentReference(FILECHANGE_XCLASS, new WikiReference(this.contextProvider.get().getMainXWiki()));
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
        result |= xClass.addUsersField(AUTHOR_PROPERTY, AUTHOR_PROPERTY);
        result |= xClass.addDateField(CREATION_DATE_PROPERTY, CREATION_DATE_PROPERTY);
        result |= xClass.addTextField(FILENAME_PROPERTY, FILENAME_PROPERTY, 30);
        result |= xClass.addTextField(PREVIOUS_VERSION_PROPERTY, PREVIOUS_VERSION_PROPERTY, 30);
        result |= xClass
            .addDateField(PREVIOUS_PUBLISHED_VERSION_DATE_PROPERTY, PREVIOUS_PUBLISHED_VERSION_DATE_PROPERTY);
        result |= xClass.addTextField(PREVIOUS_PUBLISHED_VERSION_PROPERTY, PREVIOUS_PUBLISHED_VERSION_PROPERTY, 30);
        result |= xClass.addTextField(VERSION_PROPERTY, VERSION_PROPERTY, 30);
        result |= xClass.addPageField(REFERENCE_PROPERTY, REFERENCE_PROPERTY, 1);
        result |= xClass.addTextField(REFERENCE_LOCALE_PROPERTY, REFERENCE_LOCALE_PROPERTY, 10);
        result |= xClass.addStaticListField(TYPE_PROPERTY, TYPE_PROPERTY,
            Arrays.stream(FileChange.FileChangeType.values())
                .map(value -> value.name().toLowerCase(Locale.ROOT))
                .collect(Collectors.joining()));

        return result;
    }


}
