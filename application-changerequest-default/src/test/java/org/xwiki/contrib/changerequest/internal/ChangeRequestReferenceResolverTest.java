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

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.xwiki.contrib.changerequest.ChangeRequestReference;
import org.xwiki.resource.CreateResourceReferenceException;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.url.ExtendedURL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ChangeRequestReferenceResolver}.
 *
 * @version $Id$
 * @since 0.1
 */
@ComponentTest
class ChangeRequestReferenceResolverTest
{
    @InjectMockComponents
    private ChangeRequestReferenceResolver referenceResolver;

    @Test
    void resolveWrongURL()
    {
        ExtendedURL extendedURL = mock(ExtendedURL.class);
        when(extendedURL.getSegments()).thenReturn(Collections.emptyList());
        when(extendedURL.toString()).thenReturn("myURL");

        CreateResourceReferenceException createResourceReferenceException =
            assertThrows(CreateResourceReferenceException.class,
                () -> this.referenceResolver.resolve(extendedURL, null, null));
        assertEquals("Invalid Change Request URL format: the format should contain an action and an ID. "
            + "Provided URL: [myURL]", createResourceReferenceException.getMessage());

        when(extendedURL.getSegments()).thenReturn(Arrays.asList("foo", "bar", "baz"));
        when(extendedURL.toString()).thenReturn("otherURL");

        createResourceReferenceException =
            assertThrows(CreateResourceReferenceException.class,
                () -> this.referenceResolver.resolve(extendedURL, null, null));
        assertEquals("Invalid Change Request URL format: the format should contain an action and an ID. "
            + "Provided URL: [otherURL]", createResourceReferenceException.getMessage());
    }

    @Test
    void resolveWrongAction()
    {
        ExtendedURL extendedURL = mock(ExtendedURL.class);
        when(extendedURL.getSegments()).thenReturn(Collections.singletonList("myaction"));
        UnsupportedResourceReferenceException unsupportedResourceReferenceException =
            assertThrows(UnsupportedResourceReferenceException.class,
                () -> this.referenceResolver.resolve(extendedURL, null, null));
        assertEquals("The given action is invalid for Change Request: [myaction].",
            unsupportedResourceReferenceException.getMessage());

        when(extendedURL.getSegments()).thenReturn(Collections.singletonList("merge"));
        when(extendedURL.toString()).thenReturn("myURL");
        CreateResourceReferenceException createResourceReferenceException =
            assertThrows(CreateResourceReferenceException.class,
                () -> this.referenceResolver.resolve(extendedURL, null, null));
        assertEquals("Invalid Change Request URL format: the format should contain an action and an ID. "
            + "Provided URL: [myURL]", createResourceReferenceException.getMessage());
    }

    @Test
    void resolve() throws UnsupportedResourceReferenceException, CreateResourceReferenceException
    {
        ExtendedURL extendedURL = mock(ExtendedURL.class);
        when(extendedURL.getSegments()).thenReturn(Collections.singletonList("create"));
        when(extendedURL.getParameters()).thenReturn(Collections.singletonMap("foo", Arrays.asList("bar", "baz")));

        ChangeRequestReference expected =
            new ChangeRequestReference(ChangeRequestReference.ChangeRequestAction.CREATE, null);
        expected.addParameter("foo", Arrays.asList("bar", "baz"));
        assertEquals(expected, this.referenceResolver.resolve(extendedURL, null, null));

        when(extendedURL.getSegments()).thenReturn(Arrays.asList("merge", "myid42"));
        when(extendedURL.getParameters()).thenReturn(Collections.singletonMap("bar", Arrays.asList("42", "43")));

        expected =
            new ChangeRequestReference(ChangeRequestReference.ChangeRequestAction.MERGE, "myid42");
        expected.addParameter("bar", Arrays.asList("42", "43"));
        assertEquals(expected, this.referenceResolver.resolve(extendedURL, null, null));
    }
}
