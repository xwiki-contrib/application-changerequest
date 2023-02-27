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

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.internal.cache.ChangeRequestStorageCacheManager;
import org.xwiki.contrib.changerequest.internal.storage.ReviewXClassInitializer;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.RegexEntityReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.XObjectAddedEvent;
import com.xpn.xwiki.internal.event.XObjectDeletedEvent;
import com.xpn.xwiki.internal.event.XObjectUpdatedEvent;
import com.xpn.xwiki.objects.BaseObjectReference;

/**
 * Listener dedicated to invalidate the change request cache entry whenever the review xobject is
 * updated, added or deleted.
 *
 * @version $Id$
 * @since 0.16
 */
@Component
@Singleton
@Named(ReviewXObjectUpdatedListener.NAME)
public class ReviewXObjectUpdatedListener extends AbstractEventListener
{
    static final String NAME = "org.xwiki.contrib.changerequest.internal.listeners.ReviewXObjectUpdatedListener";

    static final RegexEntityReference REFERENCE =
        BaseObjectReference.any(ReviewXClassInitializer.REVIEW_XCLASS.toString());

    static final List<Event> EVENT_LIST = List.of(
        new XObjectAddedEvent(REFERENCE),
        new XObjectUpdatedEvent(REFERENCE),
        new XObjectDeletedEvent(REFERENCE)
    );

    @Inject
    @Named("changerequestid")
    private Provider<EntityReferenceSerializer<String>> entityReferenceSerializerProvider;

    @Inject
    private Provider<ChangeRequestStorageCacheManager> cacheManagerProvider;

    /**
     * Default constructor.
     */
    public ReviewXObjectUpdatedListener()
    {
        super(NAME, EVENT_LIST);
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument updatedDoc = (XWikiDocument) source;
        String changeRequestId =
            this.entityReferenceSerializerProvider.get().serialize(updatedDoc.getDocumentReference());
        this.cacheManagerProvider.get().invalidate(changeRequestId);
    }
}
