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
package org.xwiki.contrib.changerequest.internal;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.web.EditAction;

/**
 * Handler for the edit change request action.
 * FIXME: this needs to be replaced with a proper handler, once it's fixed on XWiki side.
 *
 * @version $Id$
 * @since 0.1
 */
@Component
@Named("editcr")
@Singleton
public class EditChangeRequestAction extends EditAction
{
    private static final String EDIT = "edit";

    @Override
    public String render(XWikiContext context) throws XWikiException
    {
        String render = super.render(context);
        if (EDIT.equals(render)) {
            // We set the action to edit since there's various checks in the templates related to it.
            context.setAction(EDIT);
            return "editcr";
        } else {
            return render;
        }
    }

    @Override
    protected String getName()
    {
        // This is very hackish just to bypass security until we have a proper resource reference handler.
        return "view";
    }
}
