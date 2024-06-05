package com.sequenceiq.freeipa.flow.freeipa.rebuild.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.RebootInstancesResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.HealthCheckFailed;
import com.sequenceiq.freeipa.flow.stack.HealthCheckRequest;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

@ExtendWith(MockitoExtension.class)
class RebuildRebootWaitUntilAvailableActionTest {

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @Mock
    private FlowRegister flowRegister;

    @Mock
    private EventBus eventBus;

    @Mock
    private StackUpdater stackUpdater;

    @InjectMocks
    private RebuildRebootWaitUntilAvailableAction underTest;

    @Test
    void doExecute() throws Exception {
        Stack stack = new Stack();
        stack.setId(3L);
        CloudContext cloudContext = mock(CloudContext.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        CloudStack cloudStack = mock(CloudStack.class);
        StackContext context = new StackContext(mock(FlowParameters.class), stack, cloudContext, cloudCredential, cloudStack);
        Map<Object, Object> variables = Map.of("INSTANCE_TO_RESTORE", "im",
                "FULL_BACKUP_LOCATION", "fbl",
                "DATA_BACKUP_LOCATION", "dbl");

        underTest.doExecute(context, new RebootInstancesResult(3L, null, List.of()), variables);

        verify(stackUpdater).updateStackStatus(stack, DetailedStackStatus.REBUILD_IN_PROGRESS, "Waiting for FreeIPA to be available");
        ArgumentCaptor<Object> payloadCapture = ArgumentCaptor.forClass(Object.class);
        verify(reactorEventFactory).createEvent(anyMap(), payloadCapture.capture());
        HealthCheckRequest payload = (HealthCheckRequest) payloadCapture.getValue();
        assertEquals(3L, payload.getResourceId());
        assertTrue(payload.isWaitForFreeIpaAvailability());
        assertTrue(payload.getInstanceIds().isEmpty());
    }

    @Test
    void createRequest() {
        Stack stack = new Stack();
        stack.setId(3L);
        InstanceGroup instanceGroup = new InstanceGroup();
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceId("im1");
        instanceGroup.setInstanceMetaData(Set.of(instanceMetaData));
        stack.setInstanceGroups(Set.of(instanceGroup));
        CloudContext cloudContext = mock(CloudContext.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        CloudStack cloudStack = mock(CloudStack.class);
        StackContext context = new StackContext(mock(FlowParameters.class), stack, cloudContext, cloudCredential, cloudStack);

        HealthCheckRequest result = (HealthCheckRequest) underTest.createRequest(context);

        assertEquals(3L, result.getResourceId());
        assertTrue(result.isWaitForFreeIpaAvailability());
        assertEquals(List.of("im1"), result.getInstanceIds());
    }

    @Test
    void getFailurePayload() {
        HealthCheckFailed result = (HealthCheckFailed) underTest.getFailurePayload(new RebootInstancesResult("sdf", new Exception("fds"), 3L, List.of("im1")),
                Optional.empty(), new Exception("asdf"));

        assertEquals(3L, result.getResourceId());
        assertEquals("asdf", result.getException().getMessage());
        assertEquals(List.of("im1"), result.getInstanceIds());
    }
}