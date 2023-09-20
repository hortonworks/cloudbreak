package com.sequenceiq.datalake.service.sdx;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.DiskUpdateEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskUpdateRequest;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
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
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

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
        doReturn(mock(RegionAwareInternalCrnGenerator.class)).when(regionAwareInternalCrnGeneratorFactory).sdxAdmin();
        underTest.getDiskTypeChangeSupported("AWS");
        verify(diskUpdateEndpoint).isDiskTypeChangeSupported("AWS");
    }
}
