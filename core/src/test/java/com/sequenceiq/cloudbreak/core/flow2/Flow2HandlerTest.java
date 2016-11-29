package com.sequenceiq.cloudbreak.core.flow2;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.cloud.event.Payload;
import com.sequenceiq.cloudbreak.core.flow2.chain.FlowChains;
import com.sequenceiq.cloudbreak.core.flow2.config.FlowConfiguration;
import com.sequenceiq.cloudbreak.repository.FlowLogRepository;
import com.sequenceiq.cloudbreak.service.flowlog.FlowLogService;

import reactor.bus.Event;

public class Flow2HandlerTest {

    public static final String FLOW_ID = "flowId";

    public static final String FLOW_CHAIN_ID = "flowChainId";

    @InjectMocks
    private Flow2Handler underTest;

    @Mock
    private FlowLogService flowLogService;

    @Mock
    private FlowLogRepository flowLogRepository;

    @Mock
    private Map<String, FlowConfiguration<?>> flowConfigurationMap;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private FlowConfiguration flowConfig;

    @Mock
    private FlowChains flowChains;

    @Mock
    private FlowTriggerCondition flowTriggerCondition;

    @Mock
    private Flow flow;

    private FlowState flowState;

    private Event<? extends Payload> dummyEvent;

    private Payload payload = () -> 1L;

    @Before
    public void setUp() {
        underTest = new Flow2Handler();
        MockitoAnnotations.initMocks(this);
        Map<String, Object> headers = new HashMap<>();
        headers.put("FLOW_ID", FLOW_ID);
        dummyEvent = new Event<>(new Event.Headers(headers), payload);
        flowState = new OwnFlowState();
    }

    @Test
    public void testNewFlow() {
        BDDMockito.<FlowConfiguration>given(flowConfigurationMap.get(any())).willReturn(flowConfig);
        given(flowConfig.createFlow(anyString())).willReturn(flow);
        given(flowConfig.getFlowTriggerCondition()).willReturn(flowTriggerCondition);
        given(flowTriggerCondition.isFlowTriggerable(anyLong())).willReturn(true);
        given(flow.getCurrentState()).willReturn(flowState);
        Event<Payload> event = new Event<>(payload);
        event.setKey("KEY");
        underTest.accept(event);
        verify(flowConfigurationMap, times(1)).get(anyString());
        verify(runningFlows, times(1)).put(eq(flow), isNull(String.class));
        verify(flowLogService, times(1)).save(anyString(), anyString(), eq("KEY"), any(Payload.class), eq(flowConfig.getClass()), eq(flowState));
        verify(flow, times(1)).sendEvent(anyString(), any());
    }

    @Test
    public void testNewFlowButNotHandled() {
        Event<Payload> event = new Event<>(payload);
        event.setKey("KEY");
        underTest.accept(event);
        verify(flowConfigurationMap, times(1)).get(anyString());
        verify(runningFlows, times(0)).put(any(Flow.class), isNull(String.class));
        verify(flowLogService, times(0)).save(anyString(), anyString(), anyString(), any(Payload.class), Matchers.<Class>any(), any(FlowState.class));
    }

    @Test
    public void testExistingFlow() {
        BDDMockito.<FlowConfiguration>given(flowConfigurationMap.get(any())).willReturn(flowConfig);
        given(runningFlows.get(anyString())).willReturn(flow);
        given(flow.getCurrentState()).willReturn(flowState);
        dummyEvent.setKey("KEY");
        underTest.accept(dummyEvent);
        verify(flowLogService, times(1)).save(eq(FLOW_ID), anyString(), eq("KEY"), any(Payload.class), any(Class.class), eq(flowState));
        verify(flow, times(1)).sendEvent(eq("KEY"), any());
    }

    @Test
    public void testExistingFlowNotFound() {
        BDDMockito.<FlowConfiguration>given(flowConfigurationMap.get(any())).willReturn(flowConfig);
        dummyEvent.setKey("KEY");
        underTest.accept(dummyEvent);
        verify(flowLogService, times(0)).save(anyString(), anyString(), anyString(), any(Payload.class), Matchers.<Class>any(), any(FlowState.class));
        verify(flow, times(0)).sendEvent(anyString(), any());
    }

    @Test
    public void testFlowFinalFlowNotChained() {
        given(runningFlows.remove(FLOW_ID)).willReturn(flow);
        dummyEvent.setKey(Flow2Handler.FLOW_FINAL);
        underTest.accept(dummyEvent);
        verify(flowLogService, times(1)).close(anyLong(), eq(FLOW_ID));
        verify(runningFlows, times(1)).remove(eq(FLOW_ID));
        verify(runningFlows, times(0)).get(eq(FLOW_ID));
        verify(runningFlows, times(0)).put(any(Flow.class), isNull(String.class));
        verify(flowChains, times(0)).removeFlowChain(anyString());
        verify(flowChains, times(0)).triggerNextFlow(anyString());
    }

    @Test
    public void testFlowFinalFlowChained() {
        given(runningFlows.remove(FLOW_ID)).willReturn(flow);
        dummyEvent.setKey(Flow2Handler.FLOW_FINAL);
        dummyEvent.getHeaders().set("FLOW_CHAIN_ID", FLOW_CHAIN_ID);
        underTest.accept(dummyEvent);
        verify(flowLogService, times(1)).close(anyLong(), eq(FLOW_ID));
        verify(runningFlows, times(1)).remove(eq(FLOW_ID));
        verify(runningFlows, times(0)).get(eq(FLOW_ID));
        verify(runningFlows, times(0)).put(any(Flow.class), isNull(String.class));
        verify(flowChains, times(0)).removeFlowChain(anyString());
        verify(flowChains, times(1)).triggerNextFlow(eq(FLOW_CHAIN_ID));
    }

    @Test
    public void testFlowFinalFlowFailedNoChain() {
        given(flow.isFlowFailed()).willReturn(Boolean.TRUE);
        given(runningFlows.remove(FLOW_ID)).willReturn(flow);
        dummyEvent.setKey(Flow2Handler.FLOW_FINAL);
        given(runningFlows.remove(anyString())).willReturn(flow);
        underTest.accept(dummyEvent);
        verify(flowLogService, times(1)).close(anyLong(), eq(FLOW_ID));
        verify(runningFlows, times(1)).remove(eq(FLOW_ID));
        verify(runningFlows, times(0)).get(eq(FLOW_ID));
        verify(runningFlows, times(0)).put(any(Flow.class), isNull(String.class));
        verify(flowChains, times(0)).removeFullFlowChain(anyString());
        verify(flowChains, times(0)).triggerNextFlow(anyString());
    }

    @Test
    public void testFlowFinalFlowFailedWithChain() {
        given(flow.isFlowFailed()).willReturn(Boolean.TRUE);
        given(runningFlows.remove(FLOW_ID)).willReturn(flow);
        dummyEvent.setKey(Flow2Handler.FLOW_FINAL);
        dummyEvent.getHeaders().set("FLOW_CHAIN_ID", "FLOW_CHAIN_ID");
        given(runningFlows.remove(anyString())).willReturn(flow);
        underTest.accept(dummyEvent);
        verify(flowLogService, times(1)).close(anyLong(), eq(FLOW_ID));
        verify(runningFlows, times(1)).remove(eq(FLOW_ID));
        verify(runningFlows, times(0)).get(eq(FLOW_ID));
        verify(runningFlows, times(0)).put(any(Flow.class), isNull(String.class));
        verify(flowChains, times(1)).removeFullFlowChain(anyString());
        verify(flowChains, times(0)).triggerNextFlow(anyString());
    }

    @Test
    public void testCancelRunningFlows() {
        given(flowLogRepository.findAllRunningNonTerminationFlowIdsByStackId(anyLong())).willReturn(Collections.singleton(FLOW_ID));
        given(runningFlows.remove(FLOW_ID)).willReturn(flow);
        given(runningFlows.getFlowChainId(eq(FLOW_ID))).willReturn(FLOW_CHAIN_ID);
        dummyEvent.setKey(Flow2Handler.FLOW_CANCEL);
        underTest.accept(dummyEvent);
        verify(flowLogService, times(1)).cancel(anyLong(), eq(FLOW_ID));
        verify(flowChains, times(1)).removeFullFlowChain(eq(FLOW_CHAIN_ID));
    }

    private static class OwnFlowState implements FlowState {
        @Override
        public Class<? extends AbstractAction> action() {
            return null;
        }

        @Override
        public String name() {
            return null;
        }
    }
}
