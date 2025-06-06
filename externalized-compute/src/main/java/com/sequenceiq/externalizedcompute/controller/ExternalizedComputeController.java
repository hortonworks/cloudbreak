package com.sequenceiq.externalizedcompute.controller;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.CREATE_ENVIRONMENT;
import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DELETE_ENVIRONMENT;
import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_ENVIRONMENT;

import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
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
    @CheckPermissionByAccount(action = CREATE_ENVIRONMENT)
    public FlowIdentifier create(ExternalizedComputeClusterRequest externalizedComputeClusterRequest) {
        LOGGER.info("Externalized Compute Cluster request: {}", externalizedComputeClusterRequest);
        Crn userCrn = Crn.ofUser(ThreadBasedUserCrnProvider.getUserCrn());
        return externalizedComputeClusterService.prepareComputeClusterCreation(externalizedComputeClusterRequest, false, userCrn);
    }

    @Override
    @CheckPermissionByAccount(action = DELETE_ENVIRONMENT)
    public FlowIdentifier delete(@ResourceCrn String environmentCrn, String name, boolean force) {
        LOGGER.info("Externalized Compute Cluster delete: {}. Force: {}", name, force);
        ExternalizedComputeCluster externalizedComputeCluster = getExternalizedComputeCluster(environmentCrn, name);
        MDCBuilder.buildMdcContext(externalizedComputeCluster);
        return externalizedComputeClusterFlowManager.triggerExternalizedComputeClusterDeletion(externalizedComputeCluster, force);
    }

    @Override
    @CheckPermissionByResourceCrn(action = DESCRIBE_ENVIRONMENT)
    public ExternalizedComputeClusterResponse describe(@ResourceCrn String environmentCrn, String name) {
        ExternalizedComputeCluster externalizedComputeCluster = getExternalizedComputeCluster(environmentCrn, name);
        MDCBuilder.buildMdcContext(externalizedComputeCluster);
        return externalizedComputeClusterConverterService.convertToResponse(externalizedComputeCluster);
    }

    @Override
    @CheckPermissionByResourceCrn(action = DESCRIBE_ENVIRONMENT)
    public List<ExternalizedComputeClusterResponse> list(@ResourceCrn String environmentCrn) {
        return externalizedComputeClusterService.getAllByEnvironmentCrn(environmentCrn, ThreadBasedUserCrnProvider.getAccountId())
                .stream()
                .map(externalizedComputeClusterConverterService::convertToResponse)
                .toList();
    }

    @Deprecated(forRemoval = true)
    @Override
    @CheckPermissionByResourceCrn(action = DESCRIBE_ENVIRONMENT)
    public List<ExternalizedComputeClusterResponse> listDeprecated(@ResourceCrn String environmentCrn) {
        return externalizedComputeClusterService.getAllByEnvironmentCrn(environmentCrn, ThreadBasedUserCrnProvider.getAccountId())
                .stream()
                .map(externalizedComputeClusterConverterService::convertToResponse)
                .toList();
    }

    private ExternalizedComputeCluster getExternalizedComputeCluster(String environmentCrn, String name) {
        return externalizedComputeClusterService.getExternalizedComputeCluster(environmentCrn, name);
    }
}
