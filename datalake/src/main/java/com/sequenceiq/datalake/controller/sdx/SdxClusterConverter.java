package com.sequenceiq.datalake.controller.sdx;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxStatusEntity;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

@Service
public class SdxClusterConverter {

    @Inject
    private SdxStatusService sdxStatusService;

    public SdxClusterResponse sdxClusterToResponse(SdxCluster sdxCluster) {
        SdxClusterResponse sdxClusterResponse = new SdxClusterResponse();
        SdxStatusEntity actualStatusForSdx = sdxStatusService.getActualStatusForSdx(sdxCluster);
        if (actualStatusForSdx != null && actualStatusForSdx.getStatus() != null) {
            sdxClusterResponse.setStatus(SdxClusterStatusResponse.valueOf(actualStatusForSdx.getStatus().name()));
            sdxClusterResponse.setStatusReason(actualStatusForSdx.getStatusReason());
        }
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
        return sdxClusterResponse;
    }
}
