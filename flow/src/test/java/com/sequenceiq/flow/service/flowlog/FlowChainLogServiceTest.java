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
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import org.assertj.core.util.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.cedarsoftware.util.io.JsonWriter;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.domain.FlowChainLog;
import com.sequenceiq.flow.repository.FlowChainLogRepository;

@RunWith(MockitoJUnitRunner.class)
public class FlowChainLogServiceTest {

    private static final String FLOWCHAIN_PARENT_SUFFIX = "Parent";

    @InjectMocks
    private FlowChainLogService underTest;

    @Mock
    private FlowChainLogRepository flowLogRepository;

    @Test
    public void testGetRelatedFlowChainIds() {
        String flowChainId = "flowChainId";
        String otherFlowChainId = "otherFlowChainId";
        String childFlowChainId = "childFlowChainId";
        String parentFlowChainId = "parentFlowChainId";
        when(flowLogRepository.findByParentFlowChainIdOrderByCreatedDesc(eq(parentFlowChainId)))
                .thenReturn(Lists.newArrayList(create(flowChainId), create(otherFlowChainId)));
        when(flowLogRepository.findByParentFlowChainIdOrderByCreatedDesc(eq(flowChainId))).thenReturn(Lists.newArrayList(create(childFlowChainId)));
        when(flowLogRepository.findByParentFlowChainIdOrderByCreatedDesc(eq(otherFlowChainId))).thenReturn(Lists.newArrayList());
        when(flowLogRepository.findByParentFlowChainIdOrderByCreatedDesc(eq(childFlowChainId))).thenReturn(Lists.newArrayList());
        when(flowLogRepository.findFirstByFlowChainIdOrderByCreatedDesc(eq(flowChainId + FLOWCHAIN_PARENT_SUFFIX)))
                .thenReturn(Optional.of(create(parentFlowChainId)));
        when(flowLogRepository.findFirstByFlowChainIdOrderByCreatedDesc(eq(parentFlowChainId + FLOWCHAIN_PARENT_SUFFIX))).thenReturn(Optional.empty());

        Set<FlowChainLog> flowChains = underTest.collectRelatedFlowChains(create(flowChainId));
        List<String> flowChainIds = flowChains.stream().map(flowChainLog -> flowChainLog.getFlowChainId()).collect(Collectors.toList());

        assertEquals(4, flowChainIds.size());
        assertTrue(flowChainIds.contains(flowChainId));
        assertTrue(flowChainIds.contains(childFlowChainId));
        assertTrue(flowChainIds.contains(parentFlowChainId));

        verify(flowLogRepository, times(4)).findByParentFlowChainIdOrderByCreatedDesc(any());
        verify(flowLogRepository, times(2)).findFirstByFlowChainIdOrderByCreatedDesc(any());
    }

    @Test
    public void testCheckIfThereIsEventInQueues() {
        Set<FlowChainLog> flowChains = Sets.newHashSet(
                create("1", true, 1L),
                create("1", true, 2L),
                create("2", true, 1L),
                create("2", false, 2L)
        );
        assertTrue(underTest.checkIfAnyFlowChainHasEventInQueue(flowChains));
    }

    @Test
    public void testCheckIfThereIsNoEventInQueues() {
        Set<FlowChainLog> flowChains = Sets.newHashSet(
                create("1", true, 1L),
                create("1", false, 2L),
                create("2", true, 1L),
                create("2", false, 2L)
        );
        assertFalse(underTest.checkIfAnyFlowChainHasEventInQueue(flowChains));
    }

    @Test
    public void testCheckIfThereIsEventInQueuesBasedOnlatestChains() {
        Set<FlowChainLog> flowChains = Sets.newHashSet(
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
        flowChainLog.setParentFlowChainId(flowChanId + FLOWCHAIN_PARENT_SUFFIX);
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