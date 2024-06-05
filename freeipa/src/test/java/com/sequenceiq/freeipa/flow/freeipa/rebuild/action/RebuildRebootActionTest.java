package com.sequenceiq.freeipa.flow.freeipa.rebuild.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.sequenceiq.cloudbreak.cloud.event.instance.RebootInstancesRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.converter.cloud.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.restore.FreeIpaRestoreSuccess;
import com.sequenceiq.freeipa.flow.instance.InstanceFailureEvent;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

@ExtendWith(MockitoExtension.class)
class RebuildRebootActionTest {

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @Mock
    private FlowRegister flowRegister;

    @Mock
    private EventBus eventBus;

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private InstanceMetaDataToCloudInstanceConverter instanceMetaDataToCloudInstanceConverter;

    @InjectMocks
    private RebuildRebootAction underTest;

    @Test
    void doExecute() throws Exception {
        Stack stack = new Stack();
        stack.setId(3L);
        CloudContext cloudContext = mock(CloudContext.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        CloudStack cloudStack = mock(CloudStack.class);
        when(cloudContext.getId()).thenReturn(3L);
        StackContext context = new StackContext(mock(FlowParameters.class), stack, cloudContext, cloudCredential, cloudStack);
        Map<Object, Object> variables = Map.of("INSTANCE_TO_RESTORE", "im",
                "FULL_BACKUP_LOCATION", "fbl",
                "DATA_BACKUP_LOCATION", "dbl");

        underTest.doExecute(context, new FreeIpaRestoreSuccess(3L), variables);

        verify(stackUpdater).updateStackStatus(stack, DetailedStackStatus.REBUILD_IN_PROGRESS, "Rebooting FreeIPA instance after restore");
        ArgumentCaptor<Object> payloadCapture = ArgumentCaptor.forClass(Object.class);
        verify(reactorEventFactory).createEvent(anyMap(), payloadCapture.capture());
        RebootInstancesRequest payload = (RebootInstancesRequest) payloadCapture.getValue();
        assertEquals(3L, payload.getResourceId());
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
        when(cloudContext.getId()).thenReturn(3L);
        StackContext context = new StackContext(mock(FlowParameters.class), stack, cloudContext, cloudCredential, cloudStack);
        CloudInstance cloudInstance = mock(CloudInstance.class);
        when(instanceMetaDataToCloudInstanceConverter.convert(instanceMetaData)).thenReturn(cloudInstance);

        RebootInstancesRequest request = (RebootInstancesRequest) underTest.createRequest(context);

        assertEquals(List.of(cloudInstance), request.getCloudInstances());
        assertTrue(request.getCloudResources().isEmpty());
        assertEquals(cloudContext, request.getCloudContext());
        assertEquals(cloudCredential, request.getCloudCredential());
        assertEquals(3L, request.getResourceId());
    }

    @Test
    void getFailurePayload() {
        InstanceFailureEvent result = (InstanceFailureEvent) underTest.getFailurePayload(new FreeIpaRestoreSuccess(3L), Optional.empty(), new Exception("asd"));

        assertEquals(3L, result.getResourceId());
        assertEquals("asd", result.getException().getMessage());
        assertTrue(result.getInstanceIds().isEmpty());
    }
}