package com.sequenceiq.freeipa.flow.freeipa.rebuild.action;

import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent.UPDATE_ENVIRONMENT_STACK_CONFIG_FINISHED_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.RebuildFailureEvent;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

@ExtendWith(MockitoExtension.class)
class RebuildUpdateEnvironmentStackConfigActionTest {

    private static final Long STACK_ID = 3L;

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private FlowRegister flowRegister;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @Mock
    private EventBus eventBus;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private EnvironmentEndpoint environmentEndpoint;

    @InjectMocks
    private RebuildUpdateEnvironmentStackConfigAction underTest;

    @Test
    void doExecute() throws Exception {
        Stack stack = new Stack();
        stack.setEnvironmentCrn("environmentCrn");
        CloudContext cloudContext = mock(CloudContext.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        CloudStack cloudStack = mock(CloudStack.class);
        StackContext context = new StackContext(mock(FlowParameters.class), stack, cloudContext, cloudCredential, cloudStack);
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(mock(RegionAwareInternalCrnGenerator.class));

        underTest.doExecute(context, new StackEvent(STACK_ID), new HashMap<>());

        verify(stackUpdater).updateStackStatus(stack, DetailedStackStatus.REBUILD_IN_PROGRESS, "Updating clusters' configuration");
        ArgumentCaptor<Object> payloadCapture = ArgumentCaptor.forClass(Object.class);
        verify(reactorEventFactory).createEvent(anyMap(), payloadCapture.capture());
        StackEvent event = (StackEvent) payloadCapture.getValue();
        assertEquals(STACK_ID, event.getResourceId());
        assertEquals(UPDATE_ENVIRONMENT_STACK_CONFIG_FINISHED_EVENT.event(), event.selector());
        verify(environmentEndpoint).updateConfigsInEnvironmentByCrn(stack.getEnvironmentCrn());
    }

    @Test
    void doExecuteFails() throws Exception {
        Stack stack = new Stack();
        stack.setEnvironmentCrn("environmentCrn");
        CloudContext cloudContext = mock(CloudContext.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        CloudStack cloudStack = mock(CloudStack.class);
        StackContext context = new StackContext(mock(FlowParameters.class), stack, cloudContext, cloudCredential, cloudStack);
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(mock(RegionAwareInternalCrnGenerator.class));
        doThrow(new RuntimeException("asf")).when(environmentEndpoint).updateConfigsInEnvironmentByCrn(stack.getEnvironmentCrn());

        underTest.doExecute(context, new StackEvent(STACK_ID), new HashMap<>());

        verify(stackUpdater).updateStackStatus(stack, DetailedStackStatus.REBUILD_IN_PROGRESS, "Updating clusters' configuration");
        ArgumentCaptor<Object> payloadCapture = ArgumentCaptor.forClass(Object.class);
        verify(reactorEventFactory).createEvent(anyMap(), payloadCapture.capture());
        RebuildFailureEvent event = (RebuildFailureEvent) payloadCapture.getValue();
        assertEquals(STACK_ID, event.getResourceId());
        assertEquals("Failed to update the clusters' config due to asf", event.getException().getMessage());
        verify(environmentEndpoint).updateConfigsInEnvironmentByCrn(stack.getEnvironmentCrn());
    }
}