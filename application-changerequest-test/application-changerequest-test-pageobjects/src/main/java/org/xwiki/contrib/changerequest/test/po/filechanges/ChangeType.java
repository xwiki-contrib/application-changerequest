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
package org.xwiki.contrib.changerequest.test.po.filechanges;

import org.apache.commons.lang3.StringUtils;

/**
 * Represents the different change type displayed in the filechange live data.
 *
 * @version $Id$
 * @since 1.4.1
 */
public enum ChangeType
{
    /**
     * When the change is a page creation.
     */
    CREATION,

    /**
     * When the change is a page edition.
     */
    EDITION,

    /**
     * When the change is a page deletion.
     */
    DELETION,

    /**
     * When there's no more change for the page.
     * Note that the UI is displaying "No change", so be careful to use {@link #relaxedValueOf(String)}.
     */
    NO_CHANGE;

    /**
     * An improved version of {@link #valueOf(String)} which automatically use an uppercase version of the argument
     * and which perform custom check for {@link #NO_CHANGE}.
     *
     * @param value the value for which to find a {@link ChangeType}.
     * @return a change type matching the given value.
     */
    public static ChangeType relaxedValueOf(String value)
    {
        String upperCaseValue = value.toUpperCase();
        if (StringUtils.equals("NO CHANGE", upperCaseValue)) {
            return NO_CHANGE;
        } else {
            return ChangeType.valueOf(upperCaseValue);
        }
    }
}
