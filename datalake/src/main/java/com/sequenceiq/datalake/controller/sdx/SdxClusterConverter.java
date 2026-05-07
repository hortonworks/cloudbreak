package com.sequenceiq.datalake.controller.sdx;

import static com.sequenceiq.cloudbreak.util.Benchmark.measure;
import static com.sequenceiq.datalake.service.TagUtil.getTags;

import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxStatusEntity;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseResponse;

@Service
public class SdxClusterConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxClusterConverter.class);

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private SdxService sdxService;

    public SdxClusterResponse sdxClusterToResponse(SdxCluster sdxCluster) {
        SdxClusterResponse sdxClusterResponse = new SdxClusterResponse();
        SdxStatusEntity actualStatusForSdx = measure(() -> sdxStatusService.getActualStatusForSdx(sdxCluster), LOGGER,
                "Fetching SDX status took {}ms from DB. Name: [{}]", sdxCluster.getClusterName());
        if (actualStatusForSdx != null && actualStatusForSdx.getStatus() != null) {
            sdxClusterResponse.setStatus(SdxClusterStatusResponse.valueOf(actualStatusForSdx.getStatus().name()));
            sdxClusterResponse.setStatusReason(actualStatusForSdx.getStatusReason());
        }
        if (!sdxCluster.isDetached()) {
            Optional<SdxCluster> detachedSdxCluster = measure(() -> sdxService.findDetachedSdxClusterByOriginalCrn(sdxCluster.getCrn()), LOGGER,
                    "Fetching detached SDX cluster took {}ms fromDB, Name: [{}]", sdxCluster.getClusterName());
            detachedSdxCluster.ifPresent(detached -> sdxClusterResponse.setDetachedClusterName(detached.getClusterName()));
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
        sdxClusterResponse.setRangerRmsEnabled(sdxCluster.isRangerRmsEnabled());
        sdxClusterResponse.setSeLinuxPolicy(sdxCluster.getSeLinux().name());
        sdxClusterResponse.setNotificationState(sdxCluster.getNotificationState());
        sdxClusterResponse.setTags(getTags(sdxCluster.getTags()));
        sdxClusterResponse.setCertExpirationState(sdxCluster.getCertExpirationState());
        sdxClusterResponse.setCertExpirationDetails(sdxCluster.getCertExpirationDetails());
        sdxClusterResponse.setSdxClusterServiceVersion(sdxCluster.getSdxClusterServiceVersion());
        sdxClusterResponse.setDetached(sdxCluster.isDetached());
        sdxClusterResponse.setEnableMultiAz(sdxCluster.isEnableMultiAz());
        sdxClusterResponse.setDatabaseEngineVersion(sdxCluster.getDatabaseEngineVersion());
        SdxDatabaseResponse sdxDatabaseResponse = new SdxDatabaseResponse();
        sdxDatabaseResponse.setAvailabilityType(sdxCluster.getDatabaseAvailabilityType());
        sdxDatabaseResponse.setDatabaseEngineVersion(sdxCluster.getDatabaseEngineVersion());
        sdxDatabaseResponse.setDatabaseServerCrn(sdxCluster.getDatabaseCrn());
        sdxClusterResponse.setSdxDatabaseResponse(sdxDatabaseResponse);
        sdxClusterResponse.setProviderSyncStates(sdxCluster.getProviderSyncStates());
        return sdxClusterResponse;
    }
}
