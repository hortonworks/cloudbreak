package com.sequenceiq.datalake.flow.delete;

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
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import com.sequenceiq.datalake.flow.delete.event.RdsDeletionWaitRequest;
import com.sequenceiq.datalake.flow.delete.event.SdxDeletionFailedEvent;
import com.sequenceiq.datalake.flow.delete.event.StorageConsumptionCollectionUnschedulingSuccessEvent;
import com.sequenceiq.datalake.metric.SdxMetricService;
import com.sequenceiq.datalake.service.AbstractSdxAction;
import com.sequenceiq.datalake.service.sdx.ProvisionerService;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.flow.service.flowlog.FlowChainLogService;

@ExtendWith(MockitoExtension.class)
class SdxDeleteActionsTest {

    private static final Long SDX_ID = 123L;

    private static final String USER_ID = "userId";

    private static final String FLOW_ID = "flowId";

    private static final String FLOW_TRIGGER_USER_CRN = "flowTriggerUserCrn";

    private static final String FLOW_CHAIN_ID = "flowChainId";

    @Mock
    private SdxStatusService sdxStatusService;

    @Mock
    private ProvisionerService provisionerService;

    @Mock
    private StatusCheckerJobService jobService;

    @Mock
    private SdxService sdxService;

    @Mock
    private SdxMetricService metricService;

    @Mock
    private EventSenderService eventSenderService;

    @Mock
    private FlowLogService flowLogService;

    @Mock
    private FlowChainLogService flowChainLogService;

    @Mock
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    @InjectMocks
    private SdxDeleteActions underTest;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @Mock
    private EventBus eventBus;

    private FlowParameters flowParameters;

    @Mock
    private StateContext<FlowState, FlowEvent> stateContext;

    @Mock
    private Event<?> event;

    @Captor
    private ArgumentCaptor<Map<String, Object>> headersCaptor;

    @BeforeEach
    void setUp() {
        flowParameters = new FlowParameters(FLOW_ID, FLOW_TRIGGER_USER_CRN);

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

    private void verifyFailurePayload(Object failurePayload, Exception exExpected, boolean forcedExpected) {
        assertThat(failurePayload).isInstanceOf(SdxDeletionFailedEvent.class);

        SdxDeletionFailedEvent sdxDeletionFailedEvent = (SdxDeletionFailedEvent) failurePayload;
        assertThat(sdxDeletionFailedEvent.getSdxName()).isNull();
        assertThat(sdxDeletionFailedEvent.getException()).isSameAs(exExpected);
        assertThat(sdxDeletionFailedEvent.isForced()).isSameAs(forcedExpected);
        assertThat(sdxDeletionFailedEvent.getUserId()).isEqualTo(USER_ID);
        assertThat(sdxDeletionFailedEvent.getResourceId()).isEqualTo(SDX_ID);
    }

    @ParameterizedTest(name = "forced={0}")
    @ValueSource(booleans = {false, true})
    void sdxDeleteRdsActionTestCreateFlowContext(boolean forced) {
        StorageConsumptionCollectionUnschedulingSuccessEvent payload = new StorageConsumptionCollectionUnschedulingSuccessEvent(SDX_ID, USER_ID, forced);
        AbstractActionTestSupport<FlowState, FlowEvent, SdxContext, StorageConsumptionCollectionUnschedulingSuccessEvent> testSupport =
                new AbstractActionTestSupport<>(initAction(underTest::sdxDeleteRdsAction));

        SdxContext result = testSupport.createFlowContext(flowParameters, stateContext, payload);

        verifySdxContext(result);
    }

    @ParameterizedTest(name = "forced={0}")
    @ValueSource(booleans = {false, true})
    void sdxDeleteRdsActionTestDoExecute(boolean forced) throws Exception {
        SdxContext context = new SdxContext(flowParameters, SDX_ID, USER_ID);
        StorageConsumptionCollectionUnschedulingSuccessEvent payload = new StorageConsumptionCollectionUnschedulingSuccessEvent(SDX_ID, USER_ID, forced);
        ArgumentCaptor<RdsDeletionWaitRequest> rdsDeletionWaitRequestCaptor = ArgumentCaptor.forClass(RdsDeletionWaitRequest.class);
        AbstractActionTestSupport<FlowState, FlowEvent, SdxContext, StorageConsumptionCollectionUnschedulingSuccessEvent> testSupport =
                new AbstractActionTestSupport<>(initAction(underTest::sdxDeleteRdsAction));

        testSupport.doExecute(context, payload, Map.of());

        verifyEvent(rdsDeletionWaitRequestCaptor, "RdsDeletionWaitRequest");

        RdsDeletionWaitRequest rdsDeletionWaitRequest = rdsDeletionWaitRequestCaptor.getValue();
        assertThat(rdsDeletionWaitRequest).isNotNull();
        assertThat(rdsDeletionWaitRequest.isForced()).isEqualTo(forced);
        assertThat(rdsDeletionWaitRequest.getUserId()).isEqualTo(USER_ID);
        assertThat(rdsDeletionWaitRequest.getSdxName()).isNull();
        assertThat(rdsDeletionWaitRequest.getResourceId()).isEqualTo(SDX_ID);
    }

    @ParameterizedTest(name = "forced={0}")
    @ValueSource(booleans = {false, true})
    void sdxDeleteRdsActionTestGetFailurePayload(boolean forced) {
        StorageConsumptionCollectionUnschedulingSuccessEvent payload = new StorageConsumptionCollectionUnschedulingSuccessEvent(SDX_ID, USER_ID, forced);
        AbstractActionTestSupport<FlowState, FlowEvent, SdxContext, StorageConsumptionCollectionUnschedulingSuccessEvent> testSupport =
                new AbstractActionTestSupport<>(initAction(underTest::sdxDeleteRdsAction));
        Exception ex = new UnsupportedOperationException();

        Object result = testSupport.getFailurePayload(payload, Optional.empty(), ex);

        verifyFailurePayload(result, ex, forced);
    }

}