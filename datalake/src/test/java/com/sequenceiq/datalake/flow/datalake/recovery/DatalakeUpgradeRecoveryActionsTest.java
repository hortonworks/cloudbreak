package com.sequenceiq.datalake.flow.datalake.recovery;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_RECOVERY_STARTED;
import static com.sequenceiq.datalake.flow.datalake.recovery.DatalakeUpgradeRecoveryEvent.DATALAKE_RECOVERY_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.action.Action;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.events.EventSenderService;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.datalake.recovery.event.DatalakeRecoveryStartEvent;
import com.sequenceiq.datalake.flow.datalake.recovery.event.DatalakeRecoverySuccessEvent;
import com.sequenceiq.datalake.service.sdx.SdxRecoveryService;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxRecoveryType;

@ExtendWith(MockitoExtension.class)
public class DatalakeUpgradeRecoveryActionsTest {

    private static final Long SDX_ID = 1L;

    private static final String DATALAKE_NAME = "test_dl";

    private static final String FLOW_ID = "flow_id_1";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    @Mock
    private SdxRecoveryService sdxRecoveryService;

    @Mock
    private SdxStatusService sdxStatusService;

    @Mock
    private EventSenderService eventSenderService;

    @Mock
    private SdxService sdxService;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private EventBus eventBus;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @InjectMocks
    private DatalakeUpgradeRecoveryActions datalakeUpgradeRecoveryActions;

    @Test
    public void datalakeRecoveryStart() throws Exception {
        SdxCluster sdxCluster = generateCluster();
        when(sdxService.getById(anyLong())).thenReturn(sdxCluster);
        AbstractAction action = (AbstractAction) datalakeUpgradeRecoveryActions.datalakeRecoveryStart();
        initActionPrivateFields(action);
        AbstractActionTestSupport testSupport = new AbstractActionTestSupport(action);
        DatalakeRecoveryStartEvent datalakeRecoveryStartEvent = new DatalakeRecoveryStartEvent(DATALAKE_RECOVERY_EVENT.event(),
                SDX_ID, USER_CRN, SdxRecoveryType.RECOVER_WITHOUT_DATA);
        SdxContext sdxContext = SdxContext.from(new FlowParameters(FLOW_ID, FLOW_ID, null), datalakeRecoveryStartEvent);
        when(runningFlows.getFlowChainId(anyString())).thenReturn(FLOW_ID);
        testSupport.doExecute(sdxContext, datalakeRecoveryStartEvent, new HashMap<>());
        verify(sdxRecoveryService).recoverCluster(anyLong());
        verify(eventSenderService, times(1))
                .sendEventAndNotification(any(SdxCluster.class), eq(DATALAKE_RECOVERY_STARTED));
        ArgumentCaptor<DatalakeRecoveryStartEvent> captor = ArgumentCaptor.forClass(DatalakeRecoveryStartEvent.class);
        verify(reactorEventFactory, times(1)).createEvent(any(), captor.capture());
        DatalakeRecoveryStartEvent captorValue = captor.getValue();
        assertEquals(SDX_ID, captorValue.getResourceId());
        assertEquals(USER_CRN, captorValue.getUserId());
    }

    @Test
    public void datalakeRecoveryFinished() throws Exception {
        AbstractAction action = (AbstractAction) datalakeUpgradeRecoveryActions.finishedAction();
        initActionPrivateFields(action);
        AbstractActionTestSupport testSupport = new AbstractActionTestSupport(action);
        DatalakeRecoverySuccessEvent successEvent = new DatalakeRecoverySuccessEvent(SDX_ID, USER_CRN);
        SdxContext sdxContext = SdxContext.from(new FlowParameters(FLOW_ID, FLOW_ID, null), successEvent);
        when(runningFlows.getFlowChainId(anyString())).thenReturn(FLOW_ID);

        testSupport.doExecute(sdxContext, successEvent, new HashMap<>());
        ArgumentCaptor<DatalakeRecoverySuccessEvent> captor = ArgumentCaptor.forClass(DatalakeRecoverySuccessEvent.class);
        verify(reactorEventFactory, times(1)).createEvent(any(), captor.capture());

        assertEquals(SDX_ID, captor.getValue().getResourceId());
        assertEquals(USER_CRN, captor.getValue().getUserId());

        verify(sdxStatusService, times(1)).setStatusForDatalakeAndNotify(
                eq(DatalakeStatusEnum.RUNNING),
                eq(ResourceEvent.DATALAKE_RECOVERY_FINISHED),
                eq("Recovery finished"),
                eq(SDX_ID));
    }

    private SdxCluster generateCluster() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setId(SDX_ID);
        sdxCluster.setClusterShape(SdxClusterShape.LIGHT_DUTY);
        sdxCluster.setEnvName("env");
        sdxCluster.setEnvCrn("crn");
        sdxCluster.setClusterName(DATALAKE_NAME);
        return sdxCluster;
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
    }

}