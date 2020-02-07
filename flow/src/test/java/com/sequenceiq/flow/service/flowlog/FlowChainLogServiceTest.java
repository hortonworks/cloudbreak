package com.sequenceiq.flow.service.flowlog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import org.assertj.core.util.Lists;
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

    @InjectMocks
    private FlowChainLogService underTest;

    @Mock
    private FlowChainLogRepository flowLogRepository;

    @Test
    public void testGetRelatedFlowChainIds() {
        String flowChainId = "flowChainId";
        String parentFlowChainId = "anotherFlowChainId";
        when(flowLogRepository.findByParentFlowChainIdOrderByCreatedDesc(eq(flowChainId))).thenReturn(Lists.newArrayList(create("anotherFlowChainId")));
        when(flowLogRepository.findByParentFlowChainIdOrderByCreatedDesc(eq(parentFlowChainId))).thenReturn(Lists.newArrayList());

        List<FlowChainLog> flowChains = underTest.collectRelatedFlowChains(Lists.newArrayList(), create(flowChainId));
        List<String> flowChainIds = flowChains.stream().map(flowChainLog -> flowChainLog.getFlowChainId()).collect(Collectors.toList());

        assertEquals(2, flowChainIds.size());
        assertTrue(flowChainIds.contains(flowChainId));
        assertTrue(flowChainIds.contains(parentFlowChainId));

        verify(flowLogRepository, times(2)).findByParentFlowChainIdOrderByCreatedDesc(any());
    }

    @Test
    public void testCheckIfThereIsEventInQueues() {
        List<FlowChainLog> flowChains = Lists.newArrayList(
                create("1", true, 1L),
                create("1", true, 2L),
                create("2", true, 1L),
                create("2", false, 2L)
        );
        assertTrue(underTest.checkIfAnyFlowChainHasEventInQueue(flowChains));
    }

    @Test
    public void testCheckIfThereIsNoEventInQueues() {
        List<FlowChainLog> flowChains = Lists.newArrayList(
                create("1", true, 1L),
                create("1", false, 2L),
                create("2", true, 1L),
                create("2", false, 2L)
        );
        assertFalse(underTest.checkIfAnyFlowChainHasEventInQueue(flowChains));
    }

    @Test
    public void testCheckIfThereIsEventInQueuesBasedOnlatestChains() {
        List<FlowChainLog> flowChains = Lists.newArrayList(
                create("1", false, 1L),
                create("1", true, 2L),
                create("2", false, 1L),
                create("2", true, 2L)
        );
        assertTrue(underTest.checkIfAnyFlowChainHasEventInQueue(flowChains));
    }

    private FlowChainLog create(String flowChanId, boolean hasEventInQueue, Long created) {
        FlowChainLog flowChainLog = create(flowChanId);
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        if (hasEventInQueue) {
            flowEventChain.add(new TestEvent());
        }
        flowChainLog.setChain(JsonWriter.objectToJson(flowEventChain));
        flowChainLog.setCreated(created);
        return flowChainLog;
    }

    private FlowChainLog create(String flowChanId) {
        FlowChainLog flowChainLog = new FlowChainLog();
        flowChainLog.setFlowChainId(flowChanId);
        flowChainLog.setParentFlowChainId(flowChanId + "parent");
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