package com.sequenceiq.flow.service.flowlog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.cedarsoftware.util.io.JsonWriter;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.domain.FlowChainLog;
import com.sequenceiq.flow.repository.FlowChainLogRepository;

@RunWith(MockitoJUnitRunner.class)
public class FlowChainLogServiceTest {

    private static final String FLOWCHAIN_PARENT_SUFFIX = "Parent";

    private static final String NO_PARENT = null;

    @InjectMocks
    private FlowChainLogService underTest;

    @Mock
    private FlowChainLogRepository flowLogRepository;

    @Test
    public void testFindAllFlowChainsInTree() {
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
    public void testFindAllLatestFlowChainsInTree() {
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
    public void testCheckIfThereIsEventInQueues() {
        List<FlowChainLog> flowChains = List.of(
                flowChainLog("1", true, 1L),
                flowChainLog("1", true, 2L),
                flowChainLog("2", true, 1L),
                flowChainLog("2", false, 2L)
        );
        assertTrue(underTest.hasEventInFlowChainQueue(flowChains));
    }

    @Test
    public void testCheckIfThereIsNoEventInQueues() {
        List<FlowChainLog> flowChains = List.of(
                flowChainLog("1", true, 1L),
                flowChainLog("1", false, 2L),
                flowChainLog("2", true, 1L),
                flowChainLog("2", false, 2L)
        );
        assertFalse(underTest.hasEventInFlowChainQueue(flowChains));
    }

    @Test
    public void testCheckIfThereIsEventInQueuesBasedOnlatestChains() {
        List<FlowChainLog> flowChains = List.of(
                flowChainLog("1", false, 1L),
                flowChainLog("1", true, 2L),
                flowChainLog("2", false, 1L),
                flowChainLog("2", true, 2L)
        );
        assertTrue(underTest.hasEventInFlowChainQueue(flowChains));
    }

    private FlowChainLog flowChainLog(String flowChainId, String parentFlowChainId, long created) {
        FlowChainLog flowChainLog = new FlowChainLog();
        flowChainLog.setFlowChainId(flowChainId);
        flowChainLog.setParentFlowChainId(parentFlowChainId);
        flowChainLog.setCreated(created);
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
        flowChainLog.setChain(JsonWriter.objectToJson(flowEventChain));
        return flowChainLog;
    }

    public static class TestEvent implements Selectable {

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