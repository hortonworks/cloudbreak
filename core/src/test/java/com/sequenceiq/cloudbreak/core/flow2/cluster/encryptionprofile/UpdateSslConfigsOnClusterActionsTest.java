package com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.UpdateSslConfigsOnClusterStateSelectors.FINALIZE_UPDATE_SSL_CONFIGS_ON_CLUSTER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.UpdateSslConfigsOnClusterStateSelectors.FINISH_UPDATE_SSL_CONFIGS_ON_CLUSTER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.UpdateSslConfigsOnClusterStateSelectors.GENERATE_ALTERNATIVE_CERTIFICATE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.UpdateSslConfigsOnClusterStateSelectors.GENERATE_ALTERNATIVE_CERTIFICATE_HANDLER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.UpdateSslConfigsOnClusterStateSelectors.HANDLED_FAILED_UPDATE_SSL_CONFIGS_ON_CLUSTER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.UpdateSslConfigsOnClusterStateSelectors.SET_ENCRYPTION_PROFILE_HANDLER_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.action.Action;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.common.metrics.MetricService;
import com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.event.UpdateSslConfigEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.event.UpdateSslConfigFailedEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

@ExtendWith(MockitoExtension.class)
class UpdateSslConfigsOnClusterActionsTest {

    private static final String ENCRYPTION_PROFILE_CRN = "ENCRYPTION_PROFILE_CRN";

    private Map<Object, Object> variables;

    private StackContext context;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private EventBus eventBus;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @Mock
    private FlowParameters flowParameters;

    @Captor
    private ArgumentCaptor<String> captor;

    @Mock
    private Stack stack;

    @Mock
    private CloudStack cloudStack;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private CloudbreakFlowMessageService flowMessageService;

    @Mock
    private MetricService metricService;

    @InjectMocks
    private UpdateSslConfigsOnClusterActions underTest;

    @BeforeEach
    void setUp() {
        variables = new HashMap<>();
        context = new StackContext(flowParameters, stack, cloudContext, cloudCredential, cloudStack);
    }

    @Test
    void testSetEncryptionProfileEventAction() throws Exception {
        UpdateSslConfigEvent event = new UpdateSslConfigEvent(SET_ENCRYPTION_PROFILE_HANDLER_EVENT.event(), 1L, ENCRYPTION_PROFILE_CRN);

        doReturn(new Event<>(new Event.Headers(new HashMap<>()), event)).when(reactorEventFactory).createEvent(any(), any());
        when(stack.getId()).thenReturn(1L);
        when(stack.getType()).thenReturn(StackType.DATALAKE);

        AbstractUpdateSslConfigsOnClusterAction<UpdateSslConfigEvent> action =
                (AbstractUpdateSslConfigsOnClusterAction<UpdateSslConfigEvent>) underTest.setEncryptionProfileEventAction();
        initActionPrivateFields(action);

        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);

        verify(stackUpdater).updateStackStatus(eq(1L), eq(DetailedStackStatus.UPDATE_SSL_CONFIG_ON_CLUSTER_IN_PROGRESS),
                eq("Starting to update SSL configs"));
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(captor.capture(), eventCaptor.capture());
        String selector = SET_ENCRYPTION_PROFILE_HANDLER_EVENT.name();
        assertEquals(selector, captor.getValue());
        assertEquals(1L, ReflectionTestUtils.getField(eventCaptor.getValue().getData(), "stackId"));
    }

    @Test
    void testGenerateAlternativeCertificateAction() throws Exception {
        UpdateSslConfigEvent event = new UpdateSslConfigEvent(GENERATE_ALTERNATIVE_CERTIFICATE_EVENT.event(), 1L, ENCRYPTION_PROFILE_CRN);
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), event)).when(reactorEventFactory).createEvent(any(), any());
        when(stack.getId()).thenReturn(1L);

        AbstractUpdateSslConfigsOnClusterAction<UpdateSslConfigEvent> action =
                (AbstractUpdateSslConfigsOnClusterAction<UpdateSslConfigEvent>) underTest.generateAlternativeCertificateAction();
        initActionPrivateFields(action);

        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(captor.capture(), eventCaptor.capture());
        String selector = GENERATE_ALTERNATIVE_CERTIFICATE_HANDLER_EVENT.event();
        assertEquals(selector, captor.getValue());
    }

    @Test
    void testFinishedAction() throws Exception {
        UpdateSslConfigEvent event = new UpdateSslConfigEvent(FINISH_UPDATE_SSL_CONFIGS_ON_CLUSTER_EVENT.event(), 1L, ENCRYPTION_PROFILE_CRN);
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), event)).when(reactorEventFactory).createEvent(any(), any());
        when(stack.getId()).thenReturn(1L);
        when(stack.getType()).thenReturn(StackType.WORKLOAD);

        AbstractUpdateSslConfigsOnClusterAction<UpdateSslConfigEvent> action =
                (AbstractUpdateSslConfigsOnClusterAction<UpdateSslConfigEvent>) underTest.finishedAction();
        initActionPrivateFields(action);

        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);

        verify(stackUpdater).updateStackStatus(eq(1L), eq(DetailedStackStatus.UPDATE_SSL_CONFIG_ON_CLUSTER_COMPLETE),
                eq("SSL configs updated"));
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(captor.capture(), eventCaptor.capture());
        String selector = FINALIZE_UPDATE_SSL_CONFIGS_ON_CLUSTER_EVENT.event();
        assertEquals(selector, captor.getValue());
    }

    @Test
    void testFailedAction() throws Exception {
        UpdateSslConfigFailedEvent event = new UpdateSslConfigFailedEvent(1L, new CloudbreakException("Failed!"));

        when(stack.getId()).thenReturn(1L);

        AbstractUpdateSslConfigsOnClusterAction<UpdateSslConfigFailedEvent> action =
                (AbstractUpdateSslConfigsOnClusterAction<UpdateSslConfigFailedEvent>) underTest.failedAction();
        initActionPrivateFields(action);

        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);

        verify(stackUpdater).updateStackStatus(eq(1L), eq(DetailedStackStatus.UPDATE_SSL_CONFIG_ON_CLUSTER_FAILED), eq("Failed!"));
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(captor.capture(), eventCaptor.capture());
        String selector = HANDLED_FAILED_UPDATE_SSL_CONFIGS_ON_CLUSTER_EVENT.event();
        assertEquals(selector, captor.getValue());
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
    }
}
