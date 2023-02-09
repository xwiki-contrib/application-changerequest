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
package org.xwiki.contrib.changerequest.internal.diff;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.diff.ChangeRequestDiffRenderContent;
import org.xwiki.contrib.changerequest.internal.UserReferenceConverter;
import org.xwiki.model.document.DocumentAuthors;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.user.UserReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Implementation of {@link ChangeRequestDiffRenderContent} where the content is rendered with the author of the
 * filechange user.
 *
 * @version $Id$
 * @since 1.5
 * @since 1.4.4
 */
@Component
@Named("authorright")
@Singleton
public class AuthorRightChangeRequestDiffRenderContent implements ChangeRequestDiffRenderContent
{
    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private UserReferenceConverter userReferenceConverter;

    @Override
    public String getRenderedContent(DocumentModelBridge document, FileChange fileChange) throws ChangeRequestException
    {
        String result = "";
        if (document != null) {
            if (!(document instanceof XWikiDocument)) {
                // Should never happen.
                throw new ChangeRequestException("This API should only be used with XWikiDocument.");
            }
            XWikiContext context = contextProvider.get();
            UserReference fileChangeAuthor = fileChange.getAuthor();
            XWikiDocument cloneDoc = ((XWikiDocument) document).clone();
            DocumentAuthors authors = cloneDoc.getAuthors();
            authors.setEffectiveMetadataAuthor(fileChangeAuthor);
            authors.setContentAuthor(fileChangeAuthor);
            XWikiDocument currentDoc = context.getDoc();
            DocumentReference currentUser = context.getUserReference();
            context.setDoc(cloneDoc);
            context.setUserReference(this.userReferenceConverter.convert(fileChangeAuthor));
            try {
                result = cloneDoc.displayDocument(Syntax.HTML_5_0, false, contextProvider.get());
            } catch (XWikiException e) {
                throw new ChangeRequestException("Error while rendering the document", e);
            } finally {
                context.setDoc(currentDoc);
                context.setUserReference(currentUser);
            }
        }
        return result;
    }
}
