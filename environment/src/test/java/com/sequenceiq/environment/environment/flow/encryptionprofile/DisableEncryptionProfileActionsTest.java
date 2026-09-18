package com.sequenceiq.environment.environment.flow.encryptionprofile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
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
import com.sequenceiq.cloudbreak.eventbus.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.flow.encryptionprofile.event.DisableEncryptionProfileEvent;
import com.sequenceiq.environment.environment.flow.encryptionprofile.event.DisableEncryptionProfileFailedEvent;
import com.sequenceiq.environment.environment.flow.encryptionprofile.event.DisableEncryptionProfileStateSelectors;
import com.sequenceiq.environment.environment.service.EnvironmentStatusUpdateService;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;

@SuppressWarnings({"unchecked", "rawtypes"})
@ExtendWith(MockitoExtension.class)
class DisableEncryptionProfileActionsTest {

    @InjectMocks
    private DisableEncryptionProfileActions underTest;

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

    private CommonContext context;

    @BeforeEach
    void setUp() {
        context = new CommonContext(flowParameters);
    }

    @Test
    public void testUnsetEncryptionProfileAction() throws Exception {
        AbstractDisableEncryptionProfileActions<DisableEncryptionProfileEvent> unsetEncryptionProfileAction =
                (AbstractDisableEncryptionProfileActions<DisableEncryptionProfileEvent>) underTest.unsetEncryptionProfileAction();
        DisableEncryptionProfileEvent disableEncryptionProfileEvent = DisableEncryptionProfileEvent.builder().build();

        initActionPrivateFields(unsetEncryptionProfileAction);

        Event event = mock(Event.class);
        when(reactorEventFactory.createEvent(anyMap(), any())).thenReturn(event);

        new AbstractActionTestSupport<>(unsetEncryptionProfileAction).doExecute(context, disableEncryptionProfileEvent, Map.of());

        verify(environmentStatusUpdateService, times(1)).updateEnvironmentStatusAndNotify(context,
                disableEncryptionProfileEvent,
                EnvironmentStatus.DISABLE_ENCRYPTION_PROFILE_IN_PROGRESS,
                ResourceEvent.DISABLE_ENCRYPTION_PROFILE_STARTED,
                DisableEncryptionProfileState.UNSET_ENCRYPTION_PROFILE_STATE);
        verify(eventBus)
                .notify(eq(DisableEncryptionProfileStateSelectors.UNSET_ENCRYPTION_PROFILE_HANDLER_EVENT.selector()), eq(event));
    }

    @Test
    public void testDisableEncryptionProfileOnFreeIpaAction() throws Exception {
        AbstractDisableEncryptionProfileActions<DisableEncryptionProfileEvent> disableEncryptionProfileOnFreeIpaAction =
                (AbstractDisableEncryptionProfileActions<DisableEncryptionProfileEvent>) underTest.disableEncryptionProfileOnFreeIpaAction();
        DisableEncryptionProfileEvent disableEncryptionProfileEvent = DisableEncryptionProfileEvent.builder().build();

        initActionPrivateFields(disableEncryptionProfileOnFreeIpaAction);

        Event event = mock(Event.class);
        when(reactorEventFactory.createEvent(anyMap(), any())).thenReturn(event);

        new AbstractActionTestSupport<>(disableEncryptionProfileOnFreeIpaAction).doExecute(context, disableEncryptionProfileEvent, Map.of());

        verify(eventBus)
                .notify(eq(DisableEncryptionProfileStateSelectors.DISABLE_ENCRYPTION_PROFILE_ON_FREEIPA_HANDLER_EVENT.selector()), eq(event));
    }

    @Test
    public void testDisableEncryptionProfileOnStacksAction() throws Exception {
        AbstractDisableEncryptionProfileActions<DisableEncryptionProfileEvent> disableEncryptionProfileOnStacksAction =
                (AbstractDisableEncryptionProfileActions<DisableEncryptionProfileEvent>) underTest.disableEncryptionProfileOnStacksAction();
        DisableEncryptionProfileEvent disableEncryptionProfileEvent = DisableEncryptionProfileEvent.builder().build();

        initActionPrivateFields(disableEncryptionProfileOnStacksAction);

        Event event = mock(Event.class);
        when(reactorEventFactory.createEvent(anyMap(), any())).thenReturn(event);

        new AbstractActionTestSupport<>(disableEncryptionProfileOnStacksAction).doExecute(context, disableEncryptionProfileEvent, Map.of());

        verify(eventBus)
                .notify(eq(DisableEncryptionProfileStateSelectors.DISABLE_ENCRYPTION_PROFILE_ON_STACKS_HANDLER_EVENT.selector()), eq(event));
    }

    @Test
    public void testFinishedAction() throws Exception {
        AbstractDisableEncryptionProfileActions<DisableEncryptionProfileEvent> finishedAction =
                (AbstractDisableEncryptionProfileActions<DisableEncryptionProfileEvent>) underTest.finishedAction();
        DisableEncryptionProfileEvent disableEncryptionProfileEvent = DisableEncryptionProfileEvent.builder().build();

        initActionPrivateFields(finishedAction);

        Event event = mock(Event.class);
        when(reactorEventFactory.createEvent(anyMap(), any())).thenReturn(event);

        new AbstractActionTestSupport<>(finishedAction).doExecute(context, disableEncryptionProfileEvent, Map.of());

        verify(environmentStatusUpdateService, times(1)).updateEnvironmentStatusAndNotify(context,
                disableEncryptionProfileEvent,
                EnvironmentStatus.AVAILABLE,
                ResourceEvent.DISABLE_ENCRYPTION_PROFILE_FINISHED,
                DisableEncryptionProfileState.DISABLE_ENCRYPTION_PROFILE_FINISHED_STATE);
        verify(eventBus)
                .notify(eq(DisableEncryptionProfileStateSelectors.FINALIZE_DISABLE_ENCRYPTION_PROFILE_EVENT.selector()), eq(event));
    }

    @Test
    public void testFinishedActionWithSkippedClusters() throws Exception {
        AbstractDisableEncryptionProfileActions<DisableEncryptionProfileEvent> finishedAction =
                (AbstractDisableEncryptionProfileActions<DisableEncryptionProfileEvent>) underTest.finishedAction();
        DisableEncryptionProfileEvent disableEncryptionProfileEvent = DisableEncryptionProfileEvent.builder()
                .withSkippedClusters(List.of("dl", "dh1"))
                .build();

        initActionPrivateFields(finishedAction);

        Event event = mock(Event.class);
        when(reactorEventFactory.createEvent(anyMap(), any())).thenReturn(event);

        new AbstractActionTestSupport<>(finishedAction).doExecute(context, disableEncryptionProfileEvent, Map.of());

        verify(environmentStatusUpdateService, times(1)).updateEnvironmentStatusAndNotify(context,
                disableEncryptionProfileEvent,
                EnvironmentStatus.AVAILABLE,
                ResourceEvent.DISABLE_ENCRYPTION_PROFILE_FINISHED_WITH_SKIPPED_CLUSTERS,
                List.of("dl, dh1"),
                DisableEncryptionProfileState.DISABLE_ENCRYPTION_PROFILE_FINISHED_STATE);
        verify(eventBus)
                .notify(eq(DisableEncryptionProfileStateSelectors.FINALIZE_DISABLE_ENCRYPTION_PROFILE_EVENT.selector()), eq(event));
    }

    @Test
    public void testFailurePayloadIsFailedEvent() {
        AbstractDisableEncryptionProfileActions<DisableEncryptionProfileEvent> unsetEncryptionProfileAction =
                (AbstractDisableEncryptionProfileActions<DisableEncryptionProfileEvent>) underTest.unsetEncryptionProfileAction();
        DisableEncryptionProfileEvent disableEncryptionProfileEvent = DisableEncryptionProfileEvent.builder()
                .withResourceId(1L)
                .withResourceName("envName")
                .withResourceCrn("envCrn")
                .build();
        RuntimeException exception = new RuntimeException("boom");

        Object failurePayload = new AbstractActionTestSupport<>(unsetEncryptionProfileAction)
                .getFailurePayload(disableEncryptionProfileEvent, Optional.empty(), exception);

        assertThat(failurePayload).isInstanceOf(DisableEncryptionProfileFailedEvent.class);
        DisableEncryptionProfileFailedEvent failedEvent = (DisableEncryptionProfileFailedEvent) failurePayload;
        assertThat(failedEvent.getResourceId()).isEqualTo(1L);
        assertThat(failedEvent.getResourceName()).isEqualTo("envName");
        assertThat(failedEvent.getResourceCrn()).isEqualTo("envCrn");
        assertThat(failedEvent.getException()).isSameAs(exception);
    }

    @Test
    public void testFailedAction() throws Exception {
        AbstractDisableEncryptionProfileActions<DisableEncryptionProfileFailedEvent> failedAction =
                (AbstractDisableEncryptionProfileActions<DisableEncryptionProfileFailedEvent>) underTest.failedAction();
        DisableEncryptionProfileFailedEvent disableEncryptionProfileFailedEvent =
                new DisableEncryptionProfileFailedEvent(1L, "envName", "envCrn", new NotFoundException("404"));

        initActionPrivateFields(failedAction);

        Event event = mock(Event.class);
        when(reactorEventFactory.createEvent(anyMap(), any())).thenReturn(event);

        new AbstractActionTestSupport<>(failedAction).doExecute(context, disableEncryptionProfileFailedEvent, Map.of());

        verify(environmentStatusUpdateService, times(1)).updateFailedEnvironmentStatusAndNotify(context,
                disableEncryptionProfileFailedEvent,
                EnvironmentStatus.AVAILABLE,
                ResourceEvent.DISABLE_ENCRYPTION_PROFILE_FAILED,
                List.of("404"),
                DisableEncryptionProfileState.DISABLE_ENCRYPTION_PROFILE_FAILED_STATE);
        verify(eventBus)
                .notify(eq(DisableEncryptionProfileStateSelectors.HANDLED_FAILED_DISABLE_ENCRYPTION_PROFILE_EVENT.selector()), eq(event));
    }

    @Test
    public void testFailedActionWithoutExceptionMessage() throws Exception {
        AbstractDisableEncryptionProfileActions<DisableEncryptionProfileFailedEvent> failedAction =
                (AbstractDisableEncryptionProfileActions<DisableEncryptionProfileFailedEvent>) underTest.failedAction();
        DisableEncryptionProfileFailedEvent disableEncryptionProfileFailedEvent =
                new DisableEncryptionProfileFailedEvent(1L, "envName", "envCrn", new RuntimeException());

        initActionPrivateFields(failedAction);

        Event event = mock(Event.class);
        when(reactorEventFactory.createEvent(anyMap(), any())).thenReturn(event);

        new AbstractActionTestSupport<>(failedAction).doExecute(context, disableEncryptionProfileFailedEvent, Map.of());

        verify(environmentStatusUpdateService, times(1)).updateFailedEnvironmentStatusAndNotify(context,
                disableEncryptionProfileFailedEvent,
                EnvironmentStatus.AVAILABLE,
                ResourceEvent.DISABLE_ENCRYPTION_PROFILE_FAILED,
                List.of("Unknown error"),
                DisableEncryptionProfileState.DISABLE_ENCRYPTION_PROFILE_FAILED_STATE);
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
    }
}
