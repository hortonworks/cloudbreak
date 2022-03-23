package com.sequenceiq.datalake.service.sdx.stop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.events.EventSenderService;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.service.FreeipaService;
import com.sequenceiq.datalake.service.sdx.DistroxService;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.flowcheck.CloudbreakFlowService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;

@ExtendWith(MockitoExtension.class)
public class SdxStopServiceTest {

    private static final String CLUSTER_NAME = "clusterName";

    private static final String ENV_NAME = "envName";

    private static final Long CLUSTER_ID = 1L;

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
    private EventSenderService eventSenderService;

    @Mock
    private CloudbreakFlowService cloudbreakFlowService;

    @Mock
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

    @InjectMocks
    private SdxStopService underTest;

    @Test
    public void testTriggerStop() {
        SdxCluster sdxCluster = sdxCluster();

        underTest.triggerStopIfClusterNotStopped(sdxCluster);

        verify(sdxReactorFlowManager).triggerSdxStopFlow(sdxCluster);
    }

    @Test
    public void testStop() {
        SdxCluster sdxCluster = sdxCluster();
        when(sdxService.getById(CLUSTER_ID)).thenReturn(sdxCluster);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:datahub:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
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
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:datahub:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        underTest.stop(CLUSTER_ID);

        verify(sdxStatusService, times(0)).setStatusForDatalakeAndNotify(DatalakeStatusEnum.STOP_IN_PROGRESS,
                ResourceEvent.SDX_STOP_STARTED, "Datalake stop in progress", sdxCluster);
        verify(cloudbreakFlowService, times(0)).saveLastCloudbreakFlowChainId(eq(sdxCluster), any());
    }

    @Test
    public void testStopWhenClientErrorException() {
        SdxCluster sdxCluster = sdxCluster();
        ClientErrorException clientErrorException = mock(ClientErrorException.class);
        when(webApplicationExceptionMessageExtractor.getErrorMessage(any())).thenReturn("Error message: \"error\"");
        doThrow(clientErrorException).when(stackV4Endpoint).putStopInternal(eq(0L), eq(CLUSTER_NAME), nullable(String.class));
        when(sdxService.getById(CLUSTER_ID)).thenReturn(sdxCluster);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:datahub:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        RuntimeException exception = Assertions.assertThrows(RuntimeException.class, () -> underTest.stop(CLUSTER_ID));
        assertEquals("Cannot stop cluster, error happened during operation: Error message: \"error\"", exception.getMessage());
    }

    @Test
    public void testStopWhenWebApplicationException() {
        SdxCluster sdxCluster = sdxCluster();
        WebApplicationException clientErrorException = mock(WebApplicationException.class);
        when(webApplicationExceptionMessageExtractor.getErrorMessage(any())).thenReturn("error");
        doThrow(clientErrorException).when(stackV4Endpoint).putStopInternal(eq(0L), eq(CLUSTER_NAME), nullable(String.class));
        when(sdxService.getById(CLUSTER_ID)).thenReturn(sdxCluster);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:datahub:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        RuntimeException exception = Assertions.assertThrows(RuntimeException.class, () -> underTest.stop(CLUSTER_ID));
        assertEquals("Cannot stop cluster, error happened during operation: error", exception.getMessage());
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
