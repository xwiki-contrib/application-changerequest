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
package org.xwiki.contrib.changerequest.internal;

import java.util.regex.Pattern;

/**
 * Checks the titles given to change requests.
 * <p>
 * A change request title is a plain label, but the title of a document is interpreted as Velocity when it's displayed,
 * so the titles that Velocity would interpret are not accepted.
 *
 * @version $Id$
 * @since 1.26
 */
public final class ChangeRequestTitleChecker
{
    /**
     * Matches the start of a Velocity reference ({@code $name}, {@code $!name}, {@code ${name}}), directive or macro
     * call ({@code #name}, {@code #{name}}, {@code #@name}), comment ({@code ##}, {@code #*}) or unparsed content
     * ({@code #[[}). A {@code #} or {@code $} followed by anything else (e.g. {@code #123} or {@code $5}) is output
     * as is by Velocity.
     */
    private static final Pattern SCRIPT_PATTERN =
        Pattern.compile("\\$!?\\{?[a-zA-Z_]|#(\\{?[a-zA-Z_@]|[#*]|\\[\\[)");

    private ChangeRequestTitleChecker()
    {
    }

    /**
     * @param title a change request title
     * @return {@code true} if the title contains script syntax and should not be accepted
     */
    public static boolean containsScript(String title)
    {
        return title != null && SCRIPT_PATTERN.matcher(title).find();
    }
}
