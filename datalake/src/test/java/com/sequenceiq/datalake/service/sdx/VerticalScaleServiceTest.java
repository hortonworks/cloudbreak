package com.sequenceiq.datalake.service.sdx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.DiskUpdateEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskUpdateRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackAddVolumesRequest;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.datalake.entity.SdxCluster;
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
    @Disabled("CB-31498")
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
