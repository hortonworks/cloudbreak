package com.sequenceiq.datalake.controller.sdx;

import org.springframework.stereotype.Service;

import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

@Service
public class SdxClusterConverter {

    public SdxClusterResponse sdxClusterToResponse(SdxCluster sdxCluster) {
        SdxClusterResponse sdxClusterResponse = new SdxClusterResponse();
        if (sdxCluster.getStatus() != null) {
            sdxClusterResponse.setStatus(SdxClusterStatusResponse.valueOf(sdxCluster.getStatus().name()));
        }
        sdxClusterResponse.setStatusReason(sdxCluster.getStatusReason());
        sdxClusterResponse.setName(sdxCluster.getClusterName());
        sdxClusterResponse.setCrn(sdxCluster.getCrn());
        sdxClusterResponse.setClusterShape(sdxCluster.getClusterShape());
        sdxClusterResponse.setEnvironmentName(sdxCluster.getEnvName());
        sdxClusterResponse.setEnvironmentCrn(sdxCluster.getEnvCrn());
        return sdxClusterResponse;
    }
}
