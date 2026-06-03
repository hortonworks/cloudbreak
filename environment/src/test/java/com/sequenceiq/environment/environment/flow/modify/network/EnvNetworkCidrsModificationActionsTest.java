package com.sequenceiq.environment.environment.flow.modify.network;

import static com.sequenceiq.environment.environment.flow.modify.network.event.EnvNetworkCidrsModificationHandlerSelectors.MODIFY_NETWORK_CIDRS_ON_DATALAKE_AND_DATAHUBS_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.network.event.EnvNetworkCidrsModificationHandlerSelectors.MODIFY_NETWORK_CIDRS_ON_FREEIPA_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.network.event.EnvNetworkCidrsModificationStateSelectors.FINALIZE_MODIFY_NETWORK_CIDRS_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.network.event.EnvNetworkCidrsModificationStateSelectors.FINISH_MODIFY_NETWORK_CIDRS_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.network.event.EnvNetworkCidrsModificationStateSelectors.HANDLED_FAILED_MODIFY_NETWORK_CIDRS_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.network.event.EnvNetworkCidrsModificationStateSelectors.START_MODIFY_ENVIRONMENT_NETWORK_CIDRS_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.network.event.EnvNetworkCidrsModificationStateSelectors.START_MODIFY_NETWORK_CIDRS_DATALAKE_AND_DATAHUBS_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.network.event.EnvNetworkCidrsModificationStateSelectors.START_MODIFY_NETWORK_CIDRS_FREEIPA_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.action.Action;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.flow.modify.network.event.EnvNetworkCidrsModificationEvent;
import com.sequenceiq.environment.environment.flow.modify.network.event.EnvNetworkCidrsModificationFailureEvent;
import com.sequenceiq.environment.environment.service.EnvironmentStatusUpdateService;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

@ExtendWith(MockitoExtension.class)
class EnvNetworkCidrsModificationActionsTest {
    private static final Long ENV_ID = 1L;

    private static final String ENV_CRN = "environmentCrn";

    private static final String ENV_NAME = "envName";

    private static final String FLOW_ID = "flowId";

    private static final List<String> NETWORK_CIDRS = List.of("10.84.128.0/17", "10.84.0.0/17");

    @Mock
    private EventBus eventBus;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @Mock
    private FlowParameters flowParameters;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private EnvironmentStatusUpdateService environmentStatusUpdateService;

    @Mock
    private EnvNetworkCidrsModificationEvent payload;

    @Mock
    private CommonContext context;

    @Captor
    private ArgumentCaptor<String> selectorCaptor;

    @InjectMocks
    private EnvNetworkCidrsModificationActions underTest;

    private Map<Object, Object> variables = new HashMap<>();

    @BeforeEach
    void setup() {
        when(context.getFlowParameters()).thenReturn(flowParameters);
        when(flowParameters.getFlowId()).thenReturn(FLOW_ID);
    }

    private static Stream<Arguments> modifyNetworkCidrsActionParams() {
        return Stream.of(
                Arguments.of(
                        "Init",
                        START_MODIFY_ENVIRONMENT_NETWORK_CIDRS_EVENT.selector(),
                        (Function<EnvNetworkCidrsModificationActions, Action<?, ?>>)
                                EnvNetworkCidrsModificationActions::initNetworkCidrsModificationOnEnvironment,
                        START_MODIFY_NETWORK_CIDRS_FREEIPA_EVENT.event(),
                        EnvironmentStatus.NETWORK_CIDRS_MODIFICATION_IN_PROGRESS,
                        ResourceEvent.ENVIRONMENT_NETWORK_CIDRS_MODIFICATION_STARTED,
                        EnvNetworkCidrsModificationState.ENVIRONMENT_NETWORK_CIDRS_MODIFICATION_START_STATE
                ),
                Arguments.of(
                        "FreeIPA stack",
                        START_MODIFY_NETWORK_CIDRS_FREEIPA_EVENT.selector(),
                        (Function<EnvNetworkCidrsModificationActions, Action<?, ?>>) EnvNetworkCidrsModificationActions::modifyNetworkCidrsOnFreeIpa,
                        MODIFY_NETWORK_CIDRS_ON_FREEIPA_EVENT.event(),
                        EnvironmentStatus.NETWORK_CIDRS_MODIFICATION_ON_FREEIPA_IN_PROGRESS,
                        ResourceEvent.ENVIRONMENT_NETWORK_CIDRS_MODIFICATION_ON_FREEIPA_STARTED,
                        EnvNetworkCidrsModificationState.NETWORK_CIDRS_MODIFICATION_FREEIPA_STATE
                ),
                Arguments.of(
                        "Datalake and Datahubs",
                        START_MODIFY_NETWORK_CIDRS_DATALAKE_AND_DATAHUBS_EVENT.selector(),
                        (Function<EnvNetworkCidrsModificationActions, Action<?, ?>>)
                                EnvNetworkCidrsModificationActions::modifyNetworkCidrsOnDatalakeAndDataHubs,
                        MODIFY_NETWORK_CIDRS_ON_DATALAKE_AND_DATAHUBS_EVENT.event(),
                        EnvironmentStatus.NETWORK_CIDRS_MODIFICATION_ON_DATALAKE_AND_DATAHUBS_IN_PROGRESS,
                        ResourceEvent.ENVIRONMENT_NETWORK_CIDRS_MODIFICATION_ON_DATALAKE_AND_DATAHUBS_STARTED,
                        EnvNetworkCidrsModificationState.NETWORK_CIDRS_MODIFICATION_DATALAKE_AND_DATAHUBS_STATE
                ),
                Arguments.of(
                        "Finished",
                        FINISH_MODIFY_NETWORK_CIDRS_EVENT.selector(),
                        (Function<EnvNetworkCidrsModificationActions, Action<?, ?>>) EnvNetworkCidrsModificationActions::modifyNetworkCidrsFinished,
                        FINALIZE_MODIFY_NETWORK_CIDRS_EVENT.event(),
                        EnvironmentStatus.AVAILABLE,
                        ResourceEvent.ENVIRONMENT_NETWORK_CIDRS_MODIFICATION_FINISHED,
                        EnvNetworkCidrsModificationState.NETWORK_CIDRS_MODIFICATION_FINISHED_STATE
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("modifyNetworkCidrsActionParams")
    @SuppressWarnings("unchecked")
    void modifyNetworkCidrs(
            String testName,
            String inputSelector,
            Function<EnvNetworkCidrsModificationActions, Action<?, ?>> actionSupplier,
            String expectedEventSelector,
            EnvironmentStatus expectedStatus,
            ResourceEvent expectedResourceEvent,
            EnvNetworkCidrsModificationState expectedState) throws Exception {

        EnvNetworkCidrsModificationEvent event = new EnvNetworkCidrsModificationEvent(inputSelector,
                ENV_ID, ENV_NAME, ENV_CRN, NETWORK_CIDRS, new Promise<>());
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), event)).when(reactorEventFactory).createEvent(any(), any());

        AbstractEnvNetworkCidrsModificationAction<EnvNetworkCidrsModificationEvent> action =
                (AbstractEnvNetworkCidrsModificationAction<EnvNetworkCidrsModificationEvent>) actionSupplier.apply(underTest);
        initActionPrivateFields(action);
        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(selectorCaptor.capture(), eventCaptor.capture());
        verify(environmentStatusUpdateService).updateEnvironmentStatusAndNotify(context, event,
                expectedStatus, expectedResourceEvent, expectedState);

        assertEquals(expectedEventSelector, selectorCaptor.getValue());
        assertEquals(ENV_ID, ReflectionTestUtils.getField(eventCaptor.getValue().getData(), "resourceId"));
        assertEquals(ENV_CRN, ReflectionTestUtils.getField(eventCaptor.getValue().getData(), "resourceCrn"));
        assertEquals(ENV_NAME, ReflectionTestUtils.getField(eventCaptor.getValue().getData(), "resourceName"));
        assertEquals(NETWORK_CIDRS, ReflectionTestUtils.getField(eventCaptor.getValue().getData(), "networkCidrs"));
    }

    @Test
    void modifyNetworkCidrsFailed() throws Exception {
        String errorMessage = "Error during FreeIPA user defined tags modification";
        RuntimeException error = new RuntimeException(errorMessage);
        EnvNetworkCidrsModificationFailureEvent event = new EnvNetworkCidrsModificationFailureEvent(ENV_ID, ENV_NAME, ENV_CRN,
                EnvironmentStatus.NETWORK_CIDRS_MODIFICATION_ON_FREEIPA_FAILED, error);
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), event)).when(reactorEventFactory).createEvent(any(), any());

        AbstractEnvNetworkCidrsModificationAction<EnvNetworkCidrsModificationFailureEvent> action =
                (AbstractEnvNetworkCidrsModificationAction<EnvNetworkCidrsModificationFailureEvent>) underTest.modifyNetworkCidrsFailed();
        initActionPrivateFields(action);
        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(selectorCaptor.capture(), eventCaptor.capture());
        environmentStatusUpdateService.updateEnvironmentStatusAndNotify(context, payload, EnvironmentStatus.AVAILABLE,
                ResourceEvent.ENVIRONMENT_NETWORK_CIDRS_MODIFICATION_FAILED, List.of("NETWORK_CIDRS_MODIFICATION_ON_FREEIPA_FAILED",
                        errorMessage), EnvNetworkCidrsModificationState.NETWORK_CIDRS_MODIFICATION_FAILED_STATE);

        assertEquals(HANDLED_FAILED_MODIFY_NETWORK_CIDRS_EVENT.event(), selectorCaptor.getValue());
        assertEquals(ENV_ID, ReflectionTestUtils.getField(eventCaptor.getValue().getData(), "resourceId"));
        assertEquals(ENV_CRN, ReflectionTestUtils.getField(eventCaptor.getValue().getData(), "resourceCrn"));
        assertEquals(ENV_NAME, ReflectionTestUtils.getField(eventCaptor.getValue().getData(), "resourceName"));
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
    }
}