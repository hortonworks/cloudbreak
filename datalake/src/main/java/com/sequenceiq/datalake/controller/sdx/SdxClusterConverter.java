package com.sequenceiq.datalake.controller.sdx;

import static com.sequenceiq.cloudbreak.util.Benchmark.measure;
import static com.sequenceiq.datalake.service.TagUtil.getTags;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxStatusEntity;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

@Service
public class SdxClusterConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxClusterConverter.class);

    @Inject
    private SdxStatusService sdxStatusService;

    public SdxClusterResponse sdxClusterToResponse(SdxCluster sdxCluster) {
        SdxClusterResponse sdxClusterResponse = new SdxClusterResponse();
        SdxStatusEntity actualStatusForSdx = measure(() -> sdxStatusService.getActualStatusForSdx(sdxCluster), LOGGER,
                "Fetching SDX status took {}ms from DB. Name: [{}]", sdxCluster.getClusterName());
        if (actualStatusForSdx != null && actualStatusForSdx.getStatus() != null) {
            sdxClusterResponse.setStatus(SdxClusterStatusResponse.valueOf(actualStatusForSdx.getStatus().name()));
            sdxClusterResponse.setStatusReason(actualStatusForSdx.getStatusReason());
        }
        sdxClusterResponse.setRuntime(sdxCluster.getRuntime());
        sdxClusterResponse.setName(sdxCluster.getClusterName());
        sdxClusterResponse.setCrn(sdxCluster.getCrn());
        sdxClusterResponse.setClusterShape(sdxCluster.getClusterShape());
        sdxClusterResponse.setEnvironmentName(sdxCluster.getEnvName());
        sdxClusterResponse.setEnvironmentCrn(sdxCluster.getEnvCrn());
        sdxClusterResponse.setStackCrn(sdxCluster.getStackCrn());
        sdxClusterResponse.setCreated(sdxCluster.getCreated());
        sdxClusterResponse.setCloudStorageBaseLocation(sdxCluster.getCloudStorageBaseLocation());
        sdxClusterResponse.setCloudStorageFileSystemType(sdxCluster.getCloudStorageFileSystemType());
        sdxClusterResponse.setDatabaseServerCrn(sdxCluster.getDatabaseCrn());
        sdxClusterResponse.setRangerRazEnabled(sdxCluster.isRangerRazEnabled());
        sdxClusterResponse.setTags(getTags(sdxCluster.getTags()));
        sdxClusterResponse.setCertExpirationState(sdxCluster.getCertExpirationState());
        sdxClusterResponse.setSdxClusterServiceVersion(sdxCluster.getSdxClusterServiceVersion());
        sdxClusterResponse.setDetached(sdxCluster.isDetached());
        sdxClusterResponse.setEnableMultiAz(sdxCluster.isEnableMultiAz());
        return sdxClusterResponse;
    }
}
