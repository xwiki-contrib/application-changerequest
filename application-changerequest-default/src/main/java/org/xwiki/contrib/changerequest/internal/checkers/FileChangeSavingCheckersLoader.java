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
package org.xwiki.contrib.changerequest.internal.checkers;

import java.util.List;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.FileChangeSavingChecker;

/**
 * Helper for loading the {@link FileChangeSavingChecker} components with respect of their priority.
 *
 * @version $Id$
 * @since 1.6
 */
@Component(roles = FileChangeSavingCheckersLoader.class)
@Singleton
public class FileChangeSavingCheckersLoader
{
    @Inject
    @Named("context")
    private ComponentManager componentManager;

    /**
     * Retrieve the list of {@link FileChangeSavingChecker} ordered by their {@link Priority}.
     *
     * @return the ordered list of instances.
     * @throws ChangeRequestException in case of problem for loading the components
     */
    public List<FileChangeSavingChecker> getCheckers() throws ChangeRequestException
    {
        try {
            List<FileChangeSavingChecker> checkers =
                this.componentManager.getInstanceList(FileChangeSavingChecker.class);

            checkers.sort((checker1, checker2) -> {
                Priority priority1 = checker1.getClass().getAnnotation(Priority.class);
                Priority priority2 = checker2.getClass().getAnnotation(Priority.class);

                int priorityValue1 = (priority1 != null) ? priority1.value() : Integer.MAX_VALUE;
                int priorityValue2 = (priority2 != null) ? priority2.value() : Integer.MAX_VALUE;
                return priorityValue1 - priorityValue2;
            });

            return checkers;
        } catch (ComponentLookupException e) {
            throw new ChangeRequestException("Error when trying to retrieve the list of FileChangeSavingChecker", e);
        }
    }
}
