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

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ChangeRequestDiffManager;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.rendering.syntax.Syntax;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Default implementation of {@link ChangeRequestDiffManager}.
 * This is the most secure implementation: {@link #getRenderedContent(XWikiDocument, FileChange)} is called with the
 * restricted flag so no script is executed during the rendering.
 *
 * @version $Id$
 * @since 1.3
 */
@Component
@Singleton
public class DefaultChangeRequestDiffManager extends AbstractChangeRequestDiffManager
{
    @Inject
    private Provider<XWikiContext> contextProvider;

    protected String getRenderedContent(XWikiDocument document, FileChange fileChange) throws XWikiException
    {
        XWikiContext context = contextProvider.get();
        XWikiDocument currentDoc = context.getDoc();
        String result = "";
        if (document != null) {
            context.setDoc(document);
            try {
                // Note that we render the content in restricted mode to avoid any security issue:
                // we cannot guarantee here that the provided changes are safe
                result = document.displayDocument(Syntax.HTML_5_0, true, contextProvider.get());
            } finally {
                context.setDoc(currentDoc);
            }
        }
        return result;
    }
}
