package com.sequenceiq.datalake.flow.scale;

import static com.sequenceiq.datalake.flow.datalake.scale.DatalakeHorizontalScaleEvent.DATALAKE_HORIZONTAL_SCALE_EVENT;
import static com.sequenceiq.datalake.flow.datalake.scale.DatalakeHorizontalScaleEvent.DATALAKE_HORIZONTAL_SCALE_FAILED_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.time.Instant;
import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.action.Action;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.events.EventSenderService;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.datalake.scale.DatalakeHorizontalScaleActions;
import com.sequenceiq.datalake.flow.datalake.scale.event.DatalakeHorizontalScaleFlowEvent;
import com.sequenceiq.datalake.flow.datalake.scale.event.DatalakeHorizontalScaleSdxEvent;
import com.sequenceiq.datalake.service.sdx.SdxHorizontalScalingService;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.sdx.api.model.DatalakeHorizontalScaleRequest;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

@SuppressWarnings({"rawtypes", "unchecked"})
@ExtendWith(MockitoExtension.class)
public class DatalakeHorizontalScaleActionTest {

    private static final Long SDX_ID = 1L;

    private static final String FLOW_ID = "flow_id";

    private static final String SDX_NAME = "sdx_name";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    private static final String DATALAKE_CRN = "crn:cdp:datalake:us-west-1:460c0d8f-ae8e-4dce-9cd7-2351762eb9ac:datalake:6b2b1600-8ac6-4c26-aa34-dab36f4bd243";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:460c0d8f-ae8e-4dce-9cd7-2351762eb9ac:environment:6b2b1600-8ac6-4c26-aa34-dab36f4bd243";

    @InjectMocks
    private final DatalakeHorizontalScaleActions underTest = new DatalakeHorizontalScaleActions();

    @Mock
    private SdxService sdxService;

    @Mock
    private SdxStatusService sdxStatusService;

    @Mock
    private EventSenderService eventSenderService;

    @Mock
    private SdxHorizontalScalingService sdxHorizontalScalingService;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private EventBus eventBus;

    @Before
    public void setup() {
        openMocks(this);
    }

    @Test
    public void testStartTriggersCall() throws Exception {
        when(sdxService.getById(any())).thenReturn(getSdxCluster());
        DatalakeHorizontalScaleRequest scaleRequest = new DatalakeHorizontalScaleRequest();
        scaleRequest.setDesiredCount(1);
        scaleRequest.setGroup("solr_scale_out");
        DatalakeHorizontalScaleSdxEvent datalakeHorizontalScaleSdxEvent = new DatalakeHorizontalScaleSdxEvent(
                DATALAKE_HORIZONTAL_SCALE_EVENT.selector(),
                SDX_ID, SDX_NAME, USER_CRN, DATALAKE_CRN, scaleRequest, null, null
        );
        AbstractAction action = (AbstractAction) underTest.datalakeHorizontalScaleStart();
        initActionPrivateFields(action);
        AbstractActionTestSupport abstractActionTestSupport = new AbstractActionTestSupport<>(action);
        SdxContext sdxContext = SdxContext.from(new FlowParameters(FLOW_ID, USER_CRN), datalakeHorizontalScaleSdxEvent);
        abstractActionTestSupport.doExecute(sdxContext, datalakeHorizontalScaleSdxEvent, new HashMap<>());
        ArgumentCaptor<DatalakeHorizontalScaleSdxEvent> captor = ArgumentCaptor.forClass(DatalakeHorizontalScaleSdxEvent.class);
        verify(reactorEventFactory, times(1)).createEvent(any(), captor.capture());
        DatalakeHorizontalScaleSdxEvent value = captor.getValue();
        assertEquals(SDX_ID, value.getResourceId());
        assertEquals(USER_CRN, value.getUserId());
        assertEquals(DATALAKE_CRN, value.getResourceCrn());
        assertEquals(1, value.getScaleRequest().getDesiredCount());
        assertEquals("solr_scale_out", value.getScaleRequest().getGroup());
    }

    @Test
    public void testValidationFail() throws Exception {
        when(sdxService.getById(any())).thenReturn(getSdxCluster());
        doCallRealMethod().when(sdxHorizontalScalingService).validateHorizontalScaleRequest(any(SdxCluster.class), any(DatalakeHorizontalScaleRequest.class));
        DatalakeHorizontalScaleRequest scaleRequest = new DatalakeHorizontalScaleRequest();
        scaleRequest.setDesiredCount(1);
        scaleRequest.setGroup("master");
        DatalakeHorizontalScaleSdxEvent datalakeHorizontalScaleSdxEvent = new DatalakeHorizontalScaleSdxEvent(
                DATALAKE_HORIZONTAL_SCALE_EVENT.selector(),
                SDX_ID, SDX_NAME, USER_CRN, DATALAKE_CRN, scaleRequest, null, null
        );
        AbstractAction action = (AbstractAction) underTest.datalakeHorizontalScaleValidationStart();
        initActionPrivateFields(action);
        AbstractActionTestSupport abstractActionTestSupport = new AbstractActionTestSupport<>(action);
        SdxContext sdxContext = SdxContext.from(new FlowParameters(FLOW_ID, USER_CRN), datalakeHorizontalScaleSdxEvent);
        abstractActionTestSupport.doExecute(sdxContext, datalakeHorizontalScaleSdxEvent, new HashMap<>());
        ArgumentCaptor<DatalakeHorizontalScaleFlowEvent> captor = ArgumentCaptor.forClass(DatalakeHorizontalScaleFlowEvent.class);
        verify(reactorEventFactory, times(1)).createEvent(any(), captor.capture());
        DatalakeHorizontalScaleFlowEvent value = captor.getValue();
        assertEquals(SDX_ID, value.getResourceId());
        assertEquals(USER_CRN, value.getUserId());
        assertEquals(DATALAKE_CRN, value.getResourceCrn());
        assertEquals(1, value.getScaleRequest().getDesiredCount());
        assertEquals(BadRequestException.class, value.getException().getClass());
    }

    @Test
    public void testFailHandling() throws Exception {
        when(sdxService.getById(any())).thenReturn(getSdxCluster());
        doCallRealMethod().when(sdxHorizontalScalingService).validateHorizontalScaleRequest(any(SdxCluster.class), any(DatalakeHorizontalScaleRequest.class));
        DatalakeHorizontalScaleRequest scaleRequest = new DatalakeHorizontalScaleRequest();
        scaleRequest.setDesiredCount(1);
        scaleRequest.setGroup("master");
        DatalakeHorizontalScaleFlowEvent datalakeHorizontalScaleSdxEvent = new DatalakeHorizontalScaleFlowEvent(
                DATALAKE_HORIZONTAL_SCALE_FAILED_EVENT.selector(),
                SDX_ID, SDX_NAME, DATALAKE_CRN, USER_CRN, scaleRequest, null, null
        );
        AbstractAction action = (AbstractAction) underTest.datalakeHorizontalScaleFailed();
        initActionPrivateFields(action);
        AbstractActionTestSupport abstractActionTestSupport = new AbstractActionTestSupport<>(action);
        SdxContext sdxContext = SdxContext.from(new FlowParameters(FLOW_ID, USER_CRN), new SdxEvent(SDX_ID, USER_CRN));
        abstractActionTestSupport.doExecute(sdxContext, datalakeHorizontalScaleSdxEvent, new HashMap<>());
        ArgumentCaptor<DatalakeHorizontalScaleFlowEvent> captor = ArgumentCaptor.forClass(DatalakeHorizontalScaleFlowEvent.class);
        verify(reactorEventFactory, times(1)).createEvent(any(), captor.capture());
        DatalakeHorizontalScaleFlowEvent value = captor.getValue();
        assertEquals(SDX_ID, value.getResourceId());
        assertEquals(USER_CRN, value.getUserId());
        assertEquals(DATALAKE_CRN, value.getResourceCrn());
    }

    @Test
    public void testFailHandlingWithException() throws Exception {
        when(sdxService.getById(any())).thenReturn(getSdxCluster());
        doCallRealMethod().when(sdxHorizontalScalingService).validateHorizontalScaleRequest(any(SdxCluster.class), any(DatalakeHorizontalScaleRequest.class));
        DatalakeHorizontalScaleRequest scaleRequest = new DatalakeHorizontalScaleRequest();
        scaleRequest.setDesiredCount(1);
        scaleRequest.setGroup("master");
        DatalakeHorizontalScaleFlowEvent datalakeHorizontalScaleSdxEvent = new DatalakeHorizontalScaleFlowEvent(
                DATALAKE_HORIZONTAL_SCALE_FAILED_EVENT.selector(),
                SDX_ID, SDX_NAME, DATALAKE_CRN, USER_CRN, scaleRequest, null, new BadRequestException("")
        );
        AbstractAction action = (AbstractAction) underTest.datalakeHorizontalScaleFailed();
        initActionPrivateFields(action);
        AbstractActionTestSupport abstractActionTestSupport = new AbstractActionTestSupport<>(action);
        SdxContext sdxContext = SdxContext.from(new FlowParameters(FLOW_ID, USER_CRN), new SdxEvent(SDX_ID, USER_CRN));
        abstractActionTestSupport.doExecute(sdxContext, datalakeHorizontalScaleSdxEvent, new HashMap<>());
        ArgumentCaptor<DatalakeHorizontalScaleFlowEvent> captor = ArgumentCaptor.forClass(DatalakeHorizontalScaleFlowEvent.class);
        verify(reactorEventFactory, times(1)).createEvent(any(), captor.capture());
        DatalakeHorizontalScaleFlowEvent value = captor.getValue();
        assertEquals(SDX_ID, value.getResourceId());
        assertEquals(USER_CRN, value.getUserId());
        assertEquals(DATALAKE_CRN, value.getResourceCrn());
        assertEquals(BadRequestException.class, value.getException().getClass());
    }

    private SdxCluster getSdxCluster() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setClusterName(SDX_NAME);
        sdxCluster.setClusterShape(SdxClusterShape.ENTERPRISE);
        sdxCluster.setCrn(DATALAKE_CRN);
        sdxCluster.setCreated(Instant.now().toEpochMilli());
        sdxCluster.setId(SDX_ID);
        sdxCluster.setEnvCrn(ENV_CRN);
        return sdxCluster;
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
        ReflectionTestUtils.setField(underTest, null, sdxService, SdxService.class);
    }
}
