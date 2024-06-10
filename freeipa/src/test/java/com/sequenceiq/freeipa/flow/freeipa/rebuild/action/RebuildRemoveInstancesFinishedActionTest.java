package com.sequenceiq.freeipa.flow.freeipa.rebuild.action;

import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent.ADD_INSTANCE_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.flow.stack.termination.action.TerminationService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;

@ExtendWith(MockitoExtension.class)
class RebuildRemoveInstancesFinishedActionTest {

    private static final Long STACK_ID = 4L;

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private FlowRegister flowRegister;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @Mock
    private EventBus eventBus;

    @Mock
    private TerminationService terminationService;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @InjectMocks
    private RebuildRemoveInstancesFinishedAction underTest;

    @Test
    void doExecute() throws Exception {
        Stack stack = new Stack();
        InstanceGroup instanceGroup = new InstanceGroup();
        InstanceMetaData im = new InstanceMetaData();
        im.setInstanceMetadataType(InstanceMetadataType.GATEWAY);
        InstanceMetaData pgw = new InstanceMetaData();
        pgw.setInstanceMetadataType(InstanceMetadataType.GATEWAY_PRIMARY);
        pgw.setInstanceId("asdf");
        instanceGroup.setInstanceMetaData(Set.of(im, pgw));
        stack.setInstanceGroups(Set.of(instanceGroup));
        CloudContext cloudContext = mock(CloudContext.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        CloudStack cloudStack = mock(CloudStack.class);
        StackContext context = new StackContext(mock(FlowParameters.class), stack, cloudContext, cloudCredential, cloudStack);

        underTest.doExecute(context, new DownscaleStackResult(STACK_ID, List.of()), Map.of());

        verify(stackUpdater).updateStackStatus(stack, DetailedStackStatus.REBUILD_IN_PROGRESS, "Finished removing instances");
        verify(terminationService).terminateMetaDataInstances(stack, null);
        ArgumentCaptor<InstanceMetaData> captor = ArgumentCaptor.forClass(InstanceMetaData.class);
        verify(instanceMetaDataService, times(1)).save(captor.capture());
        InstanceMetaData instanceMetaData = captor.getValue();
        assertEquals(pgw.getInstanceId(), instanceMetaData.getInstanceId());
        assertEquals(InstanceMetadataType.GATEWAY, instanceMetaData.getInstanceMetadataType());
        ArgumentCaptor<Object> payloadCapture = ArgumentCaptor.forClass(Object.class);
        verify(reactorEventFactory).createEvent(anyMap(), payloadCapture.capture());
        StackEvent event = (StackEvent) payloadCapture.getValue();
        assertEquals(STACK_ID, event.getResourceId());
        assertEquals(ADD_INSTANCE_EVENT.event(), event.selector());
    }
}