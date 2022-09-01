package com.sequenceiq.flow.service.flowlog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cedarsoftware.util.io.JsonWriter;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.json.TypedJsonUtil;
import com.sequenceiq.flow.domain.FlowChainLog;
import com.sequenceiq.flow.domain.FlowLogWithoutPayload;
import com.sequenceiq.flow.repository.FlowChainLogRepository;

@ExtendWith(MockitoExtension.class)
class FlowChainLogServiceTest {

    private static final String FLOWCHAIN_PARENT_SUFFIX = "Parent";

    private static final String NO_PARENT = null;

    @InjectMocks
    private FlowChainLogService underTest;

    @Mock
    private FlowChainLogRepository flowLogRepository;

    @Test
    void testFindAllFlowChainsInTree() {
        FlowChainLog flowChainLogA = flowChainLog("A", NO_PARENT, 1);
        FlowChainLog flowChainLogB = flowChainLog("B", flowChainLogA.getFlowChainId(), 2);
        FlowChainLog flowChainLogC = flowChainLog("C", flowChainLogA.getFlowChainId(), 3);
        FlowChainLog flowChainLogD = flowChainLog("D", flowChainLogC.getFlowChainId(), 4);
        FlowChainLog flowChainLogE = flowChainLog("E", flowChainLogC.getFlowChainId(), 5);

        setUpParent(flowChainLogB, flowChainLogA);
        setUpChildren(flowChainLogA, List.of(flowChainLogC, flowChainLogB));
        setUpChildren(flowChainLogC, List.of(flowChainLogE, flowChainLogD));

        List<FlowChainLog> result = underTest.collectRelatedFlowChains(flowChainLogB);

        assertEquals(List.of(flowChainLogA, flowChainLogB, flowChainLogC, flowChainLogD, flowChainLogE), result);
    }

    @Test
    void testFindAllLatestFlowChainsInTree() {
        FlowChainLog flowChainLogA = flowChainLog("A", NO_PARENT, 1);
        FlowChainLog flowChainLogB = flowChainLog("B", flowChainLogA.getFlowChainId(), 2);
        FlowChainLog flowChainLogC = flowChainLog("C", flowChainLogA.getFlowChainId(), 3);
        FlowChainLog flowChainLogD = flowChainLog("D", flowChainLogC.getFlowChainId(), 4);
        FlowChainLog flowChainLogES1 = flowChainLog("E", flowChainLogC.getFlowChainId(), 5);
        FlowChainLog flowChainLogES2 = flowChainLog("E", flowChainLogC.getFlowChainId(), 6);

        setUpParent(flowChainLogB, flowChainLogA);
        setUpChildren(flowChainLogA, List.of(flowChainLogC, flowChainLogB));
        setUpChildren(flowChainLogC, List.of(flowChainLogES2, flowChainLogES1, flowChainLogD));

        List<FlowChainLog> result = underTest.collectRelatedFlowChains(flowChainLogB);

        assertEquals(List.of(flowChainLogA, flowChainLogB, flowChainLogC, flowChainLogD, flowChainLogES2), result);
    }

    @ParameterizedTest(name = "use Jackson = {0}")
    @ValueSource(booleans = {false, true})
    void testCheckIfThereIsEventInQueues(boolean useJackson) {
        List<FlowChainLog> flowChains = List.of(
                flowChainLog("1", true, 1L, useJackson),
                flowChainLog("1", true, 2L, useJackson),
                flowChainLog("2", true, 1L, useJackson),
                flowChainLog("2", false, 2L, useJackson)
        );
        assertTrue(underTest.hasEventInFlowChainQueue(flowChains));
    }

    @ParameterizedTest(name = "use Jackson = {0}")
    @ValueSource(booleans = {false, true})
    void testCheckIfThereIsNoEventInQueues(boolean useJackson) {
        List<FlowChainLog> flowChains = List.of(
                flowChainLog("1", true, 1L, useJackson),
                flowChainLog("1", false, 2L, useJackson),
                flowChainLog("2", true, 1L, useJackson),
                flowChainLog("2", false, 2L, useJackson)
        );
        assertFalse(underTest.hasEventInFlowChainQueue(flowChains));
    }

    @ParameterizedTest(name = "use Jackson = {0}")
    @ValueSource(booleans = {false, true})
    void testCheckIfThereIsEventInQueuesBasedOnlatestChains(boolean useJackson) {
        List<FlowChainLog> flowChains = List.of(
                flowChainLog("1", false, 1L, useJackson),
                flowChainLog("1", true, 2L, useJackson),
                flowChainLog("2", false, 1L, useJackson),
                flowChainLog("2", true, 2L, useJackson)
        );
        assertTrue(underTest.hasEventInFlowChainQueue(flowChains));
    }

    @Test
    void testFlowTypeEmpty() {
        String flowChainType = underTest.getFlowChainType(null);
        assertNull(flowChainType, "For null input the flowChainType must be null");
    }

    @Test
    void testFlowChainLogWhichIsEmpty() {
        // This is mainly for flows launched before 2.39
        FlowChainLog flowChainLog = new FlowChainLog();
        when(flowLogRepository.findFirstByFlowChainIdOrderByCreatedDesc(any()))
                .thenReturn(Optional.of(flowChainLog));
        String flowChainType = underTest.getFlowChainType("chainId");
        assertNull(flowChainType, "For empty flowChainLog the reurn must be null");
    }

    @Test
    void testFlowChainLogWithFlowChainType() {
        // This is mainly for flows launched before 2.39
        FlowChainLog flowChainLog = new FlowChainLog();
        flowChainLog.setFlowChainType("flowChainType");
        when(flowLogRepository.findFirstByFlowChainIdOrderByCreatedDesc(any()))
                .thenReturn(Optional.of(flowChainLog));
        String flowChainType = underTest.getFlowChainType("chainId");
        assertEquals("flowChainType", flowChainType);
    }

    @Test
    void testIsFlowTriggeredByFlowChain() {
        FlowLogWithoutPayload flowLog = mock(FlowLogWithoutPayload.class);
        when(flowLog.getFlowChainId()).thenReturn("chainId");
        FlowChainLog flowChainLog = new FlowChainLog();
        flowChainLog.setFlowChainType("flowChainType");
        when(flowLogRepository.findFirstByFlowChainIdOrderByCreatedDesc("chainId"))
                .thenReturn(Optional.of(flowChainLog));
        assertTrue(underTest.isFlowTriggeredByFlowChain("flowChainType", Optional.of(flowLog)));

        assertFalse(underTest.isFlowTriggeredByFlowChain("flowChainType1", Optional.of(flowLog)));
    }

    private FlowChainLog flowChainLog(String flowChainId, String parentFlowChainId, long created) {
        FlowChainLog flowChainLog = new FlowChainLog();
        flowChainLog.setFlowChainId(flowChainId);
        flowChainLog.setParentFlowChainId(parentFlowChainId);
        flowChainLog.setCreated(created);
        flowChainLog.setChain(JsonWriter.objectToJson(new ConcurrentLinkedQueue<>()));
        return flowChainLog;
    }

    private void setUpParent(FlowChainLog child, FlowChainLog parent) {
        when(flowLogRepository.findFirstByFlowChainIdOrderByCreatedDesc(child.getParentFlowChainId()))
                .thenReturn(Optional.of(parent));
    }

    private void setUpChildren(FlowChainLog parent, List<FlowChainLog> children) {
        when(flowLogRepository.findByParentFlowChainIdOrderByCreatedDesc(parent.getFlowChainId()))
                .thenReturn(children);
    }

    private FlowChainLog flowChainLog(String flowChanId, boolean hasEventInQueue, Long created, boolean useJackson) {
        FlowChainLog flowChainLog = flowChainLog(flowChanId, flowChanId + FLOWCHAIN_PARENT_SUFFIX, created);
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        if (hasEventInQueue) {
            flowEventChain.add(new TestEvent());
        }
        flowChainLog.setChain(JsonWriter.objectToJson(flowEventChain));
        if (useJackson) {
            flowChainLog.setChainJackson(TypedJsonUtil.writeValueAsStringSilent(flowEventChain));
        }
        return flowChainLog;
    }

    static class TestEvent implements Selectable {

        @Override
        public String selector() {
            return "test";
        }

        @Override
        public Long getResourceId() {
            return 1L;
        }
    }
}
