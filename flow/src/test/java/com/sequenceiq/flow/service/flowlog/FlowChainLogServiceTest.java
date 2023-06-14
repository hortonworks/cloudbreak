package com.sequenceiq.flow.service.flowlog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
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

    @Test
    void testCheckIfThereIsEventInQueues() {
        List<FlowChainLog> flowChains = List.of(
                flowChainLog("1", true, 1L),
                flowChainLog("1", true, 2L),
                flowChainLog("2", true, 1L),
                flowChainLog("2", false, 2L)
        );
        assertTrue(underTest.hasEventInFlowChainQueue(flowChains));
    }

    @Test
    void testCheckIfThereIsNoEventInQueues() {
        List<FlowChainLog> flowChains = List.of(
                flowChainLog("1", true, 1L),
                flowChainLog("1", false, 2L),
                flowChainLog("2", true, 1L),
                flowChainLog("2", false, 2L)
        );
        assertFalse(underTest.hasEventInFlowChainQueue(flowChains));
    }

    @Test
    void testCheckIfThereIsEventInQueuesBasedOnlatestChains() {
        List<FlowChainLog> flowChains = List.of(
                flowChainLog("1", false, 1L),
                flowChainLog("1", true, 2L),
                flowChainLog("2", false, 1L),
                flowChainLog("2", true, 2L)
        );
        assertTrue(underTest.hasEventInFlowChainQueue(flowChains));
    }

    @Test
    void testGetRelatedFlowChainLogsWithSameCreatedAndLastFlowChainHavingEmptyQueue() {
        List<FlowChainLog> flowChainLogs = List.of(
                setupFlowChainLogWithIdWithoutParentId(1L, "1", true, 1L),
                setupFlowChainLogWithIdWithoutParentId(2L, "1", false, 1L)
        );
        Queue<Selectable> chain = underTest.getRelatedFlowChainLogs(flowChainLogs).get(0).getChainAsQueue();
        assertTrue(chain.isEmpty());
    }

    @Test
    void testGetRelatedFlowChainLogsWithSameCreatedAndLastFlowChainHavingQueue() {
        List<FlowChainLog> flowChainLogs = List.of(
                setupFlowChainLogWithIdWithoutParentId(1L, "1", false, 1L),
                setupFlowChainLogWithIdWithoutParentId(2L, "1", true, 1L)
        );
        Queue<Selectable> chain = underTest.getRelatedFlowChainLogs(flowChainLogs).get(0).getChainAsQueue();
        assertFalse(chain.isEmpty());
    }

    @Test
    void testGetRelatedFlowChainLogsWithDifferentCreatedAndLastFlowChainHavingEmptyQueue() {
        List<FlowChainLog> flowChainLogs = List.of(
                setupFlowChainLogWithIdWithoutParentId(1L, "1", true, 1L),
                setupFlowChainLogWithIdWithoutParentId(2L, "1", false, 2L)
        );
        Queue<Selectable> chain = underTest.getRelatedFlowChainLogs(flowChainLogs).get(0).getChainAsQueue();
        assertTrue(chain.isEmpty());
    }

    @Test
    void testGetRelatedFlowChainLogsWithDifferentCreatedAndLastFlowChainHavingQueue() {
        List<FlowChainLog> flowChainLogs = List.of(
                setupFlowChainLogWithIdWithoutParentId(1L, "1", false, 1L),
                setupFlowChainLogWithIdWithoutParentId(2L, "1", true, 2L)
        );
        Queue<Selectable> chain = underTest.getRelatedFlowChainLogs(flowChainLogs).get(0).getChainAsQueue();
        assertFalse(chain.isEmpty());
    }

    @Test
    void testGetRelatedFlowChainLogsWithContradictoryIdAndCreatedOrderButCreatedDeterminesFinalOrder() {
        List<FlowChainLog> flowChainLogs = List.of(
                setupFlowChainLogWithIdWithoutParentId(1L, "1", false, 4L),
                setupFlowChainLogWithIdWithoutParentId(2L, "1", true, 3L),
                setupFlowChainLogWithIdWithoutParentId(3L, "1", true, 2L),
                setupFlowChainLogWithIdWithoutParentId(4L, "1", true, 1L)
        );
        Queue<Selectable> chain = underTest.getRelatedFlowChainLogs(flowChainLogs).get(0).getChainAsQueue();
        assertTrue(chain.isEmpty());
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

    @Test
    void testFindAllByFlowChainIdInOrderByCreatedDesc() {
        FlowChainLog flowChainLog = new FlowChainLog();
        flowChainLog.setFlowChainId("FLOW_CHAIN_ID");
        when(flowLogRepository.nativeFindByFlowChainIdInOrderByCreatedDesc(anySet(), any()))
                .thenReturn(new PageImpl<>(List.of(flowChainLog)));
        Page<FlowChainLog> flowChains = underTest.findAllByFlowChainIdInOrderByCreatedDesc(Set.of("chainId"), PageRequest.of(0, 50));
        assertEquals(1, flowChains.getTotalPages());
        assertEquals(1, flowChains.getTotalElements());
        assertEquals("FLOW_CHAIN_ID", flowChains.getContent().get(0).getFlowChainId());
    }

    private FlowChainLog flowChainLog(String flowChainId, String parentFlowChainId, long created) {
        FlowChainLog flowChainLog = new FlowChainLog();
        flowChainLog.setFlowChainId(flowChainId);
        flowChainLog.setParentFlowChainId(parentFlowChainId);
        flowChainLog.setCreated(created);
        flowChainLog.setChainJackson(JsonUtil.writeValueAsStringSilent(new ConcurrentLinkedQueue<>()));
        return flowChainLog;
    }

    private FlowChainLog flowChainLogWithId(Long id, String flowChainId, String parentFlowChainId, long created) {
        FlowChainLog flowChainLog = new FlowChainLog();
        flowChainLog.setId(id);
        flowChainLog.setFlowChainId(flowChainId);
        flowChainLog.setParentFlowChainId(parentFlowChainId);
        flowChainLog.setCreated(created);
        flowChainLog.setChainJackson(JsonUtil.writeValueAsStringSilent(new ConcurrentLinkedQueue<>()));
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

    private FlowChainLog flowChainLog(String flowChanId, boolean hasEventInQueue, Long created) {
        FlowChainLog flowChainLog = flowChainLog(flowChanId, flowChanId + FLOWCHAIN_PARENT_SUFFIX, created);
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        if (hasEventInQueue) {
            flowEventChain.add(new TestEvent());
        }
        flowChainLog.setChainJackson(TypedJsonUtil.writeValueAsStringSilent(flowEventChain));
        return flowChainLog;
    }

    private FlowChainLog setupFlowChainLogWithIdWithoutParentId(Long id, String flowChanId, boolean hasEventInQueue, Long created) {
        FlowChainLog flowChainLog = flowChainLogWithId(id, flowChanId, null, created);
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        if (hasEventInQueue) {
            flowEventChain.add(new TestEvent());
        }
        flowChainLog.setChainJackson(TypedJsonUtil.writeValueAsStringSilent(flowEventChain));
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
