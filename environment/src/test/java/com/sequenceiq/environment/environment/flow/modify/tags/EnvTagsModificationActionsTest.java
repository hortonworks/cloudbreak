package com.sequenceiq.environment.environment.flow.modify.tags;

import static com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationHandlerSelectors.MODIFY_USER_DEFINED_TAGS_ON_DATAHUBS_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationHandlerSelectors.MODIFY_USER_DEFINED_TAGS_ON_DATALAKE_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationHandlerSelectors.MODIFY_USER_DEFINED_TAGS_ON_FREEIPA_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationHandlerSelectors.MODIFY_USER_DEFINED_TAGS_ON_REDBEAMS_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationStateSelectors.FINALIZE_MODIFY_USER_DEFINED_TAGS_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationStateSelectors.FINISH_MODIFY_USER_DEFINED_TAGS_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationStateSelectors.HANDLED_FAILED_MODIFY_USER_DEFINED_TAGS_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationStateSelectors.START_MODIFY_ENVIRONMENT_TAGS_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationStateSelectors.START_MODIFY_USER_DEFINED_TAGS_DATAHUBS_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationStateSelectors.START_MODIFY_USER_DEFINED_TAGS_DATALAKE_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationStateSelectors.START_MODIFY_USER_DEFINED_TAGS_FREEIPA_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationStateSelectors.START_MODIFY_USER_DEFINED_TAGS_REDBEAMS_EVENT;
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
import com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationEvent;
import com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationFailureEvent;
import com.sequenceiq.environment.environment.service.EnvironmentStatusUpdateService;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

@ExtendWith(MockitoExtension.class)
class EnvTagsModificationActionsTest {

    private static final Long ENV_ID = 1L;

    private static final String ENV_CRN = "environmentCrn";

    private static final String ENV_NAME = "envName";

    private static final String FLOW_ID = "flowId";

    private static final Map<String, String> USER_DEFINED_TAGS = Map.of("custom", "value");

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
    private EnvTagsModificationEvent payload;

    @Mock
    private CommonContext context;

    @Captor
    private ArgumentCaptor<String> selectorCaptor;

    @InjectMocks
    private EnvTagsModificationActions underTest;

    private Map<Object, Object> variables = new HashMap<>();

    @BeforeEach
    void setup() {
        when(context.getFlowParameters()).thenReturn(flowParameters);
        when(flowParameters.getFlowId()).thenReturn(FLOW_ID);
    }

    private static Stream<Arguments> modifyUserDefinedTagsActionParams() {
        return Stream.of(
            Arguments.of(
                "Init",
                START_MODIFY_ENVIRONMENT_TAGS_EVENT.selector(),
                (Function<EnvTagsModificationActions, Action<?, ?>>) EnvTagsModificationActions::initUserDefinedTagsModificationOnEnvironment,
                START_MODIFY_USER_DEFINED_TAGS_FREEIPA_EVENT.event(),
                EnvironmentStatus.USER_DEFINED_TAGS_MODIFICATION_IN_PROGRESS,
                ResourceEvent.ENVIRONMENT_USER_DEFINED_TAGS_MODIFICATION_STARTED,
                EnvTagsModificationState.ENVIRONMENT_TAGS_MODIFICATION_START_STATE
            ),
            Arguments.of(
                "FreeIPA stack",
                START_MODIFY_USER_DEFINED_TAGS_FREEIPA_EVENT.selector(),
                (Function<EnvTagsModificationActions, Action<?, ?>>) EnvTagsModificationActions::modifyUserDefinedTagsOnFreeIpa,
                MODIFY_USER_DEFINED_TAGS_ON_FREEIPA_EVENT.event(),
                EnvironmentStatus.USER_DEFINED_TAGS_MODIFICATION_ON_FREEIPA_IN_PROGRESS,
                ResourceEvent.ENVIRONMENT_USER_DEFINED_TAGS_MODIFICATION_ON_FREEIPA_STARTED,
                EnvTagsModificationState.USER_DEFINED_TAGS_MODIFICATION_FREEIPA_STATE
            ),
            Arguments.of(
                "Datalake stack",
                START_MODIFY_USER_DEFINED_TAGS_DATALAKE_EVENT.selector(),
                (Function<EnvTagsModificationActions, Action<?, ?>>) EnvTagsModificationActions::modifyUserDefinedTagsOnDatalake,
                MODIFY_USER_DEFINED_TAGS_ON_DATALAKE_EVENT.event(),
                EnvironmentStatus.USER_DEFINED_TAGS_MODIFICATION_ON_DATALAKE_IN_PROGRESS,
                ResourceEvent.ENVIRONMENT_USER_DEFINED_TAGS_MODIFICATION_ON_DATALAKE_STARTED,
                EnvTagsModificationState.USER_DEFINED_TAGS_MODIFICATION_DATALAKE_STATE
            ),
            Arguments.of(
                "Datahub stacks",
                START_MODIFY_USER_DEFINED_TAGS_DATAHUBS_EVENT.selector(),
                (Function<EnvTagsModificationActions, Action<?, ?>>) EnvTagsModificationActions::modifyUserDefinedTagsOnDatahubs,
                MODIFY_USER_DEFINED_TAGS_ON_DATAHUBS_EVENT.event(),
                EnvironmentStatus.USER_DEFINED_TAGS_MODIFICATION_ON_DATAHUBS_IN_PROGRESS,
                ResourceEvent.ENVIRONMENT_USER_DEFINED_TAGS_MODIFICATION_ON_DATAHUBS_STARTED,
                EnvTagsModificationState.USER_DEFINED_TAGS_MODIFICATION_DATAHUBS_STATE
            ),
            Arguments.of(
                "Redbeams stack",
                START_MODIFY_USER_DEFINED_TAGS_REDBEAMS_EVENT.selector(),
                (Function<EnvTagsModificationActions, Action<?, ?>>) EnvTagsModificationActions::modifyUserDefinedTagsOnRedbeams,
                MODIFY_USER_DEFINED_TAGS_ON_REDBEAMS_EVENT.event(),
                EnvironmentStatus.USER_DEFINED_TAGS_MODIFICATION_ON_REDBEAMS_IN_PROGRESS,
                ResourceEvent.ENVIRONMENT_USER_DEFINED_TAGS_MODIFICATION_ON_REDBEAMS_STARTED,
                EnvTagsModificationState.USER_DEFINED_TAGS_MODIFICATION_REDBEAMS_STATE
            ),
            Arguments.of(
                "Finished",
                FINISH_MODIFY_USER_DEFINED_TAGS_EVENT.selector(),
                (Function<EnvTagsModificationActions, Action<?, ?>>) EnvTagsModificationActions::modifyUserDefinedTagsFinished,
                FINALIZE_MODIFY_USER_DEFINED_TAGS_EVENT.event(),
                EnvironmentStatus.AVAILABLE,
                ResourceEvent.ENVIRONMENT_USER_DEFINED_TAGS_MODIFICATION_FINISHED,
                EnvTagsModificationState.USER_DEFINED_TAGS_MODIFICATION_FINISHED_STATE
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("modifyUserDefinedTagsActionParams")
    @SuppressWarnings("unchecked")
    void modifyUserDefinedTags(
            String testName,
            String inputSelector,
            Function<EnvTagsModificationActions, Action<?, ?>> actionSupplier,
            String expectedEventSelector,
            EnvironmentStatus expectedStatus,
            ResourceEvent expectedResourceEvent,
            EnvTagsModificationState expectedState) throws Exception {

        EnvTagsModificationEvent event = new EnvTagsModificationEvent(inputSelector,
                ENV_ID, ENV_NAME, ENV_CRN, USER_DEFINED_TAGS, new Promise<>());
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), event)).when(reactorEventFactory).createEvent(any(), any());

        AbstractEnvTagsModificationAction<EnvTagsModificationEvent> action =
                (AbstractEnvTagsModificationAction<EnvTagsModificationEvent>) actionSupplier.apply(underTest);
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
        assertEquals(USER_DEFINED_TAGS, ReflectionTestUtils.getField(eventCaptor.getValue().getData(), "userDefinedTags"));
    }

    @Test
    void modifyUserDefinedTagsFailed() throws Exception {
        String errorMessage = "Error during FreeIPA user defined tags modification";
        RuntimeException error = new RuntimeException(errorMessage);
        EnvTagsModificationFailureEvent event = new EnvTagsModificationFailureEvent(ENV_ID, ENV_NAME, ENV_CRN,
                EnvironmentStatus.USER_DEFINED_TAGS_MODIFICATION_ON_FREEIPA_FAILED, error);
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), event)).when(reactorEventFactory).createEvent(any(), any());

        AbstractEnvTagsModificationAction<EnvTagsModificationFailureEvent> action =
                (AbstractEnvTagsModificationAction<EnvTagsModificationFailureEvent>) underTest.modifyUserDefinedTagsFailed();
        initActionPrivateFields(action);
        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(selectorCaptor.capture(), eventCaptor.capture());
        environmentStatusUpdateService.updateEnvironmentStatusAndNotify(context, payload, EnvironmentStatus.AVAILABLE,
                ResourceEvent.ENVIRONMENT_USER_DEFINED_TAGS_MODIFICATION_FAILED, List.of("USER_DEFINED_TAGS_MODIFICATION_ON_FREEIPA_FAILED",
                        errorMessage), EnvTagsModificationState.USER_DEFINED_TAGS_MODIFICATION_FAILED_STATE);

        assertEquals(HANDLED_FAILED_MODIFY_USER_DEFINED_TAGS_EVENT.event(), selectorCaptor.getValue());
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