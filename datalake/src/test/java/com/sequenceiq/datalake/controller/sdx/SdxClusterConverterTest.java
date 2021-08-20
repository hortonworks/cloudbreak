package com.sequenceiq.datalake.controller.sdx;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxStatusEntity;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

@ExtendWith(MockitoExtension.class)
public class SdxClusterConverterTest {

    @Mock
    private SdxStatusService sdxStatusService;

    @InjectMocks
    private SdxClusterConverter sdxClusterConverter;

    @Test
    public void sdxClusterToResponseWithoutDisplayNameTest() {
        SdxCluster sdxCluster = getValidSdxCluster();
        SdxStatusEntity sdxStatusEntity = new SdxStatusEntity();
        sdxStatusEntity.setDatalake(sdxCluster);
        when(sdxStatusService.getActualStatusForSdx(any())).thenReturn(sdxStatusEntity);
        SdxClusterResponse sdxClusterResponse = sdxClusterConverter.sdxClusterToResponse(sdxCluster);
        Assert.assertEquals(sdxCluster.getClusterName(), sdxClusterResponse.getName());
        Assert.assertEquals(sdxCluster.getClusterName(), sdxClusterResponse.getDisplayName());
    }

    @Test
    public void sdxClusterToResponseWithDisplayNameTest() {
        SdxCluster sdxCluster = getValidSdxCluster();
        sdxCluster.setClusterDisplayName("test-sdx-cluster-display");
        SdxStatusEntity sdxStatusEntity = new SdxStatusEntity();
        sdxStatusEntity.setDatalake(sdxCluster);
        when(sdxStatusService.getActualStatusForSdx(any())).thenReturn(sdxStatusEntity);
        SdxClusterResponse sdxClusterResponse = sdxClusterConverter.sdxClusterToResponse(sdxCluster);
        Assert.assertEquals(sdxCluster.getClusterName(), sdxClusterResponse.getName());
        Assert.assertEquals("test-sdx-cluster-display", sdxClusterResponse.getDisplayName());
    }

    private SdxCluster getValidSdxCluster() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setClusterName("test-sdx-cluster");
        sdxCluster.setClusterShape(SdxClusterShape.MEDIUM_DUTY_HA);
        sdxCluster.setEnvName("test-env");
        sdxCluster.setCrn("crn:sdxcluster");
        return sdxCluster;
    }
}
