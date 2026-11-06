package com.sequenceiq.freeipa.flow.freeipa.salt.update.action;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.eventbus.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.util.SaltUpdateSkipHighstateFlagUtil;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.orchestrator.OrchestratorConfigSuccess;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.services.InstallFreeIpaServicesRequest;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.services.InstallFreeIpaServicesSuccess;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.provision.action.AbstractStackProvisionAction;

@ExtendWith(MockitoExtension.class)
class SaltUpdateActionsTest {

    private static final Long STACK_ID = 1L;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private EventBus eventBus;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @Mock
    private Stack stack;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private CloudStack cloudStack;

    private AbstractStackProvisionAction<OrchestratorConfigSuccess> runHighstateAction;

    private StackContext context;

    @BeforeEach
    void setUp() {
        runHighstateAction = (AbstractStackProvisionAction<OrchestratorConfigSuccess>) new SaltUpdateActions().runHighstateAction();
        ReflectionTestUtils.setField(runHighstateAction, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(runHighstateAction, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(runHighstateAction, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);

        context = new StackContext(new FlowParameters("flow", "user-crn"), stack, cloudContext, cloudCredential, cloudStack);
        doReturn(STACK_ID).when(stack).getId();
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), "ok")).when(reactorEventFactory).createEvent(any(), any());
    }

    @Test
    void runHighstateSkipsPreServiceDeploymentRecipes() throws Exception {
        Object sentPayload = runHighstate(false);

        InstallFreeIpaServicesRequest request = assertInstanceOf(InstallFreeIpaServicesRequest.class, sentPayload);
        assertTrue(request.isSkipPreServiceDeploymentRecipes());
    }

    @Test
    void runHighstateIsSkippedWhenRequested() throws Exception {
        Object sentPayload = runHighstate(true);

        assertInstanceOf(InstallFreeIpaServicesSuccess.class, sentPayload);
    }

    private Object runHighstate(boolean skipHighstate) throws Exception {
        Map<Object, Object> variables = new HashMap<>();
        SaltUpdateSkipHighstateFlagUtil.putToVariables(skipHighstate, variables);

        new AbstractActionTestSupport<>(runHighstateAction).doExecute(context, new OrchestratorConfigSuccess(STACK_ID), variables);

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(reactorEventFactory).createEvent(any(), payloadCaptor.capture());
        return payloadCaptor.getValue();
    }
}
