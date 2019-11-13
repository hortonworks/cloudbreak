package com.sequenceiq.datalake.service.sdx.start;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.START_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.START_REQUESTED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.STOP_FAILED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

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
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.service.FreeipaService;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;

@ExtendWith(MockitoExtension.class)
public class SdxStartServiceTest {

    private static final String CLUSTER_NAME = "clusterName";

    private static final Long CLUSTER_ID = 1L;

    @InjectMocks
    private SdxStartService underTest;

    @Mock
    private SdxReactorFlowManager sdxReactorFlowManager;

    @Mock
    private SdxService sdxService;

    @Mock
    private StackV4Endpoint stackV4Endpoint;

    @Mock
    private SdxStatusService sdxStatusService;

    @Mock
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    @Mock
    private FreeipaService freeipaService;

    @Test
    public void testTriggerStart() {
        SdxCluster sdxCluster = sdxCluster();
        sdxCluster.setEnvCrn("envCrn");

        DescribeFreeIpaResponse freeIpaResponse = new DescribeFreeIpaResponse();
        freeIpaResponse.setStatus(Status.AVAILABLE);

        when(freeipaService.describe("envCrn")).thenReturn(freeIpaResponse);

        underTest.triggerStartIfClusterNotRunning(sdxCluster);

        verify(sdxReactorFlowManager).triggerSdxStartFlow(CLUSTER_ID);
    }

    @Test
    public void testTriggerStartWhenFreeipaNull() {
        SdxCluster sdxCluster = sdxCluster();

        underTest.triggerStartIfClusterNotRunning(sdxCluster);

        verify(sdxReactorFlowManager).triggerSdxStartFlow(CLUSTER_ID);
    }

    @Test
    public void testTriggerStartWhenFreeipaStopped() {
        SdxCluster sdxCluster = sdxCluster();
        sdxCluster.setEnvCrn("envCrn");

        DescribeFreeIpaResponse freeipa = new DescribeFreeIpaResponse();
        freeipa.setStatus(Status.STOPPED);

        when(freeipaService.describe("envCrn")).thenReturn(freeipa);

        BadRequestException exception = Assertions.assertThrows(BadRequestException.class, () -> underTest.triggerStartIfClusterNotRunning(sdxCluster));
        assertEquals("Freeipa should be in Available state but currently is " + freeipa.getStatus().name(), exception.getMessage());
    }

    @Test
    public void testStartWhenNotFoundException() {
        SdxCluster sdxCluster = sdxCluster();
        doThrow(NotFoundException.class).when(stackV4Endpoint).putStart(0L, CLUSTER_NAME);
        when(sdxService.getById(CLUSTER_ID)).thenReturn(sdxCluster);

        underTest.start(CLUSTER_ID);

        verify(sdxStatusService, times(0)).setStatusForDatalakeAndNotify(DatalakeStatusEnum.START_IN_PROGRESS, ResourceEvent.SDX_START_STARTED,
                "Datalake start in progress", sdxCluster);
    }

    @Test
    public void testStartWheClientErrorException() {
        SdxCluster sdxCluster = sdxCluster();
        ClientErrorException clientErrorException = mock(ClientErrorException.class);
        when(webApplicationExceptionMessageExtractor.getErrorMessage(any())).thenReturn("Error message: \"error\"");
        doThrow(clientErrorException).when(stackV4Endpoint).putStart(0L, CLUSTER_NAME);
        when(sdxService.getById(CLUSTER_ID)).thenReturn(sdxCluster);

        RuntimeException exception = Assertions.assertThrows(RuntimeException.class, () -> underTest.start(CLUSTER_ID));
        assertEquals("Can not start stack, client error happened on Cloudbreak side: Error message: \"error\"", exception.getMessage());
    }

    @Test
    public void testStartWhenWebApplicationException() {
        SdxCluster sdxCluster = sdxCluster();
        WebApplicationException clientErrorException = mock(WebApplicationException.class);
        when(webApplicationExceptionMessageExtractor.getErrorMessage(any())).thenReturn("Error message: \"error\"");
        doThrow(clientErrorException).when(stackV4Endpoint).putStart(0L, CLUSTER_NAME);
        when(sdxService.getById(CLUSTER_ID)).thenReturn(sdxCluster);

        RuntimeException exception = Assertions.assertThrows(RuntimeException.class, () -> underTest.start(CLUSTER_ID));

        assertEquals("Can not start stack, web application error happened on Cloudbreak side: Error message: \"error\"", exception.getMessage());
    }

    @Test
    public void testCheckClusterStatusDuringStartWhenStackAndClusterAvailable() throws JsonProcessingException {
        SdxCluster sdxCluster = sdxCluster();

        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatus(AVAILABLE);
        ClusterV4Response clusterV4Response = new ClusterV4Response();
        clusterV4Response.setStatus(AVAILABLE);
        stackV4Response.setCluster(clusterV4Response);

        when(stackV4Endpoint.get(0L, sdxCluster.getClusterName(), Collections.emptySet())).thenReturn(stackV4Response);

        AttemptResult<StackV4Response> actual = underTest.checkClusterStatusDuringStart(sdxCluster);

        assertEquals(stackV4Response, actual.getResult());
    }

    @Test
    public void testCheckClusterStatusDuringStartWhenStackAvailableOnly() throws JsonProcessingException {
        SdxCluster sdxCluster = sdxCluster();

        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatus(AVAILABLE);
        ClusterV4Response clusterV4Response = new ClusterV4Response();
        clusterV4Response.setStatus(START_REQUESTED);
        stackV4Response.setCluster(clusterV4Response);

        when(stackV4Endpoint.get(0L, sdxCluster.getClusterName(), Collections.emptySet())).thenReturn(stackV4Response);

        AttemptResult<StackV4Response> actual = underTest.checkClusterStatusDuringStart(sdxCluster);

        assertEquals(AttemptState.CONTINUE, actual.getState());
    }

    @Test
    public void testCheckClusterStatusDuringStartWhenStackFailed() throws JsonProcessingException {
        SdxCluster sdxCluster = sdxCluster();

        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatusReason("reason");
        stackV4Response.setStatus(START_FAILED);

        when(stackV4Endpoint.get(0L, sdxCluster.getClusterName(), Collections.emptySet())).thenReturn(stackV4Response);

        AttemptResult<StackV4Response> actual = underTest.checkClusterStatusDuringStart(sdxCluster);

        assertEquals(AttemptState.BREAK, actual.getState());
        assertEquals("SDX start failed 'clusterName', reason", actual.getMessage());
    }

    @Test
    public void testCheckClusterStatusDuringStartWhenClusterFailed() throws JsonProcessingException {
        SdxCluster sdxCluster = sdxCluster();

        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatusReason("reason");
        stackV4Response.setStatus(AVAILABLE);
        ClusterV4Response clusterV4Response = new ClusterV4Response();
        clusterV4Response.setStatus(START_FAILED);
        clusterV4Response.setStatusReason("cluster reason");
        stackV4Response.setCluster(clusterV4Response);

        when(stackV4Endpoint.get(0L, sdxCluster.getClusterName(), Collections.emptySet())).thenReturn(stackV4Response);

        AttemptResult<StackV4Response> actual = underTest.checkClusterStatusDuringStart(sdxCluster);

        assertEquals(AttemptState.BREAK, actual.getState());
        assertEquals("SDX start failed 'clusterName', cluster reason", actual.getMessage());
    }

    @Test
    public void testCheckClusterStatusDuringStartWhenStackStopFailed() throws JsonProcessingException {
        SdxCluster sdxCluster = sdxCluster();

        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatusReason("reason");
        stackV4Response.setStatus(STOP_FAILED);

        when(stackV4Endpoint.get(0L, sdxCluster.getClusterName(), Collections.emptySet())).thenReturn(stackV4Response);

        AttemptResult<StackV4Response> actual = underTest.checkClusterStatusDuringStart(sdxCluster);

        assertEquals(AttemptState.BREAK, actual.getState());
        assertEquals("SDX start failed 'clusterName', stack is in inconsistency state: STOP_FAILED", actual.getMessage());
    }

    @Test
    public void testCheckClusterStatusDuringStartWhenClusterStopFailed() throws JsonProcessingException {
        SdxCluster sdxCluster = sdxCluster();

        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatusReason("reason");
        stackV4Response.setStatus(AVAILABLE);
        ClusterV4Response clusterV4Response = new ClusterV4Response();
        clusterV4Response.setStatus(STOP_FAILED);
        clusterV4Response.setStatusReason("cluster reason");
        stackV4Response.setCluster(clusterV4Response);

        when(stackV4Endpoint.get(0L, sdxCluster.getClusterName(), Collections.emptySet())).thenReturn(stackV4Response);

        AttemptResult<StackV4Response> actual = underTest.checkClusterStatusDuringStart(sdxCluster);

        assertEquals(AttemptState.BREAK, actual.getState());
        assertEquals("SDX start failed 'clusterName', cluster is in inconsistency state: STOP_FAILED", actual.getMessage());
    }

    private SdxCluster sdxCluster() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setId(CLUSTER_ID);
        sdxCluster.setClusterName(CLUSTER_NAME);
        return sdxCluster;
    }

}
