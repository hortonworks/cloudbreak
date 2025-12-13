package com.sequenceiq.datalake.service.sdx.start;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.BadRequestException;
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
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.service.FreeipaService;
import com.sequenceiq.datalake.service.sdx.DistroxService;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.flowcheck.CloudbreakFlowService;
import com.sequenceiq.datalake.service.sdx.status.AvailabilityChecker;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;

@ExtendWith(MockitoExtension.class)
public class SdxStartServiceTest {

    private static final String CLUSTER_NAME = "clusterName";

    private static final Long CLUSTER_ID = 1L;

    private static final String ENV_CRN =
            "crn:cdp:environments:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:environment:4044a133-e941-48d4-82c4-ce1e231160d9";

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

    @Mock
    private CloudbreakFlowService cloudbreakFlowService;

    @Mock
    private AvailabilityChecker availabilityChecker;

    @Mock
    private DistroxService distroxService;

    @Test
    public void testTriggerStart() {
        SdxCluster sdxCluster = sdxCluster();
        sdxCluster.setEnvCrn("envCrn");

        underTest.triggerStartIfClusterNotRunning(sdxCluster);

        verify(freeipaService).checkFreeipaRunning("envCrn");
        verify(sdxReactorFlowManager).triggerSdxStartFlow(sdxCluster);
    }

    @Test
    public void testTriggerStartWhenFreeipaNull() {
        SdxCluster sdxCluster = sdxCluster();

        underTest.triggerStartIfClusterNotRunning(sdxCluster);

        verify(sdxReactorFlowManager).triggerSdxStartFlow(sdxCluster);
    }

    @Test
    public void testTriggerStartWhenFreeipaStopped() {
        SdxCluster sdxCluster = sdxCluster();
        sdxCluster.setEnvCrn("envCrn");

        BadRequestException freeIpaException = new BadRequestException("Freeipa should be in Available state but currently is " + Status.STOPPED);

        doThrow(freeIpaException).when(freeipaService).checkFreeipaRunning("envCrn");

        BadRequestException exception = assertThrows(BadRequestException.class, () -> underTest.triggerStartIfClusterNotRunning(sdxCluster));
        assertEquals("Freeipa should be in Available state but currently is " + Status.STOPPED, exception.getMessage());
    }

    @Test
    public void testStartWhenStartSucceed() {
        SdxCluster sdxCluster = sdxCluster();
        when(sdxService.getById(CLUSTER_ID)).thenReturn(sdxCluster);
        underTest.start(CLUSTER_ID);

        verify(stackV4Endpoint).putStartInternal(eq(0L), eq(CLUSTER_NAME), nullable(String.class));
        verify(cloudbreakFlowService).saveLastCloudbreakFlowChainId(eq(sdxCluster), any());
        verify(sdxStatusService).setStatusForDatalakeAndNotify(DatalakeStatusEnum.START_IN_PROGRESS, "Datalake start in progress", sdxCluster);
    }

    @Test
    public void testStartWhenNotFoundException() {
        SdxCluster sdxCluster = sdxCluster();
        doThrow(NotFoundException.class).when(stackV4Endpoint).putStartInternal(eq(0L), eq(CLUSTER_NAME), nullable(String.class));
        when(sdxService.getById(CLUSTER_ID)).thenReturn(sdxCluster);
        underTest.start(CLUSTER_ID);

        verifyNoInteractions(cloudbreakFlowService);
        verify(sdxStatusService, times(0)).setStatusForDatalakeAndNotify(DatalakeStatusEnum.START_IN_PROGRESS, ResourceEvent.SDX_START_STARTED,
                "Datalake start in progress", sdxCluster);
    }

    @Test
    public void testStartWheClientErrorException() {
        SdxCluster sdxCluster = sdxCluster();
        when(webApplicationExceptionMessageExtractor.getErrorMessage(any())).thenReturn("Error message: \"error\"");
        doThrow(new ClientErrorException(Response.Status.BAD_REQUEST)).when(stackV4Endpoint).putStartInternal(eq(0L), eq(CLUSTER_NAME), nullable(String.class));
        when(sdxService.getById(CLUSTER_ID)).thenReturn(sdxCluster);
        RuntimeException exception = assertThrows(RuntimeException.class, () -> underTest.start(CLUSTER_ID));

        verifyNoInteractions(cloudbreakFlowService);
        assertEquals("Cannot start cluster, error happened during operation: Error message: \"error\"", exception.getMessage());
    }

    @Test
    public void testStartWhenWebApplicationException() {
        SdxCluster sdxCluster = sdxCluster();
        when(webApplicationExceptionMessageExtractor.getErrorMessage(any())).thenReturn("Error message: \"error\"");
        doThrow(new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR)).when(stackV4Endpoint)
                .putStartInternal(eq(0L), eq(CLUSTER_NAME), nullable(String.class));
        when(sdxService.getById(CLUSTER_ID)).thenReturn(sdxCluster);
        RuntimeException exception = assertThrows(RuntimeException.class, () -> underTest.start(CLUSTER_ID));

        verifyNoInteractions(cloudbreakFlowService);
        assertEquals("Cannot start cluster, error happened during operation: Error message: \"error\"", exception.getMessage());
    }

    @Test
    public void testStartDatahubSucceed() {
        SdxCluster sdxCluster = sdxCluster();
        when(sdxService.getById(CLUSTER_ID)).thenReturn(sdxCluster);

        underTest.startAllDatahubs(CLUSTER_ID);

        verify(distroxService).startAttachedDistrox(eq(ENV_CRN));
    }

    @Test
    public void testStartDatahubThrowsWebException() {
        SdxCluster sdxCluster = sdxCluster();
        when(sdxService.getById(CLUSTER_ID)).thenReturn(sdxCluster);
        when(webApplicationExceptionMessageExtractor.getErrorMessage(any())).thenReturn("Error message: \"error\"");
        doThrow(new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR)).when(distroxService).startAttachedDistrox(eq(ENV_CRN));
        RuntimeException exception = assertThrows(RuntimeException.class, () -> underTest.startAllDatahubs(CLUSTER_ID));

        assertEquals("Can not start datahub, error happened during operation: Error message: \"error\"", exception.getMessage());

    }

    private SdxCluster sdxCluster() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setId(CLUSTER_ID);
        sdxCluster.setClusterName(CLUSTER_NAME);
        sdxCluster.setAccountId("accountid");
        sdxCluster.setEnvCrn(ENV_CRN);
        return sdxCluster;
    }

}
