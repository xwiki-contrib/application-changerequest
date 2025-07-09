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

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.discussions.ChangeRequestDiscussionException;
import org.xwiki.contrib.changerequest.discussions.ChangeRequestDiscussionService;
import org.xwiki.contrib.changerequest.discussions.references.AbstractChangeRequestDiscussionContextReference;
import org.xwiki.contrib.discussions.DiscussionContextService;
import org.xwiki.contrib.discussions.DiscussionException;
import org.xwiki.contrib.discussions.DiscussionStoreConfigurationParameters;
import org.xwiki.contrib.discussions.MessageService;
import org.xwiki.contrib.discussions.domain.Discussion;
import org.xwiki.contrib.discussions.domain.DiscussionContext;
import org.xwiki.contrib.discussions.domain.Message;
import org.xwiki.contrib.discussions.domain.references.DiscussionContextEntityReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Internal component for managing the creation of various elements related to discussions.
 *
 * @version $Id$
 * @since 0.7
 */
@Component(roles = ChangeRequestDiscussionFactory.class)
@Singleton
public class ChangeRequestDiscussionFactory
{
    static final String CR_ID_REF_ID_SEPARATOR = "__CRREF__";
    @Inject
    private ChangeRequestDiscussionReferenceUtils discussionReferenceUtils;

    @Inject
    private DiscussionContextService discussionContextService;

    @Inject
    @Named("changerequestid")
    private DocumentReferenceResolver<String> changeRequestIdDocumentReferenceResolver;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    private MessageService messageService;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private Logger logger;

    private <T extends AbstractChangeRequestDiscussionContextReference> DiscussionContextEntityReference
        createContextEntityReferenceFor(T reference)
    {
        String entityReference;
        if (!StringUtils.isEmpty(reference.getReference())) {
            entityReference = String.format("%s%s%s",
                reference.getChangeRequestId(), CR_ID_REF_ID_SEPARATOR, reference.getReference());
        } else {
            entityReference = reference.getChangeRequestId();
        }
        return new DiscussionContextEntityReference(
            String.format("changerequest-%s", reference.getType().name().toLowerCase()),
            entityReference
        );
    }

    /**
     * Create a {@link DiscussionStoreConfigurationParameters} containing only the change request Id parameter
     * matching the given reference.
     *
     * @param reference the reference from which to retrieve the change request identifier
     * @param <T> the real type of the reference
     * @return a discussion store configuration parameter with the change request identifier.
     */
    public <T extends AbstractChangeRequestDiscussionContextReference> DiscussionStoreConfigurationParameters
        createDiscussionStoreConfigurationParametersFor(T reference)
    {
        DiscussionStoreConfigurationParameters configurationParameters = new DiscussionStoreConfigurationParameters();
        String changeRequestId = reference.getChangeRequestId();
        configurationParameters.put(DefaultChangeRequestDiscussionStoreConfiguration.CHANGE_REQUEST_ID_PARAMETER_KEY,
            changeRequestId);

        DocumentReference crReference = this.changeRequestIdDocumentReferenceResolver.resolve(changeRequestId);
        SpaceReference crSpaceReference = new SpaceReference(crReference.getName(),
            crReference.getLastSpaceReference());
        DocumentReference crHomeReference = new DocumentReference("WebHome", crSpaceReference);
        XWikiContext context = this.contextProvider.get();
        try {
            XWikiDocument crDocument = context.getWiki().getDocument(crHomeReference, context);
            configurationParameters.put(DiscussionStoreConfigurationParameters.CREATOR_PARAMETER_KEY,
                crDocument.getAuthors().getCreator());
        } catch (XWikiException e) {
            this.logger.error("Error while trying to load the CR main document [{}] in order to set the creator of "
                + "discussion documents.", crHomeReference, e);
        }
        configurationParameters.put("redirection", this.entityReferenceSerializer.serialize(crReference));
        return configurationParameters;
    }

    /**
     * Create a context specifically for the given reference.
     *
     * @param reference the reference for which to create a context.
     * @param <T> the real type of the reference.
     * @return the created discussion context.
     * @throws ChangeRequestDiscussionException if the context has not been created.
     */
    public <T extends AbstractChangeRequestDiscussionContextReference> DiscussionContext getOrCreateContextFor(
        T reference)
        throws ChangeRequestDiscussionException
    {
        DiscussionContextEntityReference contextEntityReference = this.createContextEntityReferenceFor(reference);
        try {
            return this.discussionContextService.getOrCreate(
                ChangeRequestDiscussionService.APPLICATION_HINT,
                this.discussionReferenceUtils.getTitleTranslation(
                    ChangeRequestDiscussionReferenceUtils.DISCUSSION_CONTEXT_TRANSLATION_PREFIX, reference),
                this.discussionReferenceUtils.getDescriptionTranslation(
                    ChangeRequestDiscussionReferenceUtils.DISCUSSION_CONTEXT_TRANSLATION_PREFIX, reference),
                contextEntityReference,
                createDiscussionStoreConfigurationParametersFor(reference));
        } catch (DiscussionException e) {
            throw new ChangeRequestDiscussionException(
                String.format("Error while getting or creating discussion context for reference [%s]", reference), e);
        }
    }

    /**
     * Allow to copy messages from the given discussion to a new discussion, with the given new reference.
     *
     * @param originalDiscussion the discussion from which to get the messages to copy.
     * @param newDiscussion the target for the copied messages.
     * @param newReference the reference to use for discussion store parameters.
     */
    public void copyMessages(Discussion originalDiscussion, Discussion newDiscussion,
        AbstractChangeRequestDiscussionContextReference newReference) throws ChangeRequestDiscussionException
    {
        long limit = this.messageService.countByDiscussion(originalDiscussion);
        for (int offset = 0; offset < limit; offset += 100) {
            List<Message> messages =
                this.messageService.getByDiscussion(originalDiscussion.getReference(), offset, 100);

            for (Message message : messages) {
                this.copyMessage(message, newDiscussion, newReference);
            }
        }
    }

    private void copyMessage(Message message, Discussion newDiscussion,
        AbstractChangeRequestDiscussionContextReference newReference) throws ChangeRequestDiscussionException
    {
        DiscussionStoreConfigurationParameters storeConfigurationParameters =
            this.createDiscussionStoreConfigurationParametersFor(newReference);
        try {
            this.messageService.create(
                message.getContent(),
                message.getSyntax(),
                newDiscussion.getReference(),
                message.getActorReference(),
                storeConfigurationParameters);
        } catch (DiscussionException e) {
            throw new ChangeRequestDiscussionException(String.format("Error while creating message for discussion "
                + "[%s]", newDiscussion.getReference()), e);
        }
    }
}
