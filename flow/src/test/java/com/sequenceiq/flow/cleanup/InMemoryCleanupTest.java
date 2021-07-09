package com.sequenceiq.flow.cleanup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.flow.api.model.operation.OperationType;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.core.cache.FlowStatCache;
import com.sequenceiq.flow.core.chain.FlowChains;
import com.sequenceiq.flow.core.config.FlowConfiguration;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.domain.StateStatus;

@ExtendWith(MockitoExtension.class)
class InMemoryCleanupTest {

    private static final String FLOW_ID = "flowId";

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private FlowChains flowChains;

    @Mock
    private FlowStatCache flowStatCache;

    @InjectMocks
    private InMemoryCleanup underTest;

    @Test
    void cancelEveryFlowWithoutDbUpdate() {
        Set<FlowLog> flowLogs = new HashSet<>(getFlowLogs(2, 5000));

        // Mock InMemoryStateStore for check method execution success
        Set<Long> myStackIds = flowLogs.stream().map(FlowLog::getResourceId).collect(Collectors.toSet());
        for (Long myStackId : myStackIds) {
            InMemoryStateStore.putStack(myStackId, PollGroup.POLLABLE);
        }

        Set<String> flowIds = myStackIds.stream().map(i -> Long.toString(i)).collect(Collectors.toSet());
        when(runningFlows.getRunningFlowIds()).thenReturn(flowIds);

        underTest.cancelEveryFlowWithoutDbUpdate();

        // In case of exception the instance should terminate the flows which are in running state
        for (Long myStackId : myStackIds) {
            assertEquals(PollGroup.CANCELLED, InMemoryStateStore.getStack(myStackId));
        }

        flowIds.forEach(id -> verify(runningFlows, times(1)).remove(eq(id)));
    }

    @Test
    void cancelFlowWithoutDbUpdate() {
        underTest.cancelFlowWithoutDbUpdate(FLOW_ID);
        verify(runningFlows, times(1)).remove(FLOW_ID);
    }

    private List<FlowLog> getFlowLogs(int flowCount, int from) {
        List<FlowLog> flows = new ArrayList<>();
        Random random = new SecureRandom();
        int flowId = random.nextInt(5000) + from;
        long stackId = random.nextInt(5000) + from;
        for (int i = 0; i < flowCount; i++) {
            for (int j = 0; j < random.nextInt(99) + 1; j++) {
                FlowLog flowLog = new FlowLog(stackId + i, "" + flowId + i, "RUNNING",
                        false, StateStatus.PENDING, OperationType.UNKNOWN);
                flowLog.setFlowType(FlowConfiguration.class);
                flows.add(flowLog);
            }
        }
        return flows;
    }

}
