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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ChangeRequestTitleChecker}.
 *
 * @version $Id$
 */
class ChangeRequestTitleCheckerTest
{
    @ParameterizedTest
    @ValueSource(strings = {
        "$name",
        "Title $name",
        "Title $!name",
        "Title ${name}",
        "Title $_name",
        "#set ($a = 1)",
        "Title #{set}($a = 1)",
        "Title #macro()",
        "Title #@macro()",
        "Title ## comment",
        "Title #* comment *#",
        "Title #[[ unparsed ]]#"
    })
    void containsScript(String title)
    {
        assertTrue(ChangeRequestTitleChecker.containsScript(title));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "",
        "Some title",
        "Fix issue #123",
        "Price of $5",
        "C# and $ alone",
        "Title with {{velocity}} braces"
    })
    void doesNotContainScript(String title)
    {
        assertFalse(ChangeRequestTitleChecker.containsScript(title));
    }

    @Test
    void containsScriptWithNull()
    {
        assertFalse(ChangeRequestTitleChecker.containsScript(null));
    }
}
