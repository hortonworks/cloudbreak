package com.sequenceiq.freeipa.flow.freeipa.rebuild.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackCollectResourcesRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.converter.cloud.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.service.resource.ResourceService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

@ExtendWith(MockitoExtension.class)
class RebuildCollectResourcesActionTest {
    @Mock
    private ResourceService resourceService;

    @Mock
    private InstanceMetaDataToCloudInstanceConverter instanceConverter;

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private FlowRegister flowRegister;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @Mock
    private EventBus eventBus;

    @InjectMocks
    private RebuildCollectResourcesAction underTest;

    @Test
    void doExecute() throws Exception {
        Stack stack = new Stack();
        stack.setId(4L);
        CloudContext cloudContext = mock(CloudContext.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        CloudStack cloudStack = mock(CloudStack.class);
        StackContext context = new StackContext(mock(FlowParameters.class), stack, cloudContext, cloudCredential, cloudStack);
        List<CloudResource> cloudResources = List.of(mock(CloudResource.class));
        when(resourceService.getAllCloudResource(stack.getId())).thenReturn(cloudResources);
        InstanceGroup instanceGroup = new InstanceGroup();
        stack.setInstanceGroups(Set.of(instanceGroup));
        InstanceMetaData im1 = new InstanceMetaData();
        im1.setInstanceStatus(InstanceStatus.REQUESTED);
        InstanceMetaData im2 = new InstanceMetaData();
        im2.setInstanceStatus(InstanceStatus.CREATED);
        InstanceMetaData im3 = new InstanceMetaData();
        im3.setInstanceStatus(InstanceStatus.TERMINATED);
        instanceGroup.setInstanceMetaData(Set.of(im1, im2, im3));
        when(instanceConverter.convert(any(InstanceMetaData.class))).thenReturn(mock(CloudInstance.class));

        underTest.doExecute(context, new StackEvent(4L), new HashMap<>());

        verify(stackUpdater).updateStackStatus(stack, DetailedStackStatus.REBUILD_IN_PROGRESS, "Collecting resources");
        ArgumentCaptor<Object> payloadCapture = ArgumentCaptor.forClass(Object.class);
        verify(reactorEventFactory).createEvent(anyMap(), payloadCapture.capture());
        DownscaleStackCollectResourcesRequest payload = (DownscaleStackCollectResourcesRequest) payloadCapture.getValue();
        assertEquals(cloudContext, payload.getCloudContext());
        assertEquals(cloudCredential, payload.getCloudCredential());
        assertEquals(cloudStack, payload.getCloudStack());
        assertEquals(cloudResources, payload.getCloudResources());
        assertEquals(2, payload.getInstances().size());
    }
}