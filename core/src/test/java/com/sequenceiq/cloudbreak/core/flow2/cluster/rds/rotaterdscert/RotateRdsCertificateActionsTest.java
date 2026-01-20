package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.rotaterdscert;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.RotateRdsCertificateType.ROTATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.cert.RotateRdsCertificateEvent.FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.cert.RotateRdsCertificateEvent.FINALIZED_EVENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.ExtendedState;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.state.State;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.common.metrics.MetricService;
import com.sequenceiq.cloudbreak.core.flow2.cluster.rds.cert.RotateRdsCertificateActions;
import com.sequenceiq.cloudbreak.core.flow2.cluster.rds.cert.migrate.MigrateRdsCertificateService;
import com.sequenceiq.cloudbreak.core.flow2.cluster.rds.cert.rotate.RotateRdsCertificateService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.GetLatestRdsCertificateRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.GetLatestRdsCertificateResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.RestartCmRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.RestartCmResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.RollingRestartServicesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.RollingRestartServicesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.RotateRdsCertificateCheckPrerequisitesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.RotateRdsCertificateFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.RotateRdsCertificateOnProviderRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.RotateRdsCertificateOnProviderResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.RotateRdsCertificateTriggerRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.UpdateLatestRdsCertificateRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.UpdateLatestRdsCertificateResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.UpdateTlsRdsResult;
import com.sequenceiq.cloudbreak.service.metrics.CloudbreakMetricService;
import com.sequenceiq.cloudbreak.service.metrics.MetricType;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.core.MessageFactory;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

@SuppressWarnings({"unchecked", "rawtypes"})
@ExtendWith(MockitoExtension.class)
class RotateRdsCertificateActionsTest {

    private static final String FLOW_ID = "flowId";

    private static final String FLOW_TRIGGER_USER_CRN = "flowTriggerUserCrn";

    private static final String ACTION_PAYLOAD_SELECTOR = "selector";

    private static final Long STACK_ID = 1234L;

    private static final String MESSAGE = "Houston, we have a problem.";

    private static final String STACK_NAME = "stackName";

    private static final String STACK_CRN = "stackCrn";

    private static final Stack STACK = new Stack();

    @Mock
    private RotateRdsCertificateService rotateRdsCertificateService;

    @Mock
    private StateContext stateContext;

    @Mock
    private ExtendedState extendedState;

    @Mock
    private StateMachine stateMachine;

    @Mock
    private State state;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private FlowEvent failureEvent;

    @Mock
    private EventBus eventBus;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @Mock
    private Event<Object> event;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private CloudbreakMetricService metricService;

    @Captor
    private ArgumentCaptor<String> selectorArgumentCaptor;

    @Captor
    private ArgumentCaptor<Event<?>> eventArgumentCaptor;

    @Captor
    private ArgumentCaptor<Map<String, Object>> headersArgumentCaptor;

    @Captor
    private ArgumentCaptor<Object> payloadArgumentCaptor;

    @Captor
    private ArgumentCaptor<Exception> exceptionCaptor;

    @Mock
    private Flow flow;

    @Mock
    private FlowEvent flowEvent;

    @Mock
    private MigrateRdsCertificateService migrateRdsCertificateService;

    @InjectMocks
    private RotateRdsCertificateActions underTest;

    @BeforeEach
    void setup() {
        STACK.setId(STACK_ID);
        FlowParameters flowParameters = new FlowParameters(FLOW_ID, FLOW_TRIGGER_USER_CRN);
        when(stateContext.getMessageHeader(MessageFactory.HEADERS.FLOW_PARAMETERS.name())).thenReturn(flowParameters);
        when(stateContext.getExtendedState()).thenReturn(extendedState);
        when(extendedState.getVariables()).thenReturn(new HashMap<>());
        when(stateContext.getStateMachine()).thenReturn(stateMachine);
        when(stateMachine.getState()).thenReturn(state);
        when(reactorEventFactory.createEvent(anyMap(), isNotNull())).thenReturn(event);
        when(stackDtoService.getStackViewById(any())).thenReturn(STACK);
        Workspace workspace = new Workspace();
        workspace.setTenant(new Tenant());
        STACK.setWorkspace(workspace);
    }

    @Test
    void checkPrerequisitesAction() {
        RotateRdsCertificateTriggerRequest request = new RotateRdsCertificateTriggerRequest(ACTION_PAYLOAD_SELECTOR, STACK_ID, ROTATE);
        when(stateContext.getMessageHeader(MessageFactory.HEADERS.DATA.name())).thenReturn(request);
        Action<?, ?> action = configureAction(underTest::checkPrerequisites);
        action.execute(stateContext);
        verify(rotateRdsCertificateService).checkPrerequisitesState(STACK_ID);
        verify(eventBus).notify(selectorArgumentCaptor.capture(), eventArgumentCaptor.capture());
        assertThat(selectorArgumentCaptor.getValue()).isEqualTo(EventSelectorUtil.selector(RotateRdsCertificateCheckPrerequisitesRequest.class));
    }

    @Test
    void getLatestRdsCertificateAction() {
        UpdateTlsRdsResult payload = new UpdateTlsRdsResult(STACK_ID, ROTATE);
        when(stateContext.getMessageHeader(MessageFactory.HEADERS.DATA.name())).thenReturn(payload);
        Action<?, ?> action = configureAction(underTest::getLatestRdsCertificate);
        action.execute(stateContext);
        verify(rotateRdsCertificateService).getLatestRdsCertificateState(STACK_ID);
        verify(eventBus).notify(selectorArgumentCaptor.capture(), eventArgumentCaptor.capture());
        assertThat(selectorArgumentCaptor.getValue()).isEqualTo(EventSelectorUtil.selector(GetLatestRdsCertificateRequest.class));
    }

    @Test
    void updateLatestRdsCertificateAction() {
        GetLatestRdsCertificateResult payload = new GetLatestRdsCertificateResult(STACK_ID, ROTATE);
        when(stateContext.getMessageHeader(MessageFactory.HEADERS.DATA.name())).thenReturn(payload);
        Action<?, ?> action = configureAction(underTest::updateLatestRdsCertificate);
        action.execute(stateContext);
        verify(rotateRdsCertificateService).updateLatestRdsCertificateState(STACK_ID);
        verify(eventBus).notify(selectorArgumentCaptor.capture(), eventArgumentCaptor.capture());
        assertThat(selectorArgumentCaptor.getValue()).isEqualTo(EventSelectorUtil.selector(UpdateLatestRdsCertificateRequest.class));
    }

    @Test
    void restartCmServiceAction() {
        UpdateLatestRdsCertificateResult payload = new UpdateLatestRdsCertificateResult(STACK_ID, ROTATE);
        when(stateContext.getMessageHeader(MessageFactory.HEADERS.DATA.name())).thenReturn(payload);
        Action<?, ?> action = configureAction(underTest::restartCmService);
        action.execute(stateContext);
        verify(rotateRdsCertificateService).restartCmState(STACK_ID);
        verify(eventBus).notify(selectorArgumentCaptor.capture(), eventArgumentCaptor.capture());
        assertThat(selectorArgumentCaptor.getValue()).isEqualTo(EventSelectorUtil.selector(RestartCmRequest.class));
    }

    @Test
    void rollingRestartRdsCertificateAction() {
        RestartCmResult payload = new RestartCmResult(STACK_ID, ROTATE);
        when(stateContext.getMessageHeader(MessageFactory.HEADERS.DATA.name())).thenReturn(payload);
        Action<?, ?> action = configureAction(underTest::rollingRestartRdsCertificate);
        action.execute(stateContext);
        verify(rotateRdsCertificateService).rollingRestartRdsCertificateState(STACK_ID);
        verify(eventBus).notify(selectorArgumentCaptor.capture(), eventArgumentCaptor.capture());
        assertThat(selectorArgumentCaptor.getValue()).isEqualTo(EventSelectorUtil.selector(RollingRestartServicesRequest.class));
    }

    @Test
    void rotateRdsCertOnProviderAction() {
        RollingRestartServicesResult payload = new RollingRestartServicesResult(STACK_ID, ROTATE);
        when(stateContext.getMessageHeader(MessageFactory.HEADERS.DATA.name())).thenReturn(payload);
        Action<?, ?> action = configureAction(underTest::rotateRdsCertOnProvider);
        action.execute(stateContext);
        verify(rotateRdsCertificateService).rotateOnProviderState(STACK_ID);
        verify(eventBus).notify(selectorArgumentCaptor.capture(), eventArgumentCaptor.capture());
        assertThat(selectorArgumentCaptor.getValue()).isEqualTo(EventSelectorUtil.selector(RotateRdsCertificateOnProviderRequest.class));
    }

    @Test
    void rotateRdsCertFinishedAction() {
        RotateRdsCertificateOnProviderResult payload = new RotateRdsCertificateOnProviderResult(STACK_ID, ROTATE);
        when(stateContext.getMessageHeader(MessageFactory.HEADERS.DATA.name())).thenReturn(payload);
        Action<?, ?> action = configureAction(underTest::rotateRdsCertFinished);
        action.execute(stateContext);
        verify(rotateRdsCertificateService).rotateRdsCertFinished(STACK_ID);
        verify(metricService).incrementMetricCounter(MetricType.ROTATE_RDS_CERTIFICATE_SUCCESSFUL, STACK);
        verify(eventBus).notify(selectorArgumentCaptor.capture(), eventArgumentCaptor.capture());
        assertThat(selectorArgumentCaptor.getValue()).isEqualTo(FINALIZED_EVENT.event());
    }

    @Test
    void rotateRdsCertFailedAction() {
        RuntimeException expectedException = new RuntimeException(MESSAGE);
        RotateRdsCertificateFailedEvent failedPayload = new RotateRdsCertificateFailedEvent(STACK_ID, ROTATE, expectedException);

        when(stateContext.getMessageHeader(MessageFactory.HEADERS.DATA.name())).thenReturn(failedPayload);
        Action<?, ?> action = configureAction(underTest::rotateRdsCertFailed);
        action.execute(stateContext);

        verify(rotateRdsCertificateService).rotateRdsCertFailed(failedPayload);
        verify(eventBus).notify(selectorArgumentCaptor.capture(), eventArgumentCaptor.capture());
        verify(metricService).incrementMetricCounter(MetricType.ROTATE_RDS_CERTIFICATE_FAILED, STACK);
        assertThat(selectorArgumentCaptor.getValue()).isEqualTo(FAIL_HANDLED_EVENT.event());
    }

    private Action<?, ?> configureAction(Supplier<Action<?, ?>> actionSupplier) {
        Action<?, ?> action = actionSupplier.get();
        assertThat(action).isNotNull();
        setActionPrivateFields(action);
        AbstractAction abstractAction = (AbstractAction) action;
        abstractAction.setFailureEvent(failureEvent);
        return action;
    }

    private void setActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
        ReflectionTestUtils.setField(action, null, stackDtoService, StackDtoService.class);
        ReflectionTestUtils.setField(action, null, metricService, MetricService.class);
    }
}
