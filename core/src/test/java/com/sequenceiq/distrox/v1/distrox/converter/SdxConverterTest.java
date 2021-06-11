package com.sequenceiq.distrox.v1.distrox.converter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.sharedservice.SharedServiceV4Request;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
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
        sdxClusterResponse.setEnvironmentName("some-env");
        sdxClusterResponse.setStatusReason("external db creation in progress");
        sdxClusterResponse.setStatus(SdxClusterStatusResponse.EXTERNAL_DATABASE_CREATION_IN_PROGRESS);

        BadRequestException exception = Assertions.assertThrows(BadRequestException.class, () -> underTest.getSharedService(sdxClusterResponse));
        Assertions.assertEquals(exception.getMessage(), "Your current Environment some-env contains one Data Lake " +
                "the name of which is some-sdx. This Data Lake should be in running/available state but currently it " +
                "is in 'EXTERNAL_DATABASE_CREATION_IN_PROGRESS' instead of Running. Please make sure your Data Lake" +
                " is up and running before you provision the Data Hub. If your Data Lake is in stopped state, please" +
                " restart it. If your Data Lake has failed to provision please check our documentation" +
                " https://docs.cloudera.com/management-console/cloud/data-lakes/topics/mc-data-lake.html or" +
                " contact the Cloudera support to get some help or try to provision a new Data Lake with the" +
                " correct configuration.");
    }

    private StackStatus getStatus(Status status) {
        StackStatus stackStatus = new StackStatus();
        stackStatus.setStatus(status);
        return stackStatus;
    }
}
