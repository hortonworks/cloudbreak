package com.sequenceiq.datalake.flow.loadbalancer.dns;

import static com.sequenceiq.datalake.flow.loadbalancer.dns.UpdateLoadBalancerDNSEvent.UPDATE_LOAD_BALANCER_DNS_IPA_EVENT;
import static com.sequenceiq.datalake.flow.loadbalancer.dns.UpdateLoadBalancerDNSEvent.UPDATE_LOAD_BALANCER_DNS_PEM_EVENT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.action.Action;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.events.EventSenderService;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.chain.DatalakeResizeRecoveryFlowEventChainFactory;
import com.sequenceiq.datalake.flow.loadbalancer.dns.event.StartUpdateLoadBalancerDNSEvent;
import com.sequenceiq.datalake.service.loadbalancer.dns.UpdateLoadBalancerDNSService;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.domain.FlowLogWithoutPayload;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.flow.service.flowlog.FlowChainLogService;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

@ExtendWith(MockitoExtension.class)
class UpdateLoadBalancerDNSActionsTest {

    private static final Long SDX_ID = 2L;

    private static final String FLOW_ID = "flow_id";

    private static final String DATALAKE_NAME = "test_dl";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    @Mock
    private SdxService sdxService;

    @Mock
    private UpdateLoadBalancerDNSService updateLoadBalancerDNSService;

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
    void updateLoadBalancerDNSActionResizeRecovery() throws Exception {
        SdxCluster sdxCluster = genCluster();
        FlowParameters flowParameters = new FlowParameters(FLOW_ID, null);
        when(sdxService.getById(SDX_ID)).thenReturn(sdxCluster);
        FlowLogWithoutPayload flowLogWithoutPayload = mock(FlowLogWithoutPayload.class);
        when(flowLogService.getLastFlowLog(FLOW_ID))
                .thenReturn(Optional.of(flowLogWithoutPayload));
        when(flowChainLogService.isFlowTriggeredByFlowChain(eq(DatalakeResizeRecoveryFlowEventChainFactory.class.getSimpleName()), any()))
                .thenReturn(true);
        StartUpdateLoadBalancerDNSEvent event = new StartUpdateLoadBalancerDNSEvent(UPDATE_LOAD_BALANCER_DNS_PEM_EVENT.event(),
                SDX_ID, DATALAKE_NAME, USER_CRN);
        AbstractAction action = (AbstractAction) underTest.updateLoadBalancerDNSPEMAction();
        initActionPrivateFields(action);
        AbstractActionTestSupport testSupport = new AbstractActionTestSupport(action);
        SdxContext sdxContext = SdxContext.from(flowParameters, event);
        testSupport.doExecute(sdxContext, event, new HashMap<>());
        verify(updateLoadBalancerDNSService, times(1))
                .performLoadBalancerDNSUpdateOnPEM(sdxCluster);
        verify(eventSenderService, times(1))
                .notifyEvent(sdxCluster, sdxContext, ResourceEvent.UPDATE_LOAD_BALANCER_DNS_PEM_FINISHED);

        action = (AbstractAction) underTest.updateLoadBalancerDNSIPAAction();
        initActionPrivateFields(action);
        testSupport = new AbstractActionTestSupport(action);
        sdxContext = SdxContext.from(flowParameters, event);
        testSupport.doExecute(sdxContext, event, new HashMap<>());
        verify(updateLoadBalancerDNSService, times(1))
                .performLoadBalancerDNSUpdateOnIPA(sdxCluster);
        verify(eventSenderService, times(1))
                .notifyEvent(sdxCluster, sdxContext, ResourceEvent.UPDATE_LOAD_BALANCER_DNS_IPA_FINISHED);
        verify(eventSenderService, times(1))
                .sendEventAndNotification(sdxCluster, ResourceEvent.DATALAKE_RECOVERY_FINISHED);
    }

    @Test
    void updateLoadBalancerDNSActionResize() throws Exception {
        SdxCluster sdxCluster = genCluster();
        FlowParameters flowParameters = new FlowParameters(FLOW_ID, null);
        when(sdxService.getById(SDX_ID)).thenReturn(sdxCluster);
        FlowLogWithoutPayload flowLogWithoutPayload = mock(FlowLogWithoutPayload.class);
        when(flowLogService.getLastFlowLog(FLOW_ID))
                .thenReturn(Optional.of(flowLogWithoutPayload));
        when(flowChainLogService.isFlowTriggeredByFlowChain(eq(DatalakeResizeRecoveryFlowEventChainFactory.class.getSimpleName()), any()))
                .thenReturn(false);
        StartUpdateLoadBalancerDNSEvent event = new StartUpdateLoadBalancerDNSEvent(UPDATE_LOAD_BALANCER_DNS_IPA_EVENT.event(),
                SDX_ID, DATALAKE_NAME, USER_CRN);
        AbstractAction action = (AbstractAction) underTest.updateLoadBalancerDNSIPAAction();
        initActionPrivateFields(action);
        AbstractActionTestSupport testSupport = new AbstractActionTestSupport(action);
        SdxContext sdxContext = SdxContext.from(flowParameters, event);
        testSupport.doExecute(sdxContext, event, new HashMap<>());
        verify(updateLoadBalancerDNSService, times(1))
                .performLoadBalancerDNSUpdateOnIPA(sdxCluster);
        verify(eventSenderService, times(1))
                .notifyEvent(sdxCluster, sdxContext, ResourceEvent.UPDATE_LOAD_BALANCER_DNS_IPA_FINISHED);
        verify(eventSenderService, times(0))
                .sendEventAndNotification(sdxCluster, ResourceEvent.DATALAKE_RECOVERY_FINISHED);
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