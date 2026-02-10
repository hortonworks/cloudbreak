package com.sequenceiq.environment.environment.flow.encryptionprofile;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.action.Action;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileEvent;
import com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileFailedEvent;
import com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileStateSelectors;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.EnvironmentStatusUpdateService;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

@SuppressWarnings({"unchecked", "rawtypes"})
@ExtendWith(MockitoExtension.class)
class EnableEncryptionProfileActionsTest {

    @InjectMocks
    private EnableEncryptionProfileActions underTest;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private EventBus eventBus;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @Mock
    private FlowParameters flowParameters;

    @Mock
    private EnvironmentStatusUpdateService environmentStatusUpdateService;

    @Mock
    private EnvironmentService environmentService;

    private CommonContext context;

    @BeforeEach
    void setUp() {
        context = new CommonContext(flowParameters);
    }

    @Test
    public void testValidateEnableEncryptionProfileAction() throws Exception {
        AbstractEnableEncryptionProfileActions<EnableEncryptionProfileEvent> validateEnableEncryptionProfileAction =
                (AbstractEnableEncryptionProfileActions<EnableEncryptionProfileEvent>) underTest.validateEnableEncryptionProfileAction();
        EnableEncryptionProfileEvent enableEncryptionProfileEvent = EnableEncryptionProfileEvent.builder().build();

        initActionPrivateFields(validateEnableEncryptionProfileAction);

        Event event = mock(Event.class);
        when(reactorEventFactory.createEvent(anyMap(), any())).thenReturn(event);

        new AbstractActionTestSupport<>(validateEnableEncryptionProfileAction).doExecute(context, enableEncryptionProfileEvent, Map.of());

        verify(environmentStatusUpdateService, times(1)).updateEnvironmentStatusAndNotify(context,
                enableEncryptionProfileEvent,
                EnvironmentStatus.ENABLE_ENCRYPTION_PROFILE_IN_PROGRESS,
                ResourceEvent.ENABLE_ENCRYPTION_PROFILE_STARTED,
                EnabledEncryptionProfileState.VALIDATE_ENABLE_ENCRYPTION_PROFILE_STATE);
        verify(eventBus)
                .notify(eq(EnableEncryptionProfileStateSelectors.VALIDATE_ENABLE_ENCRYPTION_PROFILE_HANDLER_EVENT.selector()), eq(event));
    }

    @Test
    public void testFinishedAction() throws Exception {
        AbstractEnableEncryptionProfileActions<EnableEncryptionProfileEvent> finishedAction =
                (AbstractEnableEncryptionProfileActions<EnableEncryptionProfileEvent>) underTest.finishedAction();
        EnableEncryptionProfileEvent enableEncryptionProfileEvent = EnableEncryptionProfileEvent.builder().build();

        initActionPrivateFields(finishedAction);

        Event event = mock(Event.class);
        when(reactorEventFactory.createEvent(anyMap(), any())).thenReturn(event);

        new AbstractActionTestSupport<>(finishedAction).doExecute(context, enableEncryptionProfileEvent, Map.of());

        verify(environmentStatusUpdateService, times(1)).updateEnvironmentStatusAndNotify(context,
                enableEncryptionProfileEvent,
                EnvironmentStatus.AVAILABLE,
                ResourceEvent.ENABLE_ENCRYPTION_PROFILE_FINISHED,
                EnabledEncryptionProfileState.ENABLE_ENCRYPTION_PROFILE_FINISHED_STATE);
        verify(eventBus)
                .notify(eq(EnableEncryptionProfileStateSelectors.FINALIZE_ENABLE_ENCRYPTION_PROFILE_EVENT.selector()), eq(event));
    }

    @Test
    public void testFailedAction() throws Exception {
        AbstractEnableEncryptionProfileActions<EnableEncryptionProfileFailedEvent> failedAction =
                (AbstractEnableEncryptionProfileActions<EnableEncryptionProfileFailedEvent>) underTest.failedAction();
        EnableEncryptionProfileFailedEvent enableEncryptionProfileFailedEvent =
                new EnableEncryptionProfileFailedEvent(1L, "envName", "envCrn", new NotFoundException("404"));

        initActionPrivateFields(failedAction);

        Event event = mock(Event.class);
        when(reactorEventFactory.createEvent(anyMap(), any())).thenReturn(event);
        Environment environment = mock(Environment.class);
        Optional<Environment> environmentOp = Optional.of(environment);
        when(environmentService.findEnvironmentById(any())).thenReturn(environmentOp);
        when(environment.getStatus()).thenReturn(EnvironmentStatus.AVAILABLE);

        new AbstractActionTestSupport<>(failedAction).doExecute(context, enableEncryptionProfileFailedEvent, Map.of());

        verify(environmentStatusUpdateService, times(1)).updateFailedEnvironmentStatusAndNotify(context,
                enableEncryptionProfileFailedEvent,
                EnvironmentStatus.AVAILABLE,
                ResourceEvent.ENABLE_ENCRYPTION_PROFILE_FAILED,
                EnabledEncryptionProfileState.ENABLE_ENCRYPTION_PROFILE_FAILED_STATE);
        verify(eventBus)
                .notify(eq(EnableEncryptionProfileStateSelectors.HANDLED_FAILED_ENABLE_ENCRYPTION_PROFILE_EVENT.selector()), eq(event));
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
    }
}
