package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.datalake.flow.create.SdxCreateEvent.RDS_WAIT_EVENT;
import static com.sequenceiq.datalake.flow.create.SdxCreateEvent.SDX_STACK_CREATION_IN_PROGRESS_EVENT;
import static com.sequenceiq.datalake.flow.create.SdxCreateState.SDX_CREATION_FAILED_STATE;
import static com.sequenceiq.datalake.flow.create.SdxCreateState.SDX_CREATION_WAIT_RDS_STATE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.datalake.controller.exception.BadRequestException;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.flow.core.Flow2Handler;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.domain.StateStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("SDX retry service service tests")
public class SdxRetryServiceTest {

    @Mock
    private Flow2Handler flow2Handler;

    @Mock
    private FlowLogService flowLogService;

    @Mock
    private StackV4Endpoint stackV4Endpoint;

    @InjectMocks
    private SdxRetryService sdxRetryService;

    @Test
    public void badRequestExceptionIfPendingFlow() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setId(1L);
        sdxCluster.setClusterName("sdxclustername");
        List<FlowLog> flowLogs = new ArrayList<>();
        FlowLog pendingFlowLog = new FlowLog();
        pendingFlowLog.setStateStatus(StateStatus.PENDING);
        flowLogs.add(pendingFlowLog);
        when(flowLogService.findAllByResourceIdOrderByCreatedDesc(1L)).thenReturn(flowLogs);
        Assertions.assertThrows(BadRequestException.class,
                () -> sdxRetryService.retrySdx(sdxCluster),
                "Retry cannot be performed, because there is already an active flow.");
    }

    @Test
    public void noRetryAndNoRestartFlowIfStateSuccessful() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setId(1L);
        sdxCluster.setClusterName("sdxclustername");
        List<FlowLog> flowLogs = new ArrayList<>();
        FlowLog successfulFlowLog = new FlowLog();
        successfulFlowLog.setStateStatus(StateStatus.SUCCESSFUL);
        flowLogs.add(successfulFlowLog);
        when(flowLogService.findAllByResourceIdOrderByCreatedDesc(1L)).thenReturn(flowLogs);
        Assertions.assertThrows(BadRequestException.class,
                () -> sdxRetryService.retrySdx(sdxCluster),
                "Retry cannot be performed, because the last action was successful");
        verify(stackV4Endpoint, times(0)).retry(any(), any());
        verify(flow2Handler, times(0)).restartFlow(anyString());
    }

    @Test
    public void retryAndRestartFlowIfStackCreationInProgressWasTheLastFailedState() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setId(1L);
        sdxCluster.setClusterName("sdxclustername");
        List<FlowLog> flowLogs = new LinkedList<>();
        FlowLog successfulFlowLog = new FlowLog();
        successfulFlowLog.setStateStatus(StateStatus.SUCCESSFUL);
        successfulFlowLog.setNextEvent(SDX_STACK_CREATION_IN_PROGRESS_EVENT.name());
        successfulFlowLog.setCreated(1L);
        successfulFlowLog.setCurrentState(SDX_CREATION_WAIT_RDS_STATE.name());
        flowLogs.add(successfulFlowLog);
        FlowLog failedFlowLog = new FlowLog();
        failedFlowLog.setStateStatus(StateStatus.FAILED);
        failedFlowLog.setCurrentState(SDX_CREATION_FAILED_STATE.name());
        failedFlowLog.setCreated(2L);
        flowLogs.add(failedFlowLog);

        when(flowLogService.findAllByResourceIdOrderByCreatedDesc(1L)).thenReturn(flowLogs);
        sdxRetryService.retrySdx(sdxCluster);
        verify(stackV4Endpoint, times(1)).retry(any(), eq("sdxclustername"));
        verify(flow2Handler, times(1)).restartFlow(any(FlowLog.class));
    }

    @Test
    public void noRetryButRestartFlow() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setId(1L);
        sdxCluster.setClusterName("sdxclustername");
        List<FlowLog> flowLogs = new LinkedList<>();
        FlowLog successfulFlowLog = new FlowLog();
        successfulFlowLog.setStateStatus(StateStatus.SUCCESSFUL);
        successfulFlowLog.setNextEvent(RDS_WAIT_EVENT.name());
        successfulFlowLog.setCreated(1L);
        successfulFlowLog.setCurrentState(SDX_CREATION_WAIT_RDS_STATE.name());
        flowLogs.add(successfulFlowLog);
        FlowLog failedFlowLog = new FlowLog();
        failedFlowLog.setStateStatus(StateStatus.FAILED);
        failedFlowLog.setCurrentState(SDX_CREATION_FAILED_STATE.name());
        failedFlowLog.setCreated(2L);
        flowLogs.add(failedFlowLog);

        when(flowLogService.findAllByResourceIdOrderByCreatedDesc(1L)).thenReturn(flowLogs);
        sdxRetryService.retrySdx(sdxCluster);
        verify(stackV4Endpoint, times(0)).retry(any(), any());
        verify(flow2Handler, times(1)).restartFlow(any(FlowLog.class));
    }

}