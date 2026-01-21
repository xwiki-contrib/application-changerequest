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

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.contrib.changerequest.ChangeRequestReference;
import org.xwiki.contrib.changerequest.rights.ChangeRequestApproveRight;
import org.xwiki.contrib.changerequest.rights.ChangeRequestRight;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.resource.AbstractResourceReferenceHandler;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceHandlerChain;
import org.xwiki.resource.ResourceReferenceHandlerException;
import org.xwiki.resource.ResourceType;
import org.xwiki.resource.annotations.Authenticate;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.UnableToRegisterRightException;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

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
    implements Initializable, Disposable
{
    @Inject
    @Named("context")
    private ComponentManager componentManager;

    @Inject
    private AuthorizationManager authorizationManager;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Override
    public void initialize() throws InitializationException
    {
        try {
            this.authorizationManager.register(ChangeRequestRight.INSTANCE);
            this.authorizationManager.register(ChangeRequestApproveRight.INSTANCE);
        } catch (UnableToRegisterRightException e) {
            throw new InitializationException("Error when trying to register the custom rights", e);
        }
    }

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        // FIXME: We should unregister the rights, but until XWIKI-21012 is fixed we can't really as it's causing a
        //  mess.
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

        WikiReference wikiReference = changeRequestReference.getWikiReference();
        try {
            if (!this.wikiDescriptorManager.exists(wikiReference.getName())) {
                throw new ResourceReferenceHandlerException(
                    String.format("The wiki [%s] does not exist.", wikiReference));
            }
        } catch (WikiManagerException e) {
            throw new ResourceReferenceHandlerException(
                String.format("Error when checking if wiki [%s] exists.", wikiReference), e);
        }
        String actionHint = changeRequestReference.getAction().name().toLowerCase(Locale.ROOT);

        XWikiContext context = this.contextProvider.get();
        WikiReference currentWiki = context.getWikiReference();
        try {
            if (this.componentManager.hasComponent(ChangeRequestActionHandler.class, actionHint)) {
                ChangeRequestActionHandler actionHandler =
                    this.componentManager.getInstance(ChangeRequestActionHandler.class, actionHint);

                // Be sure that the handler is executed in the context of the targeted wiki.
                context.setWikiReference(wikiReference);
                actionHandler.handle(changeRequestReference);
            } else {
                throw new ResourceReferenceHandlerException(
                    String.format("The action [%s] is not implemented.", actionHint));
            }
        } catch (ChangeRequestException e) {
            throw new ResourceReferenceHandlerException(
                String.format("Error while trying to handle the reference [%s]", reference), e);
        } catch (ComponentLookupException | XWikiException e) {
            throw new ResourceReferenceHandlerException(
                String.format("Error while initializing action handler for the reference [%s]", reference), e);
        } catch (IOException e) {
            throw new ResourceReferenceHandlerException("Error while writing response", e);
        } finally {
            context.setWikiReference(currentWiki);
        }

        chain.handleNext(reference);
    }
}
