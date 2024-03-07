package com.sequenceiq.freeipa.flow.freeipa.rebuild.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.converter.cloud.StackToCloudStackConverter;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.upscale.action.PrivateIdProvider;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleStackRequest;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleStackResult;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.service.resource.ResourceService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;

@ExtendWith(MockitoExtension.class)
class RebuildAddInstanceActionTest {

    @Mock
    private StackToCloudStackConverter cloudStackConverter;

    @Mock
    private ResourceService resourceService;

    @Mock
    private PrivateIdProvider privateIdProvider;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @Mock
    private FlowRegister flowRegister;

    @Mock
    private EventBus eventBus;

    @Mock
    private StackUpdater stackUpdater;

    @InjectMocks
    private RebuildAddInstanceAction underTest;

    @Test
    void doExecute() throws Exception {
        Stack stack = new Stack();
        InstanceGroup instanceGroup = new InstanceGroup();
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setDiscoveryFQDN("ipa.test");
        instanceMetaData.setPrivateId(6L);
        instanceGroup.setInstanceMetaData(Set.of(instanceMetaData));
        stack.setInstanceGroups(Set.of(instanceGroup));
        CloudContext cloudContext = mock(CloudContext.class);
        when(cloudContext.getId()).thenReturn(4L);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        CloudStack cloudStack = mock(CloudStack.class);
        StackContext context = new StackContext(mock(FlowParameters.class), stack, cloudContext, cloudCredential, cloudStack);
        List<CloudResource> cloudResources = List.of(mock(CloudResource.class));
        when(resourceService.getAllCloudResource(stack)).thenReturn(cloudResources);
        ArgumentCaptor<InstanceMetaData> instanceMetaDataArgumentCaptor = ArgumentCaptor.forClass(InstanceMetaData.class);
        CloudInstance cloudInstance = mock(CloudInstance.class);
        when(cloudStackConverter.buildInstance(eq(stack), instanceMetaDataArgumentCaptor.capture(), any(InstanceGroup.class), any(),
                eq(instanceMetaData.getPrivateId()), eq(InstanceStatus.CREATE_REQUESTED))).thenReturn(cloudInstance);
        Stack updatedStack = new Stack();
        when(instanceMetaDataService.saveInstanceAndGetUpdatedStack(stack, List.of(cloudInstance), List.of())).thenReturn(updatedStack);
        CloudStack updatedCloudStack = mock(CloudStack.class);
        when(cloudStackConverter.convert(updatedStack)).thenReturn(updatedCloudStack);

        underTest.doExecute(context, new StackEvent(4L), Map.of("INSTANCE_TO_RESTORE", instanceMetaData.getDiscoveryFQDN()));

        ArgumentCaptor<Object> payloadCapture = ArgumentCaptor.forClass(Object.class);
        verify(reactorEventFactory).createEvent(anyMap(), payloadCapture.capture());
        UpscaleStackRequest<UpscaleStackResult> payload = (UpscaleStackRequest) payloadCapture.getValue();
        assertEquals(cloudContext, payload.getCloudContext());
        assertEquals(cloudCredential, payload.getCloudCredential());
        assertEquals(updatedCloudStack, payload.getCloudStack());
        assertEquals(cloudResources, payload.getResourceList());
        assertEquals(4L, payload.getResourceId());
        verify(stackUpdater).updateStackStatus(stack, DetailedStackStatus.REBUILD_IN_PROGRESS, "Create new instance");
    }
}