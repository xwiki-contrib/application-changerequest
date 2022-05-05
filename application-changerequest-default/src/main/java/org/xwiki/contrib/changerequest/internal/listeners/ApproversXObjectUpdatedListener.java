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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.events.ApproversUpdatedEvent;
import org.xwiki.contrib.changerequest.events.ChangeRequestUpdatingFileChangeEvent;
import org.xwiki.contrib.changerequest.internal.approvers.ApproversXClassInitializer;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.RegexEntityReference;
import org.xwiki.observation.ObservationContext;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.XObjectUpdatedEvent;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseObjectReference;

/**
 * Listener in charge of triggering events whenever the list of approvers has been updated.
 *
 * @version $Id$
 * @since 0.10
 */
@Component
@Named(ApproversXObjectUpdatedListener.NAME)
@Singleton
public class ApproversXObjectUpdatedListener extends AbstractLocalEventListener
{
    /**
     * The name of the listener.
     */
    static final String NAME = "org.xwiki.contrib.changerequest.internal.listeners.ApproversXObjectUpdatedListener";

    private static final RegexEntityReference REFERENCE =
        BaseObjectReference.any(ApproversXClassInitializer.APPROVERS_XCLASS.toString());

    private static final List<Event> EVENT_LIST = Collections.singletonList(
        new XObjectUpdatedEvent(REFERENCE)
    );

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    private ObservationContext observationContext;

    @Inject
    private ObservationManager observationManager;

    /**
     * Default constructor.
     */
    public ApproversXObjectUpdatedListener()
    {
        super(NAME, EVENT_LIST);
    }

    @Override
    public void processLocalEvent(Event event, Object source, Object data)
    {
        if (!this.observationContext.isIn(new ChangeRequestUpdatingFileChangeEvent())) {
            XWikiDocument document = (XWikiDocument) source;
            XWikiDocument originalDoc = ((XWikiDocument) source).getOriginalDocument();

            BaseObject currentObject = document.getXObject(ApproversXClassInitializer.APPROVERS_XCLASS);
            BaseObject previousObject = originalDoc.getXObject(ApproversXClassInitializer.APPROVERS_XCLASS);

            String[] currentUsersApprovers =
                this.getValues(currentObject, ApproversXClassInitializer.USERS_APPROVERS_PROPERTY);
            String[] previousUsersApprovers =
                this.getValues(previousObject, ApproversXClassInitializer.USERS_APPROVERS_PROPERTY);

            String[] currentGroupApprovers =
                this.getValues(currentObject, ApproversXClassInitializer.GROUPS_APPROVERS_PROPERTY);
            String[] previousGroupApprovers =
                this.getValues(previousObject, ApproversXClassInitializer.GROUPS_APPROVERS_PROPERTY);

            Set<String> targets = new HashSet<>();
            targets.addAll(this.resolveAndSerializeLists(currentUsersApprovers));
            targets.addAll(this.resolveAndSerializeLists(previousUsersApprovers));
            targets.addAll(this.resolveAndSerializeLists(currentGroupApprovers));
            targets.addAll(this.resolveAndSerializeLists(previousGroupApprovers));

            // filter out possible null values
            targets = targets.stream().filter(Objects::nonNull).collect(Collectors.toSet());
            this.observationManager.notify(new ApproversUpdatedEvent(), source, targets);
        }
    }

    private List<String> resolveAndSerializeLists(String[] list)
    {
        List<String> result = new ArrayList<>();
        for (String serializedRef : list) {
            if (!StringUtils.isEmpty(serializedRef)) {
                DocumentReference reference = this.documentReferenceResolver.resolve(serializedRef);
                result.add(this.entityReferenceSerializer.serialize(reference));
            }
        }
        return result;
    }

    private String[] getValues(BaseObject baseObject, String propertyName)
    {
        String largeStringValue = baseObject.getLargeStringValue(propertyName);
        if (largeStringValue == null) {
            return new String[0];
        } else {
            return StringUtils.split(largeStringValue, ApproversXClassInitializer.SEPARATOR_CHARACTER);
        }
    }
}
