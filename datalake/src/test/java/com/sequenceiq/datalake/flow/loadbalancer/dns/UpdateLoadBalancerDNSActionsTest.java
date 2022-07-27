package com.sequenceiq.datalake.flow.loadbalancer.dns;

import static com.sequenceiq.datalake.flow.loadbalancer.dns.UpdateLoadBalancerDNSEvent.UPDATE_LOAD_BALANCER_DNS_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.action.Action;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.events.EventSenderService;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.chain.DatalakeResizeFlowEventChainFactory;
import com.sequenceiq.datalake.flow.loadbalancer.dns.event.StartUpdateLoadBalancerDNSEvent;
import com.sequenceiq.datalake.service.loadbalancer.dns.UpdateLoadBalancerDNSService;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.domain.FlowChainLog;
import com.sequenceiq.flow.domain.FlowLogWithoutPayload;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.flow.service.flowlog.FlowChainLogService;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

import reactor.bus.EventBus;
import reactor.rx.Promise;

@ExtendWith(MockitoExtension.class)
class UpdateLoadBalancerDNSActionsTest {

    private static final Long SDX_ID = 2L;

    private static final String FLOW_ID = "flow_id";

    private static final String DATALAKE_NAME = "test_dl";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    private static final String BACKUP_LOCATION = "s3://cloudbreak-bucket/backup-location";

    private static final String BACKUP_ID = "backup_id";

    private static final String RESTORE_ID = "restore_id";

    @Mock
    private SdxService sdxService;

    @Mock
    private UpdateLoadBalancerDNSService updateLoadBalancerDNSService;

    @Mock
    private SdxStatusService sdxStatusService;

    @Mock
    private EventSenderService eventSenderService;

    @Mock
    private FlowLogService flowLogService;

    @Mock
    private FlowChainLogService flowChainLogService;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private EventBus eventBus;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @InjectMocks
    private UpdateLoadBalancerDNSActions underTest;

    @Test
    public void updateLoadBalancerDNSActionNotResizeRecovery() throws Exception {
        SdxCluster sdxCluster = genCluster();
        FlowParameters flowParameters = new FlowParameters(FLOW_ID, null, null);
        when(sdxService.getById(eq(SDX_ID))).thenReturn(sdxCluster);
        when(flowLogService.getLastFlowLog(anyString())).thenReturn(Optional.of(mock(FlowLogWithoutPayload.class)));
        StartUpdateLoadBalancerDNSEvent event = new StartUpdateLoadBalancerDNSEvent(UPDATE_LOAD_BALANCER_DNS_EVENT.event(), SDX_ID, USER_CRN, new Promise<>());
        AbstractAction action = (AbstractAction) underTest.updateLoadBalancerDNSAction();
        initActionPrivateFields(action);
        AbstractActionTestSupport testSupport = new AbstractActionTestSupport(action);
        SdxContext sdxContext = SdxContext.from(flowParameters, event);
        testSupport.doExecute(sdxContext, event, new HashMap<>());
        verify(updateLoadBalancerDNSService, times(1))
                .performLoadBalancerDNSUpdate(eq(sdxCluster));
        verify(eventSenderService, times(1))
                .notifyEvent(eq(sdxCluster), eq(sdxContext), eq(ResourceEvent.UPDATE_LOAD_BALANCER_DNS_FINISHED));
        ArgumentCaptor<StartUpdateLoadBalancerDNSEvent> captor = ArgumentCaptor.forClass(StartUpdateLoadBalancerDNSEvent.class);
        verify(reactorEventFactory, times(1)).createEvent(any(), captor.capture());
        assertEquals(SDX_ID, captor.getValue().getResourceId());
        assertEquals(USER_CRN, captor.getValue().getUserId());
    }

    @Test
    public void updateLoadBalancerDNSActionResizeRecovery() throws Exception {
        SdxCluster sdxCluster = genCluster();
        FlowParameters flowParameters = new FlowParameters(FLOW_ID, null, null);
        when(sdxService.getById(eq(SDX_ID))).thenReturn(sdxCluster);
        FlowChainLog mockFlowLog = mock(FlowChainLog.class);
        FlowLogWithoutPayload flowLogWithoutPayload = mock(FlowLogWithoutPayload.class);
        when(flowLogService.getLastFlowLog(eq(FLOW_ID)))
                .thenReturn(Optional.of(flowLogWithoutPayload));
        when(flowChainLogService.findFirstByFlowChainIdOrderByCreatedDesc(any()))
                .thenReturn(Optional.of(mockFlowLog));
        when(mockFlowLog.getFlowChainType()).thenReturn(DatalakeResizeFlowEventChainFactory.class.getSimpleName());
        StartUpdateLoadBalancerDNSEvent event = new StartUpdateLoadBalancerDNSEvent(UPDATE_LOAD_BALANCER_DNS_EVENT.event(), SDX_ID, USER_CRN, new Promise<>());
        AbstractAction action = (AbstractAction) underTest.updateLoadBalancerDNSAction();
        initActionPrivateFields(action);
        AbstractActionTestSupport testSupport = new AbstractActionTestSupport(action);
        SdxContext sdxContext = SdxContext.from(flowParameters, event);
        testSupport.doExecute(sdxContext, event, new HashMap<>());
        verify(updateLoadBalancerDNSService, times(1))
                .performLoadBalancerDNSUpdate(eq(sdxCluster));
        verify(eventSenderService, times(1))
                .notifyEvent(eq(sdxCluster), eq(sdxContext), eq(ResourceEvent.UPDATE_LOAD_BALANCER_DNS_FINISHED));
        verify(eventSenderService, times(1))
                .sendEventAndNotification(eq(sdxCluster), eq(USER_CRN), eq(ResourceEvent.DATALAKE_RECOVERY_FINISHED));

        ArgumentCaptor<StartUpdateLoadBalancerDNSEvent> captor = ArgumentCaptor.forClass(StartUpdateLoadBalancerDNSEvent.class);
        verify(reactorEventFactory, times(1)).createEvent(any(), captor.capture());
        assertEquals(SDX_ID, captor.getValue().getResourceId());
        assertEquals(USER_CRN, captor.getValue().getUserId());
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
    }

    private SdxCluster genCluster() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setId(SDX_ID);
        sdxCluster.setClusterShape(SdxClusterShape.LIGHT_DUTY);
        sdxCluster.setEnvName("env");
        sdxCluster.setEnvCrn("crn");
        sdxCluster.setClusterName(DATALAKE_NAME);
        return sdxCluster;
    }

}