package com.sequenceiq.datalake.service.sdx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.WebApplicationException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.DiskUpdateEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskUpdateRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackAddVolumesRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackVerticalScaleV4Request;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxStatusEntity;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.reactor.api.event.EventSender;

@ExtendWith(MockitoExtension.class)
public class VerticalScaleServiceTest {

    @Mock
    private SdxService sdxService;

    @Mock
    private SdxStatusService sdxStatusService;

    @Mock
    private StackV4Endpoint stackV4Endpoint;

    @Mock
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    @Mock
    private EventSender eventSender;

    @Mock
    private SdxReactorFlowManager sdxReactorFlowManager;

    @Mock
    private DiskUpdateEndpoint diskUpdateEndpoint;

    @InjectMocks
    private VerticalScaleService underTest;

    @Test
    public void testVerticalScaleDatalakeRejectsCrossArchitecturePreFlight() {
        SdxCluster sdxCluster = mock(SdxCluster.class);
        when(sdxCluster.getClusterName()).thenReturn("test-cluster");
        StackVerticalScaleV4Request request = mock(StackVerticalScaleV4Request.class);
        String message = "Unable to resize since changing CPU architecture is not supported.";
        WebApplicationException webApplicationException = new BadRequestException(message);
        doThrow(webApplicationException).when(stackV4Endpoint)
                .verticalScalingValidateByName(anyLong(), eq("test-cluster"), anyString(), eq(request));
        when(webApplicationExceptionMessageExtractor.getErrorMessage(any(WebApplicationException.class))).thenReturn(message);

        com.sequenceiq.cloudbreak.common.exception.BadRequestException thrown = assertThrows(
                com.sequenceiq.cloudbreak.common.exception.BadRequestException.class,
                () -> underTest.verticalScaleDatalake(sdxCluster, request, "TEST-CRN"));

        assertEquals(message, thrown.getMessage());
        verify(eventSender, never()).sendEvent(any(), any());
    }

    @Test
    public void testVerticalScaleDatalakeTriggersFlowWhenValidationPasses() {
        SdxCluster sdxCluster = mock(SdxCluster.class);
        when(sdxCluster.getClusterName()).thenReturn("test-cluster");
        StackVerticalScaleV4Request request = mock(StackVerticalScaleV4Request.class);
        SdxStatusEntity statusEntity = mock(SdxStatusEntity.class);
        when(statusEntity.getStatus()).thenReturn(DatalakeStatusEnum.RUNNING);
        when(sdxStatusService.getActualStatusForSdx(sdxCluster.getId())).thenReturn(statusEntity);

        underTest.verticalScaleDatalake(sdxCluster, request, "TEST-CRN");

        verify(stackV4Endpoint).verticalScalingValidateByName(anyLong(), eq("test-cluster"), anyString(), eq(request));
        verify(eventSender).sendEvent(any(), any());
    }

    @Test
    public void testUpdateDisksDatalake() {
        SdxCluster sdxCluster = mock(SdxCluster.class);
        DiskUpdateRequest updateRequest = mock(DiskUpdateRequest.class);
        String userCrn = "TEST-CRN";
        underTest.updateDisksDatalake(sdxCluster, updateRequest, userCrn);
        verify(sdxReactorFlowManager).triggerDatalakeDiskUpdate(sdxCluster, updateRequest, userCrn);
    }

    @Test
    public void testGetDiskTypeChangeSupported() {
        doReturn(true).when(diskUpdateEndpoint).isDiskTypeChangeSupported("AWS");
        underTest.getDiskTypeChangeSupported("AWS");
        verify(diskUpdateEndpoint).isDiskTypeChangeSupported("AWS");
    }

    @Test
    public void testAddVolumesDatalake() {
        SdxCluster sdxCluster = mock(SdxCluster.class);
        StackAddVolumesRequest addVolumesRequest = mock(StackAddVolumesRequest.class);
        String userCrn = "TEST-CRN";
        underTest.addVolumesDatalake(sdxCluster, addVolumesRequest, userCrn);
        verify(sdxReactorFlowManager).triggerDatalakeAddVolumes(sdxCluster, addVolumesRequest, userCrn);
    }

    @Test
    public void testUpdateRootVolumeDatalake() {
        SdxCluster sdxCluster = mock(SdxCluster.class);
        DiskUpdateRequest updateRequest = mock(DiskUpdateRequest.class);
        String userCrn = "TEST-CRN";
        FlowIdentifier flowIdentifier = mock(FlowIdentifier.class);
        doReturn("test-flow-identifier").when(flowIdentifier).getPollableId();
        doReturn(flowIdentifier).when(sdxReactorFlowManager).triggerDatalakeRootVolumeUpdate(sdxCluster, updateRequest, userCrn);
        FlowIdentifier result = underTest.updateRootVolumeDatalake(sdxCluster, updateRequest, userCrn);
        verify(sdxReactorFlowManager).triggerDatalakeRootVolumeUpdate(sdxCluster, updateRequest, userCrn);
        assertEquals(flowIdentifier, result);
        assertEquals("test-flow-identifier", result.getPollableId());
    }
}
