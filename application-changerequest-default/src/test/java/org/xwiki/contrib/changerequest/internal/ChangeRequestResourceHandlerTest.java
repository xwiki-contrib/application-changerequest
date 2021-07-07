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

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.changerequest.ChangeRequestReference;
import org.xwiki.contrib.changerequest.internal.handlers.ChangeRequestActionHandler;
import org.xwiki.contrib.changerequest.internal.handlers.CreateChangeRequestHandler;
import org.xwiki.contrib.changerequest.internal.handlers.MergeChangeRequestHandler;
import org.xwiki.contrib.changerequest.rights.ChangeRequestApproveRight;
import org.xwiki.contrib.changerequest.rights.ChangeRequestRight;
import org.xwiki.resource.ResourceReferenceHandlerChain;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ChangeRequestResourceHandler}.
 *
 * @version $Id$
 * @since 0.1
 */
@ComponentTest
class ChangeRequestResourceHandlerTest
{
    @InjectMockComponents
    private ChangeRequestResourceHandler resourceHandler;

    @MockComponent
    private AuthorizationManager authorizationManager;

    @BeforeComponent
    void beforeComponent(MockitoComponentManager componentManager) throws Exception
    {
        componentManager.registerComponent(ComponentManager.class, "context", componentManager);
    }

    @Test
    void initialize() throws Exception
    {
        this.resourceHandler.initialize();
        verify(this.authorizationManager, times(2)).register(ChangeRequestRight.INSTANCE);
        verify(this.authorizationManager, times(2)).register(ChangeRequestApproveRight.INSTANCE);
    }

    @Test
    void dispose() throws Exception
    {
        this.resourceHandler.dispose();
        verify(this.authorizationManager, times(2)).unregister(any());
    }

    @Test
    void getSupportedResourceReferences()
    {
        assertEquals(Collections.singletonList(ChangeRequestReference.TYPE),
            this.resourceHandler.getSupportedResourceReferences());
    }

    @Test
    void handle(MockitoComponentManager componentManager) throws Exception
    {
        ChangeRequestActionHandler createActionHandler =
            componentManager.registerMockComponent(ChangeRequestActionHandler.class, "create");
        ChangeRequestActionHandler mergeActionHandler =
            componentManager.registerMockComponent(ChangeRequestActionHandler.class, "merge");
        ChangeRequestReference reference = mock(ChangeRequestReference.class);
        ResourceReferenceHandlerChain chain = mock(ResourceReferenceHandlerChain.class);

        when(reference.getAction()).thenReturn(ChangeRequestReference.ChangeRequestAction.CREATE);
        this.resourceHandler.handle(reference, chain);
        verify(createActionHandler).handle(reference);
        verify(chain).handleNext(reference);

        when(reference.getAction()).thenReturn(ChangeRequestReference.ChangeRequestAction.MERGE);
        this.resourceHandler.handle(reference, chain);
        verify(mergeActionHandler).handle(reference);
        verify(chain, times(2)).handleNext(reference);
    }
}
