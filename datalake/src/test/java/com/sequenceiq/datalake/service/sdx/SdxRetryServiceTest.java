package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.datalake.flow.create.SdxCreateEvent.SDX_STACK_CREATION_IN_PROGRESS_EVENT;
import static com.sequenceiq.datalake.flow.create.SdxCreateEvent.STORAGE_VALIDATION_WAIT_EVENT;
import static com.sequenceiq.datalake.flow.create.SdxCreateState.SDX_CREATION_WAIT_RDS_STATE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.function.Consumer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.flow.core.Flow2Handler;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.domain.StateStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("SDX retry service service tests")
public class SdxRetryServiceTest {

    @Mock
    private Flow2Handler flow2Handler;

    @Mock
    private StackV4Endpoint stackV4Endpoint;

    @InjectMocks
    private SdxRetryService sdxRetryService;

    @Test
    public void noRetryAndNoRestartFlowIfStateSuccessful() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setId(1L);
        sdxCluster.setClusterName("sdxclustername");
        FlowLog successfulFlowLog = new FlowLog();
        successfulFlowLog.setFlowId("FLOW_ID_1");
        successfulFlowLog.setStateStatus(StateStatus.SUCCESSFUL);
        successfulFlowLog.setNextEvent(STORAGE_VALIDATION_WAIT_EVENT.name());
        successfulFlowLog.setCreated(1L);
        successfulFlowLog.setCurrentState(SDX_CREATION_WAIT_RDS_STATE.name());
        doAnswer(invocation -> {
            ((Consumer<FlowLog>) invocation.getArgument(1)).accept(successfulFlowLog);
            return null;
        }).when(flow2Handler).retryLastFailedFlow(anyLong(), any());

        sdxRetryService.retrySdx(sdxCluster);

        verify(stackV4Endpoint, times(0)).retry(any(), any(), anyString());
    }

    @Test
    public void retryAndRestartFlowIfStackCreationInProgressWasTheLastFailedState() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setId(1L);
        sdxCluster.setClusterName("sdxclustername");
        sdxCluster.setAccountId("accountid");
        FlowLog successfulFlowLog = new FlowLog();
        successfulFlowLog.setFlowId("FLOW_ID_1");
        successfulFlowLog.setStateStatus(StateStatus.SUCCESSFUL);
        successfulFlowLog.setNextEvent(SDX_STACK_CREATION_IN_PROGRESS_EVENT.name());
        successfulFlowLog.setCreated(1L);
        successfulFlowLog.setCurrentState(SDX_CREATION_WAIT_RDS_STATE.name());

        doAnswer(invocation -> {
            ((Consumer<FlowLog>) invocation.getArgument(1)).accept(successfulFlowLog);
            return null;
        }).when(flow2Handler).retryLastFailedFlow(anyLong(), any());

        sdxRetryService.retrySdx(sdxCluster);

        verify(stackV4Endpoint, times(1)).retry(any(), eq("sdxclustername"), anyString());
    }

}