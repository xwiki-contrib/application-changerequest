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
package org.xwiki.contrib.changerequest.internal.handlers;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.resource.AbstractResourceReferenceHandler;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceHandlerChain;
import org.xwiki.resource.ResourceReferenceHandlerException;
import org.xwiki.resource.entity.EntityResourceAction;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.web.Utils;

/**
 * Allow to display template for requesting deletion of documents as part of change request.
 *
 * @version $Id$
 * @since 0.5
 */
@Component
@Named("deletecr")
@Singleton
public class DeleteAsChangeRequestResourceHandler extends AbstractResourceReferenceHandler<EntityResourceAction>
{
    @Inject
    private Provider<XWikiContext> contextProvider;

    @Override
    public List<EntityResourceAction> getSupportedResourceReferences()
    {
        return Collections.singletonList(new EntityResourceAction("deletecr"));
    }

    @Override
    public void handle(ResourceReference reference, ResourceReferenceHandlerChain chain)
        throws ResourceReferenceHandlerException
    {
        // We are directly relying on Utils#parseTemplate because we want the plugin manager to properly
        // handle the javascript placeholders and it avoids duplicating code.
        try {
            Utils.parseTemplate("changerequest/deletecr", true, this.contextProvider.get());
        } catch (XWikiException e) {
            throw new ResourceReferenceHandlerException("Error when parsing deletecr template.", e);
        }
    }
}
