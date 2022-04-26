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
package org.xwiki.contrib.changerequest.internal.listeners;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ChangeRequestConfiguration;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.DelegateApproverManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.RegexEntityReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.remote.RemoteObservationManagerContext;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.XObjectUpdatedEvent;
import com.xpn.xwiki.internal.mandatory.XWikiUsersDocumentInitializer;
import com.xpn.xwiki.objects.BaseObjectReference;

/**
 * Listener in charge of updating the delegate approvers when the mechanism is enabled and some properties are set up
 * to compute the delegate approvers.
 *
 * @version $Id$
 * @since 0.13
 */
@Component
@Singleton
@Named(UsersUpdatedListener.NAME)
public class UsersUpdatedListener extends AbstractEventListener
{
    static final String NAME = "org.xwiki.contrib.changerequest.internal.listeners.UsersUpdatedListener";

    private static final RegexEntityReference REFERENCE =
        BaseObjectReference.any(XWikiUsersDocumentInitializer.CLASS_REFERENCE_STRING);

    private static final List<Event> EVENT_LIST = Collections.singletonList(
        new XObjectUpdatedEvent(REFERENCE)
    );

    @Inject
    private Provider<DelegateApproverManager<XWikiDocument>> delegateApproverManagerProvider;

    @Inject
    private ChangeRequestConfiguration configuration;

    @Inject
    @Named("document")
    private UserReferenceResolver<DocumentReference> userReferenceResolver;

    @Inject
    private RemoteObservationManagerContext remoteObservationManagerContext;

    @Inject
    private Logger logger;

    /**
     * Default constructor.
     */
    public UsersUpdatedListener()
    {
        super(NAME, EVENT_LIST);
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (this.configuration.isDelegateEnabled()
            && !this.configuration.getDelegateClassPropertyList().isEmpty()
            && !this.remoteObservationManagerContext.isRemoteState()) {
            XWikiDocument userDoc = (XWikiDocument) source;
            UserReference userReference = this.userReferenceResolver.resolve(userDoc.getDocumentReference());
            try {
                this.delegateApproverManagerProvider.get().computeDelegates(userReference);
            } catch (ChangeRequestException e) {
                logger.error("Error while computing delegate approvers for [{}]", userReference, e);
            }
        }
    }
}
