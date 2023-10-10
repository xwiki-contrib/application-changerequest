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
package org.xwiki.contrib.changerequest.discussions.internal;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.discussions.ChangeRequestDiscussionException;
import org.xwiki.contrib.changerequest.discussions.ChangeRequestDiscussionService;
import org.xwiki.contrib.changerequest.discussions.references.AbstractChangeRequestDiscussionContextReference;
import org.xwiki.contrib.changerequest.storage.ChangeRequestStorageManager;
import org.xwiki.contrib.discussions.domain.Discussion;
import org.xwiki.contrib.discussions.domain.Message;
import org.xwiki.contrib.discussions.events.ActionType;
import org.xwiki.contrib.discussions.events.MessageEvent;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.observation.event.AbstractLocalEventListener;
import org.xwiki.observation.event.Event;

/**
 * Listener responsible for created message and to update change requests whenever a message is created.
 *
 * @version $Id$
 * @since 1.12
 */
@Component
@Singleton
@Named(MessageEventListener.NAME)
public class MessageEventListener extends AbstractLocalEventListener
{
    static final String NAME = "org.xwiki.contrib.changerequest.discussions.internal.MessageEventListener";

    @Inject
    private Provider<ChangeRequestDiscussionService> discussionServiceProvider;

    @Inject
    private Provider<ChangeRequestStorageManager> changeRequestStorageManagerProvider;

    @Inject
    private Provider<ContextualLocalizationManager> contextualLocalizationManagerProvider;

    @Inject
    private Logger logger;

    /**
     * Default constructor.
     */
    public MessageEventListener()
    {
        super(NAME, new MessageEvent(ActionType.CREATE));
    }

    @Override
    public void processLocalEvent(Event event, Object source, Object data)
    {
        String applicationHint = (String) source;
        Message message = (Message) data;

        if (StringUtils.equals(ChangeRequestDiscussionService.APPLICATION_HINT, applicationHint)) {
            Discussion discussion = message.getDiscussion();
            String changeRequestId = "";
            try {
                AbstractChangeRequestDiscussionContextReference reference =
                    this.discussionServiceProvider.get().getReferenceFrom(discussion);
                changeRequestId = reference.getChangeRequestId();
                Optional<ChangeRequest> changeRequestOpt = this.changeRequestStorageManagerProvider.get()
                    .load(changeRequestId);
                if (changeRequestOpt.isPresent()) {
                    ChangeRequest changeRequest = changeRequestOpt.get();
                    changeRequest.updateDate();
                    String saveComment = this.contextualLocalizationManagerProvider.get()
                        .getTranslationPlain("changerequest.save.messagecreated");
                    this.changeRequestStorageManagerProvider.get()
                        .save(changeRequest, saveComment);
                } else {
                    this.logger.warn("No change request found with id [{}]", changeRequestId);
                }
            } catch (ChangeRequestDiscussionException e) {
                this.logger.error("Error when trying to get reference from discussion [{}]", discussion, e);
            } catch (ChangeRequestException e) {
                this.logger.error("Error when trying to load or save change request [{}]", changeRequestId, e);
            }
        }
    }
}
