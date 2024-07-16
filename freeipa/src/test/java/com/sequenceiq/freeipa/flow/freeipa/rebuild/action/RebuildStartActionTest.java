package com.sequenceiq.freeipa.flow.freeipa.rebuild.action;

import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent.REBUILD_STARTED_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.RebuildEvent;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

@ExtendWith(MockitoExtension.class)
class RebuildStartActionTest {

    private static final Long STACK_ID = 4L;

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private FlowRegister flowRegister;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @Mock
    private EventBus eventBus;

    @InjectMocks
    private RebuildStartAction underTest;

    @Test
    void prepareExecution() {
        RebuildEvent payload = new RebuildEvent(STACK_ID, "FQDN", "fback", "dback", "opid");
        Map<Object, Object> variables = new HashMap<>();

        underTest.prepareExecution(payload, variables);

        assertEquals(payload.getInstanceToRestoreFqdn(), underTest.getInstanceToRestoreFqdn(variables));
        assertEquals(payload.getFullBackupStorageLocation(), underTest.getFullBackupStorageLocation(variables));
        assertEquals(payload.getDataBackupStorageLocation(), underTest.getDataBackupStorageLocation(variables));
        assertEquals(payload.getOperationId(), underTest.getOperationId(variables));
    }

    @Test
    void doExecute() throws Exception {
        RebuildEvent payload = new RebuildEvent(STACK_ID, "FQDN", "fback", "dback", "opid");
        Stack stack = new Stack();
        CloudContext cloudContext = mock(CloudContext.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        CloudStack cloudStack = mock(CloudStack.class);
        StackContext context = new StackContext(mock(FlowParameters.class), stack, cloudContext, cloudCredential, cloudStack);

        underTest.doExecute(context, payload, new HashMap<>());

        verify(stackUpdater).updateStackStatus(stack, DetailedStackStatus.REBUILD_IN_PROGRESS, "FreeIPA rebuild requested");
        ArgumentCaptor<Object> payloadCapture = ArgumentCaptor.forClass(Object.class);
        verify(reactorEventFactory).createEvent(anyMap(), payloadCapture.capture());
        StackEvent event = (StackEvent) payloadCapture.getValue();
        assertEquals(STACK_ID, event.getResourceId());
        assertEquals(REBUILD_STARTED_EVENT.event(), event.selector());
    }
}