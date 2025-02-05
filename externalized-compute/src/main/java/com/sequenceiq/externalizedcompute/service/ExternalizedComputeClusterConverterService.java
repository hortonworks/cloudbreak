package com.sequenceiq.externalizedcompute.service;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterApiStatus;
import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterResponse;
import com.sequenceiq.externalizedcompute.entity.ExternalizedComputeCluster;
import com.sequenceiq.externalizedcompute.entity.ExternalizedComputeClusterStatus;

@Service
public class ExternalizedComputeClusterConverterService {

    @Inject
    private ExternalizedComputeClusterStatusService externalizedComputeClusterStatusService;

    @Inject
    private ExternalizedComputeClusterService externalizedComputeClusterService;

    public ExternalizedComputeClusterResponse convertToResponse(ExternalizedComputeCluster externalizedComputeCluster) {
        ExternalizedComputeClusterResponse response = new ExternalizedComputeClusterResponse();
        response.setEnvironmentCrn(externalizedComputeCluster.getEnvironmentCrn());
        response.setName(externalizedComputeCluster.getName());
        if (externalizedComputeCluster.getLiftieName() != null) {
            response.setLiftieClusterName(externalizedComputeCluster.getLiftieName());
            response.setLiftieClusterCrn(externalizedComputeClusterService.getLiftieClusterCrn(externalizedComputeCluster));
        }
        ExternalizedComputeClusterStatus actualStatus = externalizedComputeClusterStatusService.getActualStatus(externalizedComputeCluster);
        if (actualStatus != null) {
            if (actualStatus.getStatus() != null) {
                response.setStatus(ExternalizedComputeClusterApiStatus.valueOf(actualStatus.getStatus().name()));
            }
            if (actualStatus.getStatusReason() != null) {
                response.setStatusReason(actualStatus.getStatusReason());
            }
        }
        return response;
    }

}
