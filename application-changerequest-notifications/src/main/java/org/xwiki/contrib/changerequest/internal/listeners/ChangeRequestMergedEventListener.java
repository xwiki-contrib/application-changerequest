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
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.events.ChangeRequestMergedEvent;
import org.xwiki.contrib.changerequest.internal.ChangeRequestAutoWatchHandler;
import org.xwiki.notifications.filters.watch.AutomaticWatchMode;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.user.UserReference;

/**
 * Listener in charge of handling autowatch for document that have been merged.
 *
 * @version $Id$
 * @since 1.10
 */
@Component
@Named(ChangeRequestMergedEventListener.NAME)
@Singleton
public class ChangeRequestMergedEventListener extends AbstractEventListener
{
    static final String NAME = "ChangeRequestMergedEventListener";

    @Inject
    private ChangeRequestAutoWatchHandler autoWatchHandler;

    @Inject
    private Logger logger;

    /**
     * Default constructor.
     */
    public ChangeRequestMergedEventListener()
    {
        super(NAME, List.of(new ChangeRequestMergedEvent()));
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        ChangeRequest changeRequest = (ChangeRequest) data;
        List<FileChange> lastFileChanges = changeRequest.getLastFileChanges();
        for (FileChange fileChange : lastFileChanges) {
            FileChange.FileChangeType type = fileChange.getType();
            for (UserReference author : changeRequest.getAuthors()) {
                AutomaticWatchMode watchMode = this.autoWatchHandler.getAutomaticWatchMode(author);
                boolean createWatch = false;
                if (type == FileChange.FileChangeType.CREATION) {
                    createWatch = watchMode != AutomaticWatchMode.NONE;
                } else if (type == FileChange.FileChangeType.EDITION) {
                    createWatch = (watchMode == AutomaticWatchMode.ALL || watchMode == AutomaticWatchMode.MAJOR);
                }
                if (createWatch) {
                    try {
                        this.autoWatchHandler.watchDocument(fileChange.getTargetEntity(), author);
                    } catch (ChangeRequestException e) {
                        this.logger.error("Error while trying to autowatch [{}] after merge of change request "
                            + "[{}]", fileChange.getTargetEntity(), changeRequest.getId(), e);
                    }
                }
            }
        }
    }
}
