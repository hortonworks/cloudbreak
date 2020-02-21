package com.sequenceiq.distrox.v1.distrox.converter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.sharedservice.SharedServiceV4Request;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

@ExtendWith(MockitoExtension.class)
public class SdxConverterTest {

    @InjectMocks
    private SdxConverter underTest;

    @Test
    public void testGetSharedServiceWhenSdxNullAndEnvironmentHasNotSdx() {
        SharedServiceV4Request sdxRequest = underTest.getSharedService(null);

        Assertions.assertNull(sdxRequest);
    }

    @Test
    public void testGetSharedServiceWhenSdxIsRunning() {
        SdxClusterResponse sdxClusterResponse = new SdxClusterResponse();
        sdxClusterResponse.setName("some-sdx");
        sdxClusterResponse.setStatus(SdxClusterStatusResponse.RUNNING);

        SharedServiceV4Request sdxRequest = underTest.getSharedService(sdxClusterResponse);

        Assertions.assertEquals("some-sdx", sdxRequest.getDatalakeName());
    }

    @Test
    public void testGetSharedServiceWhenSdxIsNotRunning() {
        SdxClusterResponse sdxClusterResponse = new SdxClusterResponse();
        sdxClusterResponse.setName("some-sdx");
        sdxClusterResponse.setStatusReason("external db creation in progress");
        sdxClusterResponse.setStatus(SdxClusterStatusResponse.EXTERNAL_DATABASE_CREATION_IN_PROGRESS);

        BadRequestException exception = Assertions.assertThrows(BadRequestException.class, () -> underTest.getSharedService(sdxClusterResponse));
        Assertions.assertEquals(exception.getMessage(), "Datalake should be running state. Current state is 'EXTERNAL_DATABASE_CREATION_IN_PROGRESS' "
                + "instead of Running");
    }

    private StackStatus getStatus(Status status) {
        StackStatus stackStatus = new StackStatus();
        stackStatus.setStatus(status);
        return stackStatus;
    }
}
