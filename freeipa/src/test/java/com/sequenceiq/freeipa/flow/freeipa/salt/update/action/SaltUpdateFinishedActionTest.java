package com.sequenceiq.freeipa.flow.freeipa.salt.update.action;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_SALT_UPDATE_FINISHED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SuccessDetails;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.events.EventSenderService;
import com.sequenceiq.freeipa.flow.OperationAwareAction;
import com.sequenceiq.freeipa.flow.chain.FlowChainAwareAction;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.services.InstallFreeIpaServicesSuccess;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

@ExtendWith(MockitoExtension.class)
class SaltUpdateFinishedActionTest {

    private static final String OPERATION_ID = "op-1";

    private static final String ACCOUNT_ID = "account-id";

    private static final String ENVIRONMENT_CRN = "environment-crn";

    @Mock
    private OperationService operationService;

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private EventSenderService eventSenderService;

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

    private SaltUpdateFinishedAction underTest;

    private StackContext context;

    @BeforeEach
    void setUp() {
        underTest = new SaltUpdateFinishedAction();
        ReflectionTestUtils.setField(underTest, "operationService", operationService);
        ReflectionTestUtils.setField(underTest, "stackUpdater", stackUpdater);
        ReflectionTestUtils.setField(underTest, null, eventSenderService, EventSenderService.class);
        ReflectionTestUtils.setField(underTest, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(underTest, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(underTest, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);

        context = new StackContext(new FlowParameters("flow", "user-crn"), stack, cloudContext, cloudCredential, cloudStack);
        doReturn(1L).when(stack).getId();
    }

    @Test
    void saltUpdateFinishedActionSendsFinishedNotification() throws Exception {
        InstallFreeIpaServicesSuccess payload = new InstallFreeIpaServicesSuccess(1L);
        Map<Object, Object> variables = new HashMap<>();
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), "ok")).when(reactorEventFactory).createEvent(any(), any());

        new AbstractActionTestSupport<>(underTest).doExecute(context, payload, variables);

        verify(eventSenderService).sendEventAndNotification(stack, "user-crn", FREEIPA_SALT_UPDATE_FINISHED);
    }

    static Stream<Arguments> statusAndOperationCompletionScenarios() {
        return Stream.of(
                // standalone (e.g. salt_update API or stack patcher), no operation id
                Arguments.of("standalone without operationId", false, false, null, DetailedStackStatus.AVAILABLE, false),
                Arguments.of("standalone with operationId", false, false, OPERATION_ID, DetailedStackStatus.AVAILABLE, true),
                Arguments.of("chained non-final with operationId", true, false, OPERATION_ID, DetailedStackStatus.SALT_STATE_UPDATE_IN_PROGRESS, false),
                Arguments.of("chained final with operationId", true, true, OPERATION_ID, DetailedStackStatus.AVAILABLE, true),
                Arguments.of("chained non-final without operationId", true, false, null, DetailedStackStatus.SALT_STATE_UPDATE_IN_PROGRESS, false)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("statusAndOperationCompletionScenarios")
    void saltUpdateFinishedActionUpdatesStatusAndCompletesOperationOnlyOnLastFlow(String scenario, boolean chained, boolean finalChain, String operationId,
            DetailedStackStatus expectedStatus, boolean expectOperationCompleted) throws Exception {
        InstallFreeIpaServicesSuccess payload = new InstallFreeIpaServicesSuccess(1L);
        Map<Object, Object> variables = new HashMap<>();
        if (chained) {
            variables.put(FlowChainAwareAction.CHAINED_ACTION, true);
        }
        if (finalChain) {
            variables.put(FlowChainAwareAction.FINAL_CHAIN, true);
        }
        if (operationId != null) {
            variables.put(OperationAwareAction.OPERATION_ID, operationId);
        }
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), "ok")).when(reactorEventFactory).createEvent(any(), any());
        if (expectOperationCompleted) {
            doReturn(ACCOUNT_ID).when(stack).getAccountId();
            doReturn(ENVIRONMENT_CRN).when(stack).getEnvironmentCrn();
        }

        new AbstractActionTestSupport<>(underTest).doExecute(context, payload, variables);

        verify(stackUpdater).updateStackStatus(stack, expectedStatus, "Salt update finished");
        if (expectOperationCompleted) {
            verify(operationService).completeOperation(ACCOUNT_ID, operationId, Set.of(new SuccessDetails(ENVIRONMENT_CRN)), Set.of());
        } else {
            verify(operationService, never()).completeOperation(anyString(), anyString(), anySet(), anySet());
        }
    }
}
