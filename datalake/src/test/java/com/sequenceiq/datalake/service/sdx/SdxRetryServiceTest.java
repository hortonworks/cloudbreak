package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.datalake.flow.create.SdxCreateEvent.SDX_STACK_CREATION_IN_PROGRESS_EVENT;
import static com.sequenceiq.datalake.flow.create.SdxCreateEvent.SDX_VALIDATION_EVENT;
import static com.sequenceiq.datalake.flow.create.SdxCreateState.INIT_STATE;
import static com.sequenceiq.datalake.flow.create.SdxCreateState.SDX_CREATION_WAIT_RDS_STATE;
import static com.sequenceiq.datalake.flow.dr.backup.DatalakeBackupEvent.DATALAKE_TRIGGER_BACKUP_EVENT;
import static com.sequenceiq.datalake.flow.dr.restore.DatalakeRestoreEvent.DATALAKE_TRIGGER_RESTORE_EVENT;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.InternalServerErrorException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.create.SdxCreateFlowConfig;
import com.sequenceiq.datalake.flow.dr.restore.DatalakeRestoreFlowConfig;
import com.sequenceiq.datalake.service.sdx.flowcheck.CloudbreakFlowService;
import com.sequenceiq.datalake.service.sdx.flowcheck.FlowState;
import com.sequenceiq.flow.core.Flow2Handler;
import com.sequenceiq.flow.domain.ClassValue;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.domain.StateStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("Data Lake retry service service tests")
class SdxRetryServiceTest {

    @Mock
    private Flow2Handler flow2Handler;

    @Mock
    private StackV4Endpoint stackV4Endpoint;

    @Mock
    private CloudbreakFlowService cloudbreakFlowService;

    @Mock
    private WebApplicationExceptionMessageExtractor exceptionMessageExtractor;

    @Spy
    private List<SdxCreateFlowConfig> retryableDatalakeFlowConfigurations = new ArrayList<>();

    @InjectMocks
    private SdxRetryService underTest;

    @BeforeEach
    void setUp() {
        retryableDatalakeFlowConfigurations.add(new SdxCreateFlowConfig());
        underTest.init();
    }

    @Test
    void noRetryAndNoRestartFlowIfStateSuccessful() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setId(1L);
        sdxCluster.setClusterName("sdxclustername");
        FlowLog successfulFlowLog = new FlowLog();
        successfulFlowLog.setFlowId("FLOW_ID_1");
        successfulFlowLog.setStateStatus(StateStatus.SUCCESSFUL);
        successfulFlowLog.setNextEvent(SDX_VALIDATION_EVENT.name());
        successfulFlowLog.setCreated(1L);
        successfulFlowLog.setCurrentState(SDX_CREATION_WAIT_RDS_STATE.name());
        successfulFlowLog.setFlowType(ClassValue.of(SdxCreateFlowConfig.class));
        doAnswer(invocation -> {
            ((Consumer<FlowLog>) invocation.getArgument(1)).accept(successfulFlowLog);
            return null;
        }).when(flow2Handler).retryLastFailedFlow(anyLong(), any());
        when(flow2Handler.getFirstRetryableStateLogfromLatestFlow(anyLong())).thenReturn(successfulFlowLog);

        underTest.retrySdx(sdxCluster);

        verify(stackV4Endpoint, times(0)).retry(any(), any(), anyString());
    }

    @Test
    void noRetryForEmptyFlowLog() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setId(1L);
        sdxCluster.setClusterName("sdxclustername");
        when(flow2Handler.getFirstRetryableStateLogfromLatestFlow(anyLong())).thenThrow(new InternalServerErrorException());

        assertThrows(InternalServerErrorException.class, () -> underTest.retrySdx(sdxCluster));

        verify(stackV4Endpoint, times(0)).retry(any(), any(), anyString());
    }

    @Test
    void retryAndRestartFlowIfStackCreationInProgressWasTheLastFailedState() {
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
        successfulFlowLog.setFlowType(ClassValue.of(SdxCreateFlowConfig.class));
        doAnswer(invocation -> {
            ((Consumer<FlowLog>) invocation.getArgument(1)).accept(successfulFlowLog);
            return null;
        }).when(flow2Handler).retryLastFailedFlow(anyLong(), any());
        when(flow2Handler.getFirstRetryableStateLogfromLatestFlow(anyLong())).thenReturn(successfulFlowLog);
        underTest.retrySdx(sdxCluster);

        verify(stackV4Endpoint, times(1)).retry(any(), eq("sdxclustername"), anyString());
    }

    @Test
    void retryOnBackupRestoreFailure() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setId(1L);
        sdxCluster.setClusterName("sdxclustername");
        FlowLog successfulFlowLog = new FlowLog();
        successfulFlowLog.setFlowId("FLOW_ID_1");
        successfulFlowLog.setStateStatus(StateStatus.SUCCESSFUL);
        successfulFlowLog.setNextEvent(DATALAKE_TRIGGER_RESTORE_EVENT.name());
        successfulFlowLog.setCreated(1L);
        successfulFlowLog.setCurrentState(INIT_STATE.name());
        successfulFlowLog.setFlowType(ClassValue.of(DatalakeRestoreFlowConfig.class));
        when(flow2Handler.getFirstRetryableStateLogfromLatestFlow(anyLong())).thenReturn(successfulFlowLog);

        underTest.retrySdx(sdxCluster);

        verify(flow2Handler, times(1)).retryLastFailedFlowFromStart(any());
    }

    @Test
    void retryOnBackupBackupFailure() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setId(1L);
        sdxCluster.setClusterName("sdxclustername");
        FlowLog successfulFlowLog = new FlowLog();
        successfulFlowLog.setFlowId("FLOW_ID_1");
        successfulFlowLog.setStateStatus(StateStatus.SUCCESSFUL);
        successfulFlowLog.setNextEvent(DATALAKE_TRIGGER_BACKUP_EVENT.name());
        successfulFlowLog.setCreated(1L);
        successfulFlowLog.setCurrentState(INIT_STATE.name());
        successfulFlowLog.setFlowType(ClassValue.of(DatalakeRestoreFlowConfig.class));
        when(flow2Handler.getFirstRetryableStateLogfromLatestFlow(anyLong())).thenReturn(successfulFlowLog);

        underTest.retrySdx(sdxCluster);
        verify(flow2Handler, times(1)).retryLastFailedFlowFromStart(any());
    }

    @Test
    void retryFailsWithCloudbreakReasonWhenStackRetryRejectedAndLastCloudbreakFlowFailed() {
        SdxCluster sdxCluster = createSdxClusterWithStackCreationInProgressAsLastSuccessfulState();
        BadRequestException stackRetryException = new BadRequestException("HTTP 400 Bad Request");
        when(stackV4Endpoint.retry(any(), eq("sdxclustername"), eq("accountid"))).thenThrow(stackRetryException);
        when(exceptionMessageExtractor.getErrorMessage(stackRetryException))
                .thenReturn("Error message: \"Retry cannot be performed. The last flow did not fail or not retryable.\"");
        when(cloudbreakFlowService.getLastKnownFlowState(sdxCluster)).thenReturn(FlowState.FAILED);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> underTest.retrySdx(sdxCluster));

        assertEquals("Data Lake retry cannot be performed, because the last stack operation failed and the stack retry was rejected. "
                + "Reason: Error message: \"Retry cannot be performed. The last flow did not fail or not retryable.\"", exception.getMessage());
    }

    @ParameterizedTest
    @EnumSource(value = FlowState.class, names = "FAILED", mode = EnumSource.Mode.EXCLUDE)
    void retryRestartsFlowWhenStackRetryRejectedAndLastCloudbreakFlowNotFailed(FlowState lastKnownFlowState) {
        SdxCluster sdxCluster = createSdxClusterWithStackCreationInProgressAsLastSuccessfulState();
        BadRequestException stackRetryException = new BadRequestException("HTTP 400 Bad Request");
        when(stackV4Endpoint.retry(any(), eq("sdxclustername"), eq("accountid"))).thenThrow(stackRetryException);
        when(cloudbreakFlowService.getLastKnownFlowState(sdxCluster)).thenReturn(lastKnownFlowState);

        assertDoesNotThrow(() -> underTest.retrySdx(sdxCluster));

        verify(flow2Handler, times(1)).retryLastFailedFlow(eq(1L), any());
    }

    @Test
    void retryDoesNotCheckLastCloudbreakFlowWhenStackRetryAccepted() {
        SdxCluster sdxCluster = createSdxClusterWithStackCreationInProgressAsLastSuccessfulState();

        underTest.retrySdx(sdxCluster);

        verify(stackV4Endpoint, times(1)).retry(any(), eq("sdxclustername"), eq("accountid"));
        verify(cloudbreakFlowService, never()).getLastKnownFlowState(any());
    }

    private SdxCluster createSdxClusterWithStackCreationInProgressAsLastSuccessfulState() {
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
        successfulFlowLog.setFlowType(ClassValue.of(SdxCreateFlowConfig.class));
        doAnswer(invocation -> {
            ((Consumer<FlowLog>) invocation.getArgument(1)).accept(successfulFlowLog);
            return null;
        }).when(flow2Handler).retryLastFailedFlow(anyLong(), any());
        when(flow2Handler.getFirstRetryableStateLogfromLatestFlow(anyLong())).thenReturn(successfulFlowLog);
        return sdxCluster;
    }
}
