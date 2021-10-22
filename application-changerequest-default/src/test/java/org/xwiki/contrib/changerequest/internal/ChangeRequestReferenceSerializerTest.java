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

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.contrib.changerequest.ChangeRequestReference;
import org.xwiki.resource.SerializeResourceReferenceException;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.url.ExtendedURL;
import org.xwiki.url.URLNormalizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ChangeRequestReferenceSerializer}.
 *
 * @version $Id$
 * @since 0.6
 */
@ComponentTest
class ChangeRequestReferenceSerializerTest
{
    @InjectMockComponents
    private ChangeRequestReferenceSerializer serializer;

    @MockComponent
    @Named("contextpath")
    private URLNormalizer<ExtendedURL> extendedURLNormalizer;

    @Test
    void serialize() throws SerializeResourceReferenceException, UnsupportedResourceReferenceException
    {
        ChangeRequestReference resource =
            new ChangeRequestReference(ChangeRequestReference.ChangeRequestAction.ADDCHANGES, "someId");
        resource.addParameter("key", "value");

        when(extendedURLNormalizer.normalize(any(ExtendedURL.class)))
            .then(invocationOnMock -> invocationOnMock.getArgument(0));
        ExtendedURL expected = new ExtendedURL(Arrays.asList("changerequest", "addchanges", "someId"),
            Collections.singletonMap("key", Collections.singletonList("value")));
        assertEquals(expected, this.serializer.serialize(resource));
        verify(this.extendedURLNormalizer).normalize(expected);

        resource =
            new ChangeRequestReference(ChangeRequestReference.ChangeRequestAction.CREATE, null);
        expected = new ExtendedURL(Arrays.asList("changerequest", "create"), Collections.emptyMap());
        assertEquals(expected, this.serializer.serialize(resource));
        verify(this.extendedURLNormalizer).normalize(expected);
    }
}
