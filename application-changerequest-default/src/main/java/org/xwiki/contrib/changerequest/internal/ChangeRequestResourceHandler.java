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

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.contrib.changerequest.ChangeRequestReference;
import org.xwiki.contrib.changerequest.internal.handlers.CreateChangeRequestHandler;
import org.xwiki.contrib.changerequest.rights.ChangeRequestApproveRight;
import org.xwiki.contrib.changerequest.rights.ChangeRequestRight;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.resource.AbstractResourceReferenceHandler;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceHandlerChain;
import org.xwiki.resource.ResourceReferenceHandlerException;
import org.xwiki.resource.ResourceType;
import org.xwiki.resource.annotations.Authenticate;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.UnableToRegisterRightException;

/**
 * Default handler for all {@link ChangeRequestReference}.
 *
 * @version $Id$
 * @since 0.1
 */
@Component
@Singleton
@Named("changerequest")
@Authenticate
public class ChangeRequestResourceHandler extends AbstractResourceReferenceHandler<ResourceType>
    implements Initializable
{
    @Inject
    private CreateChangeRequestHandler createChangeRequestHandler;

    @Inject
    private AuthorizationManager authorizationManager;

    @Override
    public void initialize() throws InitializationException
    {
        try {
            this.authorizationManager.register(ChangeRequestRight.INSTANCE);
            this.authorizationManager.register(ChangeRequestApproveRight.INSTANCE);
        } catch (UnableToRegisterRightException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<ResourceType> getSupportedResourceReferences()
    {
        return Collections.singletonList(ChangeRequestReference.TYPE);
    }

    @Override
    public void handle(ResourceReference reference, ResourceReferenceHandlerChain chain)
        throws ResourceReferenceHandlerException
    {
        ChangeRequestReference changeRequestReference = (ChangeRequestReference) reference;
        if (changeRequestReference.getAction() == ChangeRequestReference.ChangeRequestAction.CREATE) {
            try {
                this.createChangeRequestHandler.handle(changeRequestReference);
            } catch (ChangeRequestException e) {
                e.printStackTrace();
            }
        }
        chain.handleNext(reference);
    }
}
