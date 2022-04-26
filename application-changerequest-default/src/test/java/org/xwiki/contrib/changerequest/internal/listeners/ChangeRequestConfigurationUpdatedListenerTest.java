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
package org.xwiki.contrib.changerequest.internal.listeners;

import java.util.Collections;
import java.util.List;

import javax.inject.Provider;

import org.junit.jupiter.api.Test;
import org.xwiki.contrib.changerequest.ChangeRequestConfiguration;
import org.xwiki.contrib.changerequest.internal.ChangeRequestConfigurationSource;
import org.xwiki.contrib.changerequest.internal.DefaultChangeRequestConfiguration;
import org.xwiki.contrib.changerequest.internal.jobs.DelegateApproversComputationRequest;
import org.xwiki.job.JobExecutor;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.remote.RemoteObservationManagerContext;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ChangeRequestConfigurationUpdatedListener}.
 *
 * @version $Id$
 * @since 0.13
 */
@ComponentTest
class ChangeRequestConfigurationUpdatedListenerTest
{
    @InjectMockComponents
    private ChangeRequestConfigurationUpdatedListener configurationUpdatedListener;

    @MockComponent
    private RemoteObservationManagerContext remoteObservationManagerContext;

    @MockComponent
    private ChangeRequestConfiguration configuration;

    @MockComponent
    private Provider<QueryManager> queryManagerProvider;

    @MockComponent
    private DocumentReferenceResolver<String> stringDocumentReferenceResolver;

    @MockComponent
    private Provider<JobExecutor> jobExecutorProvider;

    @Test
    void onEventNoRecomputation()
    {
        XWikiDocument source = mock(XWikiDocument.class);
        when(this.configuration.isDelegateEnabled()).thenReturn(false);
        when(this.configuration.getDelegateClassPropertyList()).thenReturn(Collections.emptyList());
        when(this.remoteObservationManagerContext.isRemoteState()).thenReturn(true);

        this.configurationUpdatedListener.onEvent(null, source, null);
        verifyNoInteractions(source);

        when(this.remoteObservationManagerContext.isRemoteState()).thenReturn(false);
        this.configurationUpdatedListener.onEvent(null, source, null);
        verifyNoInteractions(source);

        when(this.configuration.getDelegateClassPropertyList()).thenReturn(Collections.singletonList("delegate"));
        this.configurationUpdatedListener.onEvent(null, source, null);
        verifyNoInteractions(source);

        when(this.configuration.isDelegateEnabled()).thenReturn(true);
        when(this.remoteObservationManagerContext.isRemoteState()).thenReturn(true);
        this.configurationUpdatedListener.onEvent(null, source, null);
        verifyNoInteractions(source);

        when(this.remoteObservationManagerContext.isRemoteState()).thenReturn(false);
        when(this.configuration.isDelegateEnabled()).thenReturn(true);
        when(this.configuration.getDelegateClassPropertyList()).thenReturn(Collections.emptyList());
        this.configurationUpdatedListener.onEvent(null, source, null);
        verifyNoInteractions(source);

        when(this.configuration.getDelegateClassPropertyList()).thenReturn(Collections.singletonList("delegate"));
        when(source.getDocumentReference()).thenReturn(new DocumentReference("xwiki", "Foo", "Bar"));
        this.configurationUpdatedListener.onEvent(null, source, null);
        verify(source, never()).getOriginalDocument();
        verify(source, never()).getXObject(any(EntityReference.class));

        when(source.getDocumentReference()).thenReturn(
            new DocumentReference(ChangeRequestConfigurationSource.DOC_REFERENCE, new WikiReference("xwiki")));
        XWikiDocument originalDoc = mock(XWikiDocument.class);
        when(source.getOriginalDocument()).thenReturn(originalDoc);
        BaseObject currentObj = mock(BaseObject.class);

        when(source.getXObject(ChangeRequestConfigurationSource.CLASS_REFERENCE)).thenReturn(currentObj);
        BaseObject previousObj = mock(BaseObject.class);
        when(originalDoc.getXObject(ChangeRequestConfigurationSource.CLASS_REFERENCE)).thenReturn(previousObj);

        when(currentObj.getStringValue(DefaultChangeRequestConfiguration.DELEGATE_CLASS_PROPERTY_LIST_PROPERTY))
            .thenReturn("delegate");
        when(previousObj.getStringValue(DefaultChangeRequestConfiguration.DELEGATE_CLASS_PROPERTY_LIST_PROPERTY))
            .thenReturn("delegate");
        when(currentObj.getIntValue(DefaultChangeRequestConfiguration.DELEGATE_ENABLED_PROPERTY)).thenReturn(1);
        when(previousObj.getIntValue(DefaultChangeRequestConfiguration.DELEGATE_ENABLED_PROPERTY)).thenReturn(1);

        this.configurationUpdatedListener.onEvent(null, source, null);
        verifyNoInteractions(this.queryManagerProvider);
        verifyNoInteractions(this.jobExecutorProvider);
    }

    @Test
    void onEventConfigEnabled() throws Exception
    {
        XWikiDocument source = mock(XWikiDocument.class);
        when(this.configuration.isDelegateEnabled()).thenReturn(true);
        when(this.configuration.getDelegateClassPropertyList()).thenReturn(Collections.singletonList("delegate"));
        when(this.remoteObservationManagerContext.isRemoteState()).thenReturn(false);

        when(source.getDocumentReference()).thenReturn(
            new DocumentReference(ChangeRequestConfigurationSource.DOC_REFERENCE, new WikiReference("xwiki")));
        XWikiDocument originalDoc = mock(XWikiDocument.class);
        when(source.getOriginalDocument()).thenReturn(originalDoc);
        BaseObject currentObj = mock(BaseObject.class);

        when(source.getXObject(ChangeRequestConfigurationSource.CLASS_REFERENCE)).thenReturn(currentObj);
        BaseObject previousObj = mock(BaseObject.class);
        when(originalDoc.getXObject(ChangeRequestConfigurationSource.CLASS_REFERENCE)).thenReturn(previousObj);

        when(currentObj.getStringValue(DefaultChangeRequestConfiguration.DELEGATE_CLASS_PROPERTY_LIST_PROPERTY))
            .thenReturn("delegate");
        when(previousObj.getStringValue(DefaultChangeRequestConfiguration.DELEGATE_CLASS_PROPERTY_LIST_PROPERTY))
            .thenReturn("delegate");
        when(currentObj.getIntValue(DefaultChangeRequestConfiguration.DELEGATE_ENABLED_PROPERTY)).thenReturn(1);
        when(previousObj.getIntValue(DefaultChangeRequestConfiguration.DELEGATE_ENABLED_PROPERTY)).thenReturn(0);

        QueryManager queryManager = mock(QueryManager.class);
        when(this.queryManagerProvider.get()).thenReturn(queryManager);
        Query query = mock(Query.class);
        String statement = "select distinct doc.fullName from Document doc, "
            + "doc.object(XWiki.XWikiUsers) objUser where objUser.active = 1 order by doc.fullName";
        when(queryManager.createQuery(statement, Query.XWQL)).thenReturn(query);

        String foo = "XWiki.Foo";
        String bar = "XWiki.Bar";
        String buz = "XWiki.Buz";

        DocumentReference fooRef = mock(DocumentReference.class);
        DocumentReference barRef = mock(DocumentReference.class);
        DocumentReference buzRef = mock(DocumentReference.class);

        when(this.stringDocumentReferenceResolver.resolve(foo)).thenReturn(fooRef);
        when(this.stringDocumentReferenceResolver.resolve(bar)).thenReturn(barRef);
        when(this.stringDocumentReferenceResolver.resolve(buz)).thenReturn(buzRef);

        when(query.execute()).thenReturn(List.of(foo, bar, buz));

        JobExecutor jobExecutor = mock(JobExecutor.class);
        when(jobExecutorProvider.get()).thenReturn(jobExecutor);

        when(jobExecutor.execute(eq(DelegateApproversComputationRequest.DELEGATE_APPROVERS_COMPUTATION_JOB), any()))
            .then(invocation -> {
                DelegateApproversComputationRequest request = invocation.getArgument(1);
                assertEquals(List.of(fooRef, barRef, buzRef), request.getEntityReferences());
                assertFalse(request.isDeep());
                assertFalse(request.isInteractive());
                return null;
        });
        this.configurationUpdatedListener.onEvent(null, source, null);
        verify(query).setWiki("xwiki");
        verify(jobExecutor).execute(eq(DelegateApproversComputationRequest.DELEGATE_APPROVERS_COMPUTATION_JOB), any());
    }
}
