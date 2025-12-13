package com.sequenceiq.distrox.v1.distrox.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.sharedservice.SharedServiceV4Request;
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

        assertNull(sdxRequest);
    }

    @ParameterizedTest
    @EnumSource(value = SdxClusterStatusResponse.class, names = { "RUNNING", "DATALAKE_BACKUP_INPROGRESS", "DATALAKE_ROLLING_UPGRADE_IN_PROGRESS",
            "DATALAKE_UPGRADE_PREPARATION_IN_PROGRESS" })
    public void testGetSharedServiceWhenSdxIsAvailable(SdxClusterStatusResponse status) {
        SdxClusterResponse sdxClusterResponse = new SdxClusterResponse();
        sdxClusterResponse.setName("some-sdx");
        sdxClusterResponse.setStatus(status);

        SharedServiceV4Request sdxRequest = underTest.getSharedService(sdxClusterResponse);

        assertEquals("some-sdx", sdxRequest.getDatalakeName());
    }

    @Test
    public void testGetSharedServiceWhenSdxIsNotRunning() {
        SdxClusterResponse sdxClusterResponse = new SdxClusterResponse();
        sdxClusterResponse.setName("some-sdx");
        sdxClusterResponse.setEnvironmentName("some-env");
        sdxClusterResponse.setStatusReason("external db creation in progress");
        sdxClusterResponse.setStatus(SdxClusterStatusResponse.EXTERNAL_DATABASE_CREATION_IN_PROGRESS);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> underTest.getSharedService(sdxClusterResponse));
        assertEquals(exception.getMessage(), "Your current Environment some-env contains one Data Lake " +
                "the name of which is some-sdx. This Data Lake should be in running/available state but currently it " +
                "is in 'EXTERNAL_DATABASE_CREATION_IN_PROGRESS' instead of Running. Please make sure your Data Lake" +
                " is up and running before you provision the Data Hub. If your Data Lake is in stopped state, please" +
                " restart it. If your Data Lake has failed to provision please check our documentation" +
                " https://docs.cloudera.com/management-console/cloud/data-lakes/topics/mc-data-lake.html or" +
                " contact the Cloudera support to get some help or try to provision a new Data Lake with the" +
                " correct configuration.");
    }
}
