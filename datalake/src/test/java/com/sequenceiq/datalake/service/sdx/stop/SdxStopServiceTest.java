package com.sequenceiq.datalake.service.sdx.stop;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.START_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.STOPPED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.STOP_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.STOP_REQUESTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptState;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.views.ClusterViewV4Response;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxStatusEntity;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.service.sdx.DistroxService;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;

@ExtendWith(MockitoExtension.class)
public class SdxStopServiceTest {

    private static final String CLUSTER_NAME = "clusterName";

    private static final String ENV_NAME = "envName";

    private static final Long CLUSTER_ID = 1L;

    @InjectMocks
    private SdxStopService underTest;

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
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    @Test
    public void testTriggerStopWhenSdxRunning() {
        SdxCluster sdxCluster = sdxCluster();
        SdxStatusEntity sdxStatusEntity = new SdxStatusEntity();
        sdxStatusEntity.setStatus(DatalakeStatusEnum.RUNNING);

        when(distroxService.getAttachedDistroXClusters(ENV_NAME, null)).thenReturn(Collections.emptyList());
        when(sdxStatusService.getActualStatusForSdx(sdxCluster)).thenReturn(sdxStatusEntity);

        underTest.triggerStopIfClusterNotStopped(sdxCluster);

        verify(sdxReactorFlowManager).triggerSdxStopFlow(CLUSTER_ID);
    }

    @Test
    public void testTriggerStopWhenSdxStopped() {
        SdxCluster sdxCluster = sdxCluster();
        SdxStatusEntity sdxStatusEntity = new SdxStatusEntity();
        sdxStatusEntity.setStatus(DatalakeStatusEnum.STOPPED);

        when(sdxStatusService.getActualStatusForSdx(sdxCluster)).thenReturn(sdxStatusEntity);

        BadRequestException exception = Assertions.assertThrows(BadRequestException.class, () -> underTest.triggerStopIfClusterNotStopped(sdxCluster));
        assertEquals("SDX is in stopped state, ignore it.", exception.getMessage());
    }

    @Test
    public void testTriggerStopWhenHasAttachedClusterWithStoppedState() {
        SdxCluster sdxCluster = sdxCluster();
        SdxStatusEntity sdxStatusEntity = new SdxStatusEntity();
        sdxStatusEntity.setStatus(DatalakeStatusEnum.RUNNING);

        Set<StackViewV4Response> stackViews = new HashSet<>();

        stackViews.add(getStackViewV4Response(STOPPED, STOPPED));
        stackViews.add(getStackViewV4Response(STOPPED, STOPPED));
        stackViews.add(getStackViewV4Response(STOPPED, STOPPED));

        when(distroxService.getAttachedDistroXClusters(ENV_NAME, null)).thenReturn(stackViews);
        when(sdxStatusService.getActualStatusForSdx(sdxCluster)).thenReturn(sdxStatusEntity);

        underTest.triggerStopIfClusterNotStopped(sdxCluster);
        verify(sdxReactorFlowManager).triggerSdxStopFlow(CLUSTER_ID);
    }

    @Test
    public void testTriggerStopWhenHasAttachedClusterWithAvailableState() {
        SdxCluster sdxCluster = sdxCluster();
        SdxStatusEntity sdxStatusEntity = new SdxStatusEntity();
        sdxStatusEntity.setStatus(DatalakeStatusEnum.RUNNING);

        Set<StackViewV4Response> stackViews = new HashSet<>();

        stackViews.add(getStackViewV4Response(STOPPED, STOPPED));
        stackViews.add(getStackViewV4Response(AVAILABLE, STOPPED));
        stackViews.add(getStackViewV4Response(STOPPED, STOPPED));

        when(distroxService.getAttachedDistroXClusters(ENV_NAME, null)).thenReturn(stackViews);
        when(sdxStatusService.getActualStatusForSdx(sdxCluster)).thenReturn(sdxStatusEntity);

        BadRequestException exception = Assertions.assertThrows(BadRequestException.class, () -> underTest.triggerStopIfClusterNotStopped(sdxCluster));
        assertEquals("Please stop all Datahub for the Environment (envName) before stop the Datalake", exception.getMessage());
    }

    @Test
    public void testStopWhenNotFoundException() {
        SdxCluster sdxCluster = sdxCluster();
        doThrow(NotFoundException.class).when(stackV4Endpoint).putStop(0L, CLUSTER_NAME);
        when(sdxService.getById(CLUSTER_ID)).thenReturn(sdxCluster);

        underTest.stop(CLUSTER_ID);

        verify(sdxStatusService, times(0)).setStatusForDatalakeAndNotify(DatalakeStatusEnum.STOP_IN_PROGRESS, ResourceEvent.SDX_STOP_STARTED,
                "Datalake stop in progress", sdxCluster);
    }

    @Test
    public void testStopWhenClientErrorException() {
        SdxCluster sdxCluster = sdxCluster();
        ClientErrorException clientErrorException = mock(ClientErrorException.class);
        when(webApplicationExceptionMessageExtractor.getErrorMessage(any())).thenReturn("Error message: \"error\"");
        doThrow(clientErrorException).when(stackV4Endpoint).putStop(0L, CLUSTER_NAME);
        when(sdxService.getById(CLUSTER_ID)).thenReturn(sdxCluster);

        RuntimeException exception = Assertions.assertThrows(RuntimeException.class, () -> underTest.stop(CLUSTER_ID));
        assertEquals("Can not stop stack, client error happened on Cloudbreak side: Error message: \"error\"", exception.getMessage());
    }

    @Test
    public void testStopWhenWebApplicationException() {
        SdxCluster sdxCluster = sdxCluster();
        WebApplicationException clientErrorException = mock(WebApplicationException.class);
        when(webApplicationExceptionMessageExtractor.getErrorMessage(any())).thenReturn("error");
        doThrow(clientErrorException).when(stackV4Endpoint).putStop(0L, CLUSTER_NAME);
        when(sdxService.getById(CLUSTER_ID)).thenReturn(sdxCluster);

        RuntimeException exception = Assertions.assertThrows(RuntimeException.class, () -> underTest.stop(CLUSTER_ID));
        assertEquals("Can not stop stack, web application error happened on Cloudbreak side: error", exception.getMessage());
    }

    @Test
    public void testCheckClusterStatusDuringStopWhenStackAndClusterStopped() throws JsonProcessingException {
        SdxCluster sdxCluster = sdxCluster();

        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatus(STOPPED);
        ClusterV4Response clusterV4Response = new ClusterV4Response();
        clusterV4Response.setStatus(STOPPED);
        stackV4Response.setCluster(clusterV4Response);

        when(stackV4Endpoint.get(0L, sdxCluster.getClusterName(), Collections.emptySet())).thenReturn(stackV4Response);

        AttemptResult<StackV4Response> actual = underTest.checkClusterStatusDuringStop(sdxCluster);

        assertEquals(stackV4Response, actual.getResult());
    }

    @Test
    public void testCheckClusterStatusDuringStopWhenStackAvailableOnly() throws JsonProcessingException {
        SdxCluster sdxCluster = sdxCluster();

        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatus(STOPPED);
        ClusterV4Response clusterV4Response = new ClusterV4Response();
        clusterV4Response.setStatus(STOP_REQUESTED);
        stackV4Response.setCluster(clusterV4Response);

        when(stackV4Endpoint.get(0L, sdxCluster.getClusterName(), Collections.emptySet())).thenReturn(stackV4Response);

        AttemptResult<StackV4Response> actual = underTest.checkClusterStatusDuringStop(sdxCluster);

        assertEquals(AttemptState.CONTINUE, actual.getState());
    }

    @Test
    public void testCheckClusterStatusDuringStopWhenStackFailed() throws JsonProcessingException {
        SdxCluster sdxCluster = sdxCluster();

        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatusReason("reason");
        stackV4Response.setStatus(STOP_FAILED);

        when(stackV4Endpoint.get(0L, sdxCluster.getClusterName(), Collections.emptySet())).thenReturn(stackV4Response);

        AttemptResult<StackV4Response> actual = underTest.checkClusterStatusDuringStop(sdxCluster);

        assertEquals(AttemptState.BREAK, actual.getState());
        assertEquals("SDX stop failed 'clusterName', reason", actual.getMessage());
    }

    @Test
    public void testCheckClusterStatusDuringStopWhenClusterFailed() throws JsonProcessingException {
        SdxCluster sdxCluster = sdxCluster();

        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatusReason("reason");
        stackV4Response.setStatus(AVAILABLE);
        ClusterV4Response clusterV4Response = new ClusterV4Response();
        clusterV4Response.setStatus(STOP_FAILED);
        clusterV4Response.setStatusReason("cluster reason");
        stackV4Response.setCluster(clusterV4Response);

        when(stackV4Endpoint.get(0L, sdxCluster.getClusterName(), Collections.emptySet())).thenReturn(stackV4Response);

        AttemptResult<StackV4Response> actual = underTest.checkClusterStatusDuringStop(sdxCluster);

        assertEquals(AttemptState.BREAK, actual.getState());
        assertEquals("SDX stop failed 'clusterName', cluster reason", actual.getMessage());
    }

    @Test
    public void testCheckClusterStatusDuringStartWhenStackStartFailed() throws JsonProcessingException {
        SdxCluster sdxCluster = sdxCluster();

        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatusReason("reason");
        stackV4Response.setStatus(START_FAILED);

        when(stackV4Endpoint.get(0L, sdxCluster.getClusterName(), Collections.emptySet())).thenReturn(stackV4Response);

        AttemptResult<StackV4Response> actual = underTest.checkClusterStatusDuringStop(sdxCluster);

        assertEquals(AttemptState.BREAK, actual.getState());
        assertEquals("SDX stop failed 'clusterName', stack is in inconsistency state: START_FAILED", actual.getMessage());
    }

    @Test
    public void testCheckClusterStatusDuringStartWhenClusterStartFailed() throws JsonProcessingException {
        SdxCluster sdxCluster = sdxCluster();

        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatusReason("reason");
        stackV4Response.setStatus(STOPPED);
        ClusterV4Response clusterV4Response = new ClusterV4Response();
        clusterV4Response.setStatus(START_FAILED);
        clusterV4Response.setStatusReason("cluster reason");
        stackV4Response.setCluster(clusterV4Response);

        when(stackV4Endpoint.get(0L, sdxCluster.getClusterName(), Collections.emptySet())).thenReturn(stackV4Response);

        AttemptResult<StackV4Response> actual = underTest.checkClusterStatusDuringStop(sdxCluster);

        assertEquals(AttemptState.BREAK, actual.getState());
        assertEquals("SDX stop failed 'clusterName', cluster is in inconsistency state: START_FAILED", actual.getMessage());
    }

    private SdxCluster sdxCluster() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setId(CLUSTER_ID);
        sdxCluster.setClusterName(CLUSTER_NAME);
        sdxCluster.setEnvName(ENV_NAME);
        return sdxCluster;
    }

    private StackViewV4Response getStackViewV4Response(Status stackStatus, Status clusterStatus) {
        StackViewV4Response stackViewV4Response = new StackViewV4Response();
        stackViewV4Response.setStatus(stackStatus);
        ClusterViewV4Response clusterViewV4Response = new ClusterViewV4Response();
        clusterViewV4Response.setStatus(clusterStatus);
        stackViewV4Response.setCluster(clusterViewV4Response);
        return stackViewV4Response;
    }
}
