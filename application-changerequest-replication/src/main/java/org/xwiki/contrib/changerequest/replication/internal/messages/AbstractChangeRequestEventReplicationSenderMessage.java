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
package org.xwiki.contrib.changerequest.replication.internal.messages;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Provider;

import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.contrib.replication.AbstractReplicationMessage;
import org.xwiki.contrib.replication.ReplicationSenderMessage;
import org.xwiki.model.reference.EntityReferenceSerializer;

import com.xpn.xwiki.XWikiContext;

@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public abstract class AbstractChangeRequestEventReplicationSenderMessage extends AbstractReplicationMessage
    implements ReplicationSenderMessage
{
    public static final String CHANGE_REQUEST_ID_PARAMETER = "CHANGE_REQUEST_ID";
    public static final String MODIFIED_DOCUMENT = "MODIFIED_DOCUMENT";

    public static final String CONTEXT_USER = "CONTEXT_USER";

    @Inject
    protected EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    private Provider<XWikiContext> contextProvider;

    private final String id;
    private final Date date;
    private final String type;

    public AbstractChangeRequestEventReplicationSenderMessage(String type)
    {
        this.id = UUID.randomUUID().toString();
        this.date = new Date();
        this.type = type;
    }

    @Override
    public String getId()
    {
        return this.id;
    }

    @Override
    public Date getDate()
    {
        return this.date;
    }

    @Override
    public String getSource()
    {
        return null;
    }

    @Override
    public String getType()
    {
        return this.type;
    }

    protected void saveContextUser()
    {
        String serializedUser = this.entityReferenceSerializer.serialize(contextProvider.get().getUserReference());
        this.putCustomMetadata(CONTEXT_USER, serializedUser);
    }

    @Override
    public void write(OutputStream stream) throws IOException
    {
        // No content associated with this event
    }
}
