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
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.diff.ChangeRequestDiffRenderContent;
import org.xwiki.rendering.syntax.Syntax;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Default implementation of {@link ChangeRequestDiffRenderContent}.
 * This is the most secure implementation: the restricted flag is used to performing the rendering so no script is
 * executed during the rendering.
 *
 * @version $Id$
 * @since 1.5
 */
@Component
@Singleton
public class DefaultChangeRequestDiffRenderContent implements ChangeRequestDiffRenderContent
{
    @Inject
    private Provider<XWikiContext> contextProvider;

    @Override
    public String getRenderedContent(DocumentModelBridge document, FileChange fileChange) throws ChangeRequestException
    {
        String result = "";
        if (document != null) {
            if (!(document instanceof XWikiDocument)) {
                // Should never happen.
                throw new ChangeRequestException("This API should only be used with XWikiDocument.");
            }
            XWikiDocument xWikiDocument = (XWikiDocument) document;
            XWikiContext context = contextProvider.get();
            XWikiDocument currentDoc = context.getDoc();
            context.setDoc(xWikiDocument);
            try {
                // Note that we render the content in restricted mode to avoid any security issue:
                // we cannot guarantee here that the provided changes are safe
                result = xWikiDocument.displayDocument(Syntax.HTML_5_0, true, context);
            } catch (XWikiException e) {
                throw new ChangeRequestException("Error while rendering the document", e);
            } finally {
                context.setDoc(currentDoc);
            }
        }
        return result;
    }
}
