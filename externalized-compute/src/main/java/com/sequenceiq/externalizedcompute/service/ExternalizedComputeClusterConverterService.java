package com.sequenceiq.externalizedcompute.service;

import java.io.IOException;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterApiStatus;
import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterResponse;
import com.sequenceiq.externalizedcompute.entity.ExternalizedComputeCluster;
import com.sequenceiq.externalizedcompute.entity.ExternalizedComputeClusterStatus;

@Service
public class ExternalizedComputeClusterConverterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalizedComputeClusterConverterService.class);

    @Inject
    private ExternalizedComputeClusterStatusService externalizedComputeClusterStatusService;

    @Inject
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    public ExternalizedComputeClusterResponse convertToResponse(ExternalizedComputeCluster externalizedComputeCluster) {
        ExternalizedComputeClusterResponse response = new ExternalizedComputeClusterResponse();
        response.setEnvironmentCrn(externalizedComputeCluster.getEnvironmentCrn());
        response.setName(externalizedComputeCluster.getName());
        try {
            response.setTags(externalizedComputeCluster.getTags().get(new TypeReference<>() { }));
        } catch (IOException e) {
            LOGGER.error("Can't convert tags", e);
            throw new CloudbreakServiceException("Can't convert tags", e);
        }
        if (externalizedComputeCluster.getLiftieName() != null) {
            response.setLiftieClusterName(externalizedComputeCluster.getLiftieName());
            response.setLiftieClusterCrn(regionAwareCrnGenerator.generateCrnString(CrnResourceDescriptor.COMPUTE_CLUSTER,
                    externalizedComputeCluster.getLiftieName(), externalizedComputeCluster.getAccountId()));
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
