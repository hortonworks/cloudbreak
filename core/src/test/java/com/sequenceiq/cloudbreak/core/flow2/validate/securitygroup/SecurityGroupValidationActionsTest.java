package com.sequenceiq.cloudbreak.core.flow2.validate.securitygroup;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.REPAIR_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.SECURITY_GROUP_VALIDATION_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.SECURITY_GROUP_VALIDATION_STARTED;
import static com.sequenceiq.cloudbreak.core.flow2.validate.securitygroup.config.SecurityGroupValidationEvent.SECURITY_GROUP_VALIDATION_FAIL_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.validate.securitygroup.config.SecurityGroupValidationEvent.SECURITY_GROUP_VALIDATION_FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.validate.securitygroup.config.SecurityGroupValidationEvent.SECURITY_GROUP_VALIDATION_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_SECURITY_GROUP_VALIDATION_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_SECURITY_GROUP_VALIDATION_FINISHED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.statemachine.action.Action;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.event.resource.validation.SecurityGroupValidationResult;
import com.sequenceiq.cloudbreak.core.flow2.event.SecurityGroupValidationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialClientService;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

@SuppressWarnings({"unchecked", "rawtypes"})
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SecurityGroupValidationActionsTest {

    private static final long STACK_ID = 42L;

    @InjectMocks
    private SecurityGroupValidationActions underTest;

    @Mock
    private AwsSecurityGroupValidationRequestProvider requestProvider;

    @Mock
    private CredentialClientService credentialClientService;

    @Mock
    private CloudbreakFlowMessageService flowMessageService;

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private EventBus eventBus;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @Mock
    private StackContext stackContext;

    @Mock
    private StackDto stackDto;

    @Mock
    private FlowParameters flowParameters;

    private void stubReactorEventFactory() {
        lenient().when(reactorEventFactory.createEvent(any(), any())).thenAnswer(invocation -> {
            Event event = mock(Event.class);
            when(event.getData()).thenReturn(invocation.getArgument(1));
            return event;
        });
    }

    private void stubStackContext() {
        when(stackContext.getStack()).thenReturn(stackDto);
        when(stackDto.getId()).thenReturn(STACK_ID);
    }

    private void stubStackContextWithFlowParameters() {
        stubStackContext();
        when(stackContext.getFlowParameters()).thenReturn(flowParameters);
    }

    @Test
    void validateSecurityGroupsShortCircuitsWhenNoIds() throws Exception {
        stubReactorEventFactory();
        stubStackContextWithFlowParameters();
        when(requestProvider.collectSecurityGroupIds(stackDto)).thenReturn(Set.of());

        Action<?, ?> action = initActionPrivateFields(underTest.validateSecurityGroups());
        SecurityGroupValidationTriggerEvent payload =
                new SecurityGroupValidationTriggerEvent("selector", STACK_ID);

        new AbstractActionTestSupport((AbstractAction) action).doExecute(stackContext, payload, new HashMap<>());

        verify(stackUpdater).updateStackStatus(STACK_ID, SECURITY_GROUP_VALIDATION_STARTED, "Validating security groups");
        ArgumentCaptor<String> selectorCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(selectorCaptor.capture(), eventCaptor.capture());
        assertThat(selectorCaptor.getValue()).isEqualTo(SECURITY_GROUP_VALIDATION_FINISHED_EVENT.event());
        SecurityGroupValidationResult result = (SecurityGroupValidationResult) eventCaptor.getValue().getData();
        assertThat(result.getMissingSecurityGroupIds()).isEmpty();
        assertThat(result.getNotInNetworkSecurityGroupIds()).isEmpty();
    }

    @Test
    void handleValidationResultSendsFailureEventOnMissingSecurityGroups() throws Exception {
        stubReactorEventFactory();
        stubStackContextWithFlowParameters();
        SecurityGroupValidationResult payload = new SecurityGroupValidationResult(STACK_ID, Set.of("sg-missing"), Set.of());
        Action<?, ?> action = initActionPrivateFields(underTest.handleValidationResult());

        new AbstractActionTestSupport((AbstractAction) action).doExecute(stackContext, payload, new HashMap<>());

        ArgumentCaptor<String> selectorCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(selectorCaptor.capture(), eventCaptor.capture());
        assertThat(selectorCaptor.getValue()).isEqualTo(SECURITY_GROUP_VALIDATION_FAIL_EVENT.event());
        StackFailureEvent failureEvent = (StackFailureEvent) eventCaptor.getValue().getData();
        assertThat(failureEvent.getException().getMessage()).contains("sg-missing");
        assertThat(failureEvent.getException().getMessage()).contains("do not exist");
    }

    @Test
    void handleValidationResultSendsFailureEventOnVpcMismatch() throws Exception {
        stubReactorEventFactory();
        stubStackContextWithFlowParameters();
        SecurityGroupValidationResult payload = new SecurityGroupValidationResult(STACK_ID, Set.of(), Set.of("sg-wrong-vpc"));
        Action<?, ?> action = initActionPrivateFields(underTest.handleValidationResult());

        new AbstractActionTestSupport((AbstractAction) action).doExecute(stackContext, payload, new HashMap<>());

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(eq(SECURITY_GROUP_VALIDATION_FAIL_EVENT.event()), eventCaptor.capture());
        StackFailureEvent failureEvent = (StackFailureEvent) eventCaptor.getValue().getData();
        assertThat(failureEvent.getException().getMessage()).contains("sg-wrong-vpc");
        assertThat(failureEvent.getException().getMessage()).contains("do not belong to the environment VPC");
    }

    @Test
    void handleValidationResultSendsFailureEventOnHandlerFailure() throws Exception {
        stubReactorEventFactory();
        when(stackContext.getFlowParameters()).thenReturn(flowParameters);
        SecurityGroupValidationResult payload = new SecurityGroupValidationResult("provider boom", new RuntimeException("boom"), STACK_ID);
        Action<?, ?> action = initActionPrivateFields(underTest.handleValidationResult());

        new AbstractActionTestSupport((AbstractAction) action).doExecute(stackContext, payload, new HashMap<>());

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(eq(SECURITY_GROUP_VALIDATION_FAIL_EVENT.event()), eventCaptor.capture());
        assertThat(((StackFailureEvent) eventCaptor.getValue().getData()).getException().getMessage()).isEqualTo("provider boom");
    }

    @Test
    void handleValidationResultUpdatesStatusAndFiresEventOnSuccess() throws Exception {
        stubReactorEventFactory();
        lenient().when(stackContext.getFlowParameters()).thenReturn(flowParameters);
        when(flowParameters.getFlowId()).thenReturn("flow-1");
        SecurityGroupValidationResult payload = new SecurityGroupValidationResult(STACK_ID, Set.of(), Set.of());
        Action<?, ?> action = initActionPrivateFields(underTest.handleValidationResult());

        new AbstractActionTestSupport((AbstractAction) action).doExecute(stackContext, payload, new HashMap<>());

        verify(stackUpdater).updateStackStatus(STACK_ID, REPAIR_IN_PROGRESS, "Security group validation finished");
        verify(flowMessageService).fireEventAndLog(STACK_ID, com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS.name(),
                STACK_SECURITY_GROUP_VALIDATION_FINISHED);
        ArgumentCaptor<String> selectorCaptor = ArgumentCaptor.forClass(String.class);
        verify(eventBus).notify(selectorCaptor.capture(), any(Event.class));
        assertThat(selectorCaptor.getValue()).isEqualTo(SECURITY_GROUP_VALIDATION_FINALIZED_EVENT.event());
    }

    @Test
    void validationFailedNotifiesUserAndUpdatesStackStatus() throws Exception {
        String errorReason = "validation failed";
        StackFailureEvent payload = new StackFailureEvent(STACK_ID, new Exception(errorReason));
        StackFailureContext failureContext = new StackFailureContext(flowParameters, mock(com.sequenceiq.cloudbreak.view.StackView.class), STACK_ID);
        Action<?, ?> action = initActionPrivateFields(underTest.validationFailed());

        new AbstractActionTestSupport((AbstractAction) action).doExecute(failureContext, payload, new HashMap<>());

        verify(stackUpdater).updateStackStatus(STACK_ID, SECURITY_GROUP_VALIDATION_FAILED, errorReason);
        verify(flowMessageService).fireEventAndLog(STACK_ID, com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED.name(),
                STACK_SECURITY_GROUP_VALIDATION_FAILED, errorReason);
    }

    private Action<?, ?> initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
        return action;
    }
}
