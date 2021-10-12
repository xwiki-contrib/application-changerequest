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
package org.xwiki.contrib.changerequest.internal.id;

import java.util.regex.Pattern;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.Test;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.model.validation.EntityNameValidation;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link TitleChangeRequestIDGenerator}.
 *
 * @version $Id$
 * @since 0.6
 */
@ComponentTest
class TitleChangeRequestIDGeneratorTest
{
    @InjectMockComponents
    private TitleChangeRequestIDGenerator generator;

    @MockComponent
    @Named("SlugEntityNameValidation")
    private Provider<EntityNameValidation> slugEntityNameValidation;

    @Test
    void generateId()
    {
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        EntityNameValidation entityNameValidation = mock(EntityNameValidation.class);
        when(slugEntityNameValidation.get()).thenReturn(entityNameValidation);

        when(changeRequest.getTitle()).thenReturn("CR 1");
        when(entityNameValidation.transform("CR 1")).thenReturn("CR1");
        String obtainedTitle = this.generator.generateId(changeRequest);
        String uuidPattern = "([a-f0-9]){8}-([a-f0-9]){4}-([a-f0-9]){4}-([a-f0-9]){4}-([a-f0-9]){12}";
        assertTrue(Pattern.matches(String.format("^CR1-%s$", uuidPattern), obtainedTitle),
            String.format("Obtained title [%s] does not respect the pattern", obtainedTitle));

        when(changeRequest.getTitle()).thenReturn("This is a very long title with lots of words for no reason");
        when(entityNameValidation.transform("This is a very long title with lots of words for no reason"))
            .thenReturn("This-is-a-very-long-title-with-lots-of-words-for-no-reason");
        obtainedTitle = this.generator.generateId(changeRequest);
        assertTrue(Pattern.matches(String.format("^This-is-a-very-long-title-w-%s$", uuidPattern), obtainedTitle),
            String.format("Obtained title [%s] does not respect the pattern", obtainedTitle));
    }
}
