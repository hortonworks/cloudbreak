package com.sequenceiq.externalizedcompute.controller;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.externalizedcompute.api.endpoint.ExternalizedComputeClusterEndpoint;
import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterRequest;
import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterResponse;
import com.sequenceiq.externalizedcompute.entity.ExternalizedComputeCluster;
import com.sequenceiq.externalizedcompute.flow.ExternalizedComputeClusterFlowManager;
import com.sequenceiq.externalizedcompute.service.ExternalizedComputeClusterConverterService;
import com.sequenceiq.externalizedcompute.service.ExternalizedComputeClusterService;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Controller
public class ExternalizedComputeController implements ExternalizedComputeClusterEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalizedComputeController.class);

    @Inject
    private ExternalizedComputeClusterService externalizedComputeClusterService;

    @Inject
    private ExternalizedComputeClusterFlowManager externalizedComputeClusterFlowManager;

    @Inject
    private ExternalizedComputeClusterConverterService externalizedComputeClusterConverterService;

    @Override
    public FlowIdentifier create(ExternalizedComputeClusterRequest externalizedComputeClusterRequest) {
        LOGGER.info("Externalized Compute Cluster request: {}", externalizedComputeClusterRequest);
        return externalizedComputeClusterService.prepareComputeClusterCreation(externalizedComputeClusterRequest);
    }

    @Override
    public FlowIdentifier delete(String name) {
        LOGGER.info("Externalized Compute Cluster delete: {}", name);
        ExternalizedComputeCluster externalizedComputeCluster =
                externalizedComputeClusterService.getExternalizedComputeCluster(name, ThreadBasedUserCrnProvider.getAccountId());
        return externalizedComputeClusterFlowManager.triggerExternalizedComputeClusterDeletion(externalizedComputeCluster);
    }

    @Override
    public ExternalizedComputeClusterResponse describe(String name) {
        ExternalizedComputeCluster externalizedComputeCluster = externalizedComputeClusterService.getExternalizedComputeCluster(name,
                ThreadBasedUserCrnProvider.getAccountId());
        return externalizedComputeClusterConverterService.convertToResponse(externalizedComputeCluster);
    }

    @Override
    public List<ExternalizedComputeClusterResponse> list(String envCrn) {
        List<ExternalizedComputeCluster> allByEnvironmentCrn = externalizedComputeClusterService.getAllByEnvironmentCrn(envCrn,
                ThreadBasedUserCrnProvider.getAccountId());
        List<ExternalizedComputeClusterResponse> externalizedComputeClusterResponses = new ArrayList<>();
        for (ExternalizedComputeCluster externalizedComputeCluster : allByEnvironmentCrn) {
            ExternalizedComputeClusterResponse response =
                    externalizedComputeClusterConverterService.convertToResponse(externalizedComputeCluster);
            externalizedComputeClusterResponses.add(response);
        }
        return externalizedComputeClusterResponses;
    }
}
