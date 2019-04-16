package com.sequenceiq.cloudbreak.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import javax.ws.rs.BadRequestException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.core.flow2.Flow2Handler;
import com.sequenceiq.cloudbreak.core.flow2.FlowLogService;
import com.sequenceiq.cloudbreak.domain.FlowLog;
import com.sequenceiq.cloudbreak.domain.StateStatus;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;

@RunWith(MockitoJUnitRunner.class)
public class OperationRetryServiceTest {

    private static final Long STACK_ID = 1L;

    private static final String FLOW_ID = "flowId";

    private static final LocalDateTime BASE_DATE_TIME = LocalDateTime.of(2018, 1, 1, 0, 1);

    @InjectMocks
    private OperationRetryService underTest;

    @Mock
    private Flow2Handler flow2Handler;

    @Mock
    private FlowLogService flowLogService;

    @Mock
    private Stack stackMock;

    @Mock
    private Cluster clusterMock;

    @Test(expected = BadRequestException.class)
    public void retryPending() {
        when(stackMock.getId()).thenReturn(STACK_ID);

        List<FlowLog> pendingFlowLogs = Lists.newArrayList(
                createFlowLog("INIT_STATE", StateStatus.SUCCESSFUL, Instant.now().toEpochMilli()),
                createFlowLog("START_STATE", StateStatus.PENDING, Instant.now().toEpochMilli())
                );
        when(flowLogService.findAllByStackIdOrderByCreatedDesc(STACK_ID)).thenReturn(pendingFlowLogs);
        try {
            underTest.retry(stackMock);
        } finally {
            verify(flow2Handler, times(0)).restartFlow(any(FlowLog.class));
        }
    }

    private FlowLog createFlowLog(String currentState, StateStatus stateStatus, long created) {
        FlowLog flowLog = new FlowLog(STACK_ID, FLOW_ID, currentState, true, stateStatus);
        flowLog.setCreated(created);
        return flowLog;
    }

    @Test(expected = BadRequestException.class)
    public void retrySuccessful() {
        when(stackMock.getId()).thenReturn(STACK_ID);
        when(stackMock.getStatus()).thenReturn(Status.AVAILABLE);
        Cluster cluster = new Cluster();
        cluster.setStatus(Status.AVAILABLE);
        when(stackMock.getCluster()).thenReturn(cluster);

        List<FlowLog> pendingFlowLogs = Lists.newArrayList(
                createFlowLog("INIT_STATE", StateStatus.SUCCESSFUL, Instant.now().toEpochMilli()),
                createFlowLog("START_STATE", StateStatus.SUCCESSFUL, Instant.now().toEpochMilli())
        );
        when(flowLogService.findAllByStackIdOrderByCreatedDesc(STACK_ID)).thenReturn(pendingFlowLogs);
        try {
            underTest.retry(stackMock);
        } finally {
            verify(flow2Handler, times(0)).restartFlow(any(FlowLog.class));
        }
    }

    @Test
    public void retry() {
        when(stackMock.getId()).thenReturn(STACK_ID);

        FlowLog lastSuccessfulState = createFlowLog("INTERMEDIATE_STATE", StateStatus.SUCCESSFUL, getOffsettedCreated(2));
        List<FlowLog> pendingFlowLogs = Lists.newArrayList(
                createFlowLog("FINISHED", StateStatus.SUCCESSFUL, getOffsettedCreated(6)),
                createFlowLog("NEXT_STATE", StateStatus.FAILED, getOffsettedCreated(5)),
                createFlowLog("FINISHED", StateStatus.SUCCESSFUL, getOffsettedCreated(4)),
                createFlowLog("NEXT_STATE", StateStatus.FAILED, getOffsettedCreated(3)),
                lastSuccessfulState,
                createFlowLog("INIT_STATE", StateStatus.SUCCESSFUL, getOffsettedCreated(1))
                );
        when(flowLogService.findAllByStackIdOrderByCreatedDesc(STACK_ID)).thenReturn(pendingFlowLogs);
        when(stackMock.getStatus()).thenReturn(Status.CREATE_FAILED);
        underTest.retry(stackMock);

        verify(flow2Handler, times(1)).restartFlow(ArgumentMatchers.eq(lastSuccessfulState));
    }

    @Test
    public void retryCluster() {
        when(stackMock.getId()).thenReturn(STACK_ID);

        FlowLog lastSuccessfulState = createFlowLog("INTERMEDIATE_STATE", StateStatus.SUCCESSFUL, getOffsettedCreated(2));
        List<FlowLog> pendingFlowLogs = Lists.newArrayList(
                createFlowLog("FINISHED", StateStatus.SUCCESSFUL, getOffsettedCreated(6)),
                createFlowLog("NEXT_STATE", StateStatus.FAILED, getOffsettedCreated(5)),
                createFlowLog("FINISHED", StateStatus.SUCCESSFUL, getOffsettedCreated(4)),
                createFlowLog("NEXT_STATE", StateStatus.FAILED, getOffsettedCreated(3)),
                lastSuccessfulState,
                createFlowLog("INIT_STATE", StateStatus.SUCCESSFUL, getOffsettedCreated(1))
                );
        when(flowLogService.findAllByStackIdOrderByCreatedDesc(STACK_ID)).thenReturn(pendingFlowLogs);
        when(stackMock.getStatus()).thenReturn(Status.AVAILABLE);
        when(stackMock.getCluster()).thenReturn(clusterMock);
        when(clusterMock.getStatus()).thenReturn(Status.CREATE_FAILED);
        underTest.retry(stackMock);

        verify(flow2Handler, times(1)).restartFlow(ArgumentMatchers.eq(lastSuccessfulState));
    }

    private long getOffsettedCreated(int minutes) {
        return BASE_DATE_TIME.plusMinutes(minutes).toEpochSecond(ZoneOffset.UTC);
    }
}