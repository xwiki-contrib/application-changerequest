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
package org.xwiki.contrib.changerequest.discussions.references.difflocation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.stability.Unstable;

/**
 * Abstract representation of a diff location to hold common methods.
 *
 * @version $Id$
 * @since 0.8
 */
@Unstable
public abstract class AbstractDiffLocation
{
    protected static final char SEPARATOR = '/';
    private static final char ESCAPE = '\\';
    private static final String[] REPLACED_STRING = new String[] { SEPARATOR + "", ESCAPE + "" };
    private static final String[] REPLACEMENT_STRING = new String[] { ESCAPE + "" + SEPARATOR, ESCAPE + "" + ESCAPE };

    private String escape(String part)
    {
        return StringUtils.replaceEach(part, REPLACED_STRING, REPLACEMENT_STRING);
    }

    protected String getSerializedString(List<String> tokens)
    {
        StringBuilder result = new StringBuilder();
        Iterator<String> iterator = tokens.iterator();
        while (iterator.hasNext()) {
            String token = iterator.next();
            result.append(this.escape(token));

            if (iterator.hasNext()) {
                result.append(SEPARATOR);
            }
        }
        return result.toString();
    }

    protected static List<String> parseToList(String reference)
    {
        List<String> result = new ArrayList<>();
        int i = 0;
        boolean inEscape = false;
        char currentChar;
        StringBuilder currentToken = new StringBuilder();
        while (i < reference.length()) {
            currentChar = reference.charAt(i++);

            if (currentChar == SEPARATOR) {
                if (inEscape) {
                    currentToken.append(currentChar);
                    inEscape = false;
                } else {
                    result.add(currentToken.toString());
                    currentToken = new StringBuilder();
                }
            } else if (currentChar == ESCAPE) {
                if (inEscape) {
                    currentToken.append(currentChar);
                    inEscape = false;
                } else {
                    inEscape = true;
                }
            } else {
                currentToken.append(currentChar);
            }
        }
        result.add(currentToken.toString());
        return result;
    }
}
