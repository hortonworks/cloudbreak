package com.sequenceiq.freeipa.flow.freeipa.rebuild.action;

import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent.UPDATE_METADATA_FOR_DELETION_FINISHED_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.HashMap;

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
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.flow.stack.termination.action.TerminationService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

@ExtendWith(MockitoExtension.class)
class RebuildUpdateMetadataForDeletionActionTest {
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

    @InjectMocks
    private RebuildUpdateMetadataForDeletionAction underTest;

    @Test
    void doExecute() throws Exception {
        Stack stack = new Stack();
        CloudContext cloudContext = mock(CloudContext.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        CloudStack cloudStack = mock(CloudStack.class);
        StackContext context = new StackContext(mock(FlowParameters.class), stack, cloudContext, cloudCredential, cloudStack);

        underTest.doExecute(context, new StackEvent(STACK_ID), new HashMap<>());

        verify(stackUpdater).updateStackStatus(stack, DetailedStackStatus.REBUILD_IN_PROGRESS, "Updating metadata for deletion request");
        verify(terminationService).requestDeletion(STACK_ID, null);
        ArgumentCaptor<Object> payloadCapture = ArgumentCaptor.forClass(Object.class);
        verify(reactorEventFactory).createEvent(anyMap(), payloadCapture.capture());
        StackEvent event = (StackEvent) payloadCapture.getValue();
        assertEquals(STACK_ID, event.getResourceId());
        assertEquals(UPDATE_METADATA_FOR_DELETION_FINISHED_EVENT.event(), event.selector());
    }
}