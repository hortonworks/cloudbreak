package com.sequenceiq.datalake.flow.create;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.quartz.statuschecker.service.StatusCheckerJobService;
import com.sequenceiq.datalake.events.EventSenderService;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.create.event.SdxCreateFailedEvent;
import com.sequenceiq.datalake.flow.create.event.StorageConsumptionCollectionSchedulingSuccessEvent;
import com.sequenceiq.datalake.metric.SdxMetricService;
import com.sequenceiq.datalake.service.AbstractSdxAction;
import com.sequenceiq.datalake.service.sdx.ProvisionerService;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

@ExtendWith(MockitoExtension.class)
class SdxCreateActionsTest {

    private static final Long SDX_ID = 123L;

    private static final String USER_ID = "userId";

    private static final String FLOW_ID = "flowId";

    private static final String FLOW_TRIGGER_USER_CRN = "flowTriggerUserCrn";

    private static final String FLOW_CHAIN_ID = "flowChainId";

    @Mock
    private ProvisionerService provisionerService;

    @Mock
    private SdxStatusService sdxStatusService;

    @Mock
    private StatusCheckerJobService jobService;

    @Mock
    private SdxMetricService metricService;

    @Mock
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    @Mock
    private SdxService sdxService;

    @Mock
    private FlowLogService flowLogService;

    @Mock
    private EventSenderService eventSenderService;

    @InjectMocks
    private SdxCreateActions underTest;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @Mock
    private EventBus eventBus;

    private FlowParameters flowParameters;

    @Mock
    private StateContext<FlowState, FlowEvent> stateContext;

    private DetailedEnvironmentResponse detailedEnvironmentResponse;

    @Mock
    private Event<?> event;

    @Captor
    private ArgumentCaptor<Map<String, Object>> headersCaptor;

    @BeforeEach
    void setUp() {
        flowParameters = new FlowParameters(FLOW_ID, FLOW_TRIGGER_USER_CRN);
        detailedEnvironmentResponse = DetailedEnvironmentResponse.builder()
                .build();

        lenient().when(runningFlows.getFlowChainId(FLOW_ID)).thenReturn(FLOW_CHAIN_ID);
        lenient().when(reactorEventFactory.createEvent(anyMap(), any())).thenReturn((Event<Object>) event);
    }

    private <P extends SdxEvent> AbstractSdxAction<P> initAction(Supplier<Action<?, ?>> actionSupplier) {
        AbstractSdxAction<P> action = (AbstractSdxAction<P>) actionSupplier.get();
        ReflectionTestUtils.setField(action, "runningFlows", runningFlows);
        ReflectionTestUtils.setField(action, "reactorEventFactory", reactorEventFactory);
        ReflectionTestUtils.setField(action, "eventBus", eventBus);
        return action;
    }

    private void verifySdxContext(SdxContext result) {
        assertThat(result).isNotNull();
        assertThat(result.getSdxId()).isEqualTo(SDX_ID);
        assertThat(result.getUserId()).isEqualTo(USER_ID);
        assertThat(result.getFlowParameters()).isSameAs(flowParameters);
    }

    private void verifyEvent(ArgumentCaptor<?> payloadCaptor, String selectorExpected) {
        verify(reactorEventFactory).createEvent(headersCaptor.capture(), payloadCaptor.capture());
        verify(eventBus).notify(selectorExpected, event);

        verifyHeaders();
    }

    private void verifyHeaders() {
        Map<String, Object> headers = headersCaptor.getValue();
        assertThat(headers).isNotNull();
        assertThat(headers).containsOnly(entry(FlowConstants.FLOW_ID, FLOW_ID), entry(FlowConstants.FLOW_TRIGGER_USERCRN, FLOW_TRIGGER_USER_CRN),
                entry(FlowConstants.FLOW_OPERATION_TYPE, "UNKNOWN"), entry(FlowConstants.FLOW_CHAIN_ID, FLOW_CHAIN_ID));
    }

    private void verifyFailurePayload(Object failurePayload, Exception exExpected) {
        assertThat(failurePayload).isInstanceOf(SdxCreateFailedEvent.class);

        SdxCreateFailedEvent sdxCreateFailedEvent = (SdxCreateFailedEvent) failurePayload;
        assertThat(sdxCreateFailedEvent.getSdxName()).isNull();
        assertThat(sdxCreateFailedEvent.getException()).isSameAs(exExpected);
        assertThat(sdxCreateFailedEvent.getUserId()).isEqualTo(USER_ID);
        assertThat(sdxCreateFailedEvent.getResourceId()).isEqualTo(SDX_ID);
    }

    @Test
    void sdxCreationTestCreateFlowContext() {
        StorageConsumptionCollectionSchedulingSuccessEvent payload =
                new StorageConsumptionCollectionSchedulingSuccessEvent(SDX_ID, USER_ID, detailedEnvironmentResponse);
        AbstractActionTestSupport<FlowState, FlowEvent, SdxContext, StorageConsumptionCollectionSchedulingSuccessEvent> testSupport =
                new AbstractActionTestSupport<>(initAction(underTest::sdxCreation));

        SdxContext result = testSupport.createFlowContext(flowParameters, stateContext, payload);

        verifySdxContext(result);
    }

    @Test
    void sdxCreationTestDoExecute() throws Exception {
        SdxContext context = new SdxContext(flowParameters, SDX_ID, USER_ID);
        StorageConsumptionCollectionSchedulingSuccessEvent payload =
                new StorageConsumptionCollectionSchedulingSuccessEvent(SDX_ID, USER_ID, detailedEnvironmentResponse);
        ArgumentCaptor<StorageConsumptionCollectionSchedulingSuccessEvent> successEventCaptor =
                ArgumentCaptor.forClass(StorageConsumptionCollectionSchedulingSuccessEvent.class);
        AbstractActionTestSupport<FlowState, FlowEvent, SdxContext, StorageConsumptionCollectionSchedulingSuccessEvent> testSupport =
                new AbstractActionTestSupport<>(initAction(underTest::sdxCreation));

        testSupport.doExecute(context, payload, Map.of());

        verify(provisionerService).startStackProvisioning(SDX_ID, detailedEnvironmentResponse);

        verifyEvent(successEventCaptor, "SDX_STACK_CREATION_IN_PROGRESS_EVENT");

        StorageConsumptionCollectionSchedulingSuccessEvent successEvent = successEventCaptor.getValue();
        assertThat(successEvent).isSameAs(payload);
    }

    @Test
    void sdxCreationTestGetFailurePayload() {
        StorageConsumptionCollectionSchedulingSuccessEvent payload =
                new StorageConsumptionCollectionSchedulingSuccessEvent(SDX_ID, USER_ID, detailedEnvironmentResponse);
        AbstractActionTestSupport<FlowState, FlowEvent, SdxContext, StorageConsumptionCollectionSchedulingSuccessEvent> testSupport =
                new AbstractActionTestSupport<>(initAction(underTest::sdxCreation));
        Exception ex = new UnsupportedOperationException();

        Object result = testSupport.getFailurePayload(payload, Optional.empty(), ex);

        verifyFailurePayload(result, ex);
    }

}