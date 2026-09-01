package com.sequenceiq.freeipa.flow.freeipa.migration.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.metadata.LoadBalancerMetadataCollectionRequest;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationInitFailedEvent;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationLbMetadataCollectionHandlerRequest;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationLbMetadataCollectionResult;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerMetadataCollectionService;

@ExtendWith(MockitoExtension.class)
class MultiAzMigrationLbMetadataCollectionHandlerTest {

    private static final long STACK_ID = 1L;

    private static final String OPERATION_ID = "op-1";

    @Mock
    private FreeIpaLoadBalancerMetadataCollectionService freeIpaLoadBalancerMetadataCollectionService;

    @InjectMocks
    private MultiAzMigrationLbMetadataCollectionHandler underTest;

    @Test
    void testSelector() {
        assertEquals(EventSelectorUtil.selector(MultiAzMigrationLbMetadataCollectionHandlerRequest.class), underTest.selector());
    }

    @Test
    void testDefaultFailureEvent() {
        Exception e = new Exception("test");

        Selectable result = underTest.defaultFailureEvent(STACK_ID, e,
                new Event<>(new MultiAzMigrationLbMetadataCollectionHandlerRequest(STACK_ID, OPERATION_ID, mock(), mock(), mock())));

        assertThat(result).isInstanceOf(MultiAzMigrationInitFailedEvent.class);
        MultiAzMigrationInitFailedEvent failure = (MultiAzMigrationInitFailedEvent) result;
        assertEquals(STACK_ID, failure.getResourceId());
        assertEquals(e, failure.getException());
    }

    @Test
    void testDoAcceptCollectsMetadata() {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setCloudPlatform("AWS");
        stack.setPlatformvariant("AWS");

        CloudContext cloudContext = mock(CloudContext.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        CloudStack cloudStack = mock(CloudStack.class);

        MultiAzMigrationLbMetadataCollectionHandlerRequest request = new MultiAzMigrationLbMetadataCollectionHandlerRequest(
                STACK_ID, OPERATION_ID, cloudContext, cloudCredential, cloudStack);

        Selectable result = underTest.doAccept(new HandlerEvent<>(new Event<>(request)));

        assertThat(result).isInstanceOf(MultiAzMigrationLbMetadataCollectionResult.class);
        MultiAzMigrationLbMetadataCollectionResult metadataResult = (MultiAzMigrationLbMetadataCollectionResult) result;
        assertEquals(STACK_ID, metadataResult.getResourceId());
        assertEquals(OPERATION_ID, metadataResult.getOperationId());

        ArgumentCaptor<LoadBalancerMetadataCollectionRequest> requestCaptor =
                ArgumentCaptor.forClass(LoadBalancerMetadataCollectionRequest.class);
        verify(freeIpaLoadBalancerMetadataCollectionService).collectLoadBalancerMetadata(requestCaptor.capture());
        LoadBalancerMetadataCollectionRequest captured = requestCaptor.getValue();
        assertEquals(STACK_ID, captured.getResourceId());
        assertEquals(cloudContext, captured.getCloudContext());
        assertEquals(cloudCredential, captured.getCloudCredential());
        assertEquals(cloudStack, captured.getCloudStack());
    }
}
