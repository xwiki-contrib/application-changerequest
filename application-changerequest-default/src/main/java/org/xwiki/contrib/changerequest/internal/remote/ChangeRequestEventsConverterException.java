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
package org.xwiki.contrib.changerequest.internal.remote;

/**
 * Internal exception to be used in converters and helpers.
 *
 * @version $Id$
 * @since 1.5
 */
public class ChangeRequestEventsConverterException extends Exception
{
    /**
     * Default constructor.
     * @param message the message of the exception.
     */
    public ChangeRequestEventsConverterException(String message)
    {
        super(message);
    }

    /**
     * Default constructor with cause.
     * @param message the message of the exception.
     * @param throwable the actual cause.
     */
    public ChangeRequestEventsConverterException(String message, Throwable throwable)
    {
        super(message, throwable);
    }
}
