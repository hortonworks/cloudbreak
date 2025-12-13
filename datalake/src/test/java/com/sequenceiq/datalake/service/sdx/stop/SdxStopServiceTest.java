package com.sequenceiq.datalake.service.sdx.stop;

import static com.sequenceiq.datalake.service.sdx.stop.SdxStopService.UNSTOPPABLE_FLOWS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.service.FreeipaService;
import com.sequenceiq.datalake.service.sdx.DistroxService;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.flowcheck.CloudbreakFlowService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.service.flowlog.FlowChainLogService;

@ExtendWith(MockitoExtension.class)
public class SdxStopServiceTest {

    private static final String CLUSTER_NAME = "clusterName";

    private static final String ENV_NAME = "envName";

    private static final Long CLUSTER_ID = 1L;

    private static final String UNSTOPPABLE_FLOW_EXCEPTION_MESSAGE = "Can't stop datalake! Reason: Datalake " +
            "%s can not be stopped while flow chain %s is running.";

    @Mock
    private SdxReactorFlowManager sdxReactorFlowManager;

    @Mock
    private SdxService sdxService;

    @Mock
    private StackV4Endpoint stackV4Endpoint;

    @Mock
    private SdxStatusService sdxStatusService;

    @Mock
    private DistroxService distroxService;

    @Mock
    private FreeipaService freeipaService;

    @Mock
    private CloudbreakFlowService cloudbreakFlowService;

    @Mock
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    @Mock
    private FlowChainLogService flowChainLogService;

    @Mock
    private FlowLogService flowLogService;

    @InjectMocks
    private SdxStopService underTest;

    @Test
    public void testTriggerStop() {
        when(flowLogService.getLastFlowLog(anyLong())).thenReturn(Optional.empty());
        SdxCluster sdxCluster = sdxCluster();
        underTest.triggerStopIfClusterNotStopped(sdxCluster);
        verify(sdxReactorFlowManager).triggerSdxStopFlow(sdxCluster);
    }

    @Test
    public void testTriggerStopWhenUnstoppableFlowRunning() {
        FlowLog flowLog = new FlowLog();
        when(flowLogService.getLastFlowLog(anyLong())).thenReturn(Optional.of(flowLog));

        SdxCluster sdxCluster = sdxCluster();
        for (String flowChainType : UNSTOPPABLE_FLOWS) {
            when(flowChainLogService.getFlowChainType(any())).thenReturn(flowChainType);
            CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class,
                    () -> underTest.triggerStopIfClusterNotStopped(sdxCluster));
            assertEquals(String.format(UNSTOPPABLE_FLOW_EXCEPTION_MESSAGE, CLUSTER_NAME, flowChainType), exception.getMessage());
        }
    }

    @Test
    public void testStop() {
        SdxCluster sdxCluster = sdxCluster();
        when(sdxService.getById(CLUSTER_ID)).thenReturn(sdxCluster);
        underTest.stop(CLUSTER_ID);

        verify(stackV4Endpoint).putStopInternal(eq(0L), eq(CLUSTER_NAME), nullable(String.class));
        verify(sdxStatusService).setStatusForDatalakeAndNotify(DatalakeStatusEnum.STOP_IN_PROGRESS, "Datalake stop in progress", sdxCluster);
        verify(cloudbreakFlowService).saveLastCloudbreakFlowChainId(eq(sdxCluster), any());
    }

    @Test
    public void testStopWhenNotFoundException() {
        SdxCluster sdxCluster = sdxCluster();
        doThrow(NotFoundException.class).when(stackV4Endpoint).putStopInternal(eq(0L), eq(CLUSTER_NAME), nullable(String.class));
        when(sdxService.getById(CLUSTER_ID)).thenReturn(sdxCluster);
        underTest.stop(CLUSTER_ID);

        verify(sdxStatusService, times(0)).setStatusForDatalakeAndNotify(DatalakeStatusEnum.STOP_IN_PROGRESS,
                ResourceEvent.SDX_STOP_STARTED, "Datalake stop in progress", sdxCluster);
        verify(cloudbreakFlowService, times(0)).saveLastCloudbreakFlowChainId(eq(sdxCluster), any());
    }

    @Test
    public void testStopWhenClientErrorException() {
        SdxCluster sdxCluster = sdxCluster();
        when(webApplicationExceptionMessageExtractor.getErrorMessage(any())).thenReturn("Error message: \"error\"");
        doThrow(new ClientErrorException(Response.Status.BAD_REQUEST)).when(stackV4Endpoint)
                .putStopInternal(eq(0L), eq(CLUSTER_NAME), nullable(String.class));
        when(sdxService.getById(CLUSTER_ID)).thenReturn(sdxCluster);
        RuntimeException exception = assertThrows(RuntimeException.class, () -> underTest.stop(CLUSTER_ID));
        assertEquals("Cannot stop cluster, error happened during operation: Error message: \"error\"", exception.getMessage());
    }

    @Test
    public void testStopWhenWebApplicationException() {
        SdxCluster sdxCluster = sdxCluster();
        when(webApplicationExceptionMessageExtractor.getErrorMessage(any())).thenReturn("error");
        doThrow(new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR)).when(stackV4Endpoint)
                .putStopInternal(eq(0L), eq(CLUSTER_NAME), nullable(String.class));
        when(sdxService.getById(CLUSTER_ID)).thenReturn(sdxCluster);
        RuntimeException exception = assertThrows(RuntimeException.class, () -> underTest.stop(CLUSTER_ID));
        assertEquals("Cannot stop cluster, error happened during operation: error", exception.getMessage());
    }

    @Test
    public void testCheckIfStoppableMissingFlowChainId() {
        SdxCluster sdxCluster = sdxCluster();
        FlowLog flowLog = new FlowLog();
        when(flowLogService.getLastFlowLog(anyLong())).thenReturn(Optional.of(flowLog));
        when(flowChainLogService.getFlowChainType(any())).thenReturn(null);
        assertTrue(underTest.checkIfStoppable(sdxCluster).isEmpty());
    }

    @Test
    public void testCheckIfStoppableTrueForFinalizedFlow() {
        SdxCluster sdxCluster = sdxCluster();
        FlowLog flowLog = new FlowLog();
        flowLog.setFinalized(true);
        when(flowLogService.getLastFlowLog(anyLong())).thenReturn(Optional.of(flowLog));
        assertTrue(underTest.checkIfStoppable(sdxCluster).isEmpty());
    }

    private SdxCluster sdxCluster() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setId(CLUSTER_ID);
        sdxCluster.setClusterName(CLUSTER_NAME);
        sdxCluster.setEnvName(ENV_NAME);
        sdxCluster.setAccountId("accountid");
        return sdxCluster;
    }
}
