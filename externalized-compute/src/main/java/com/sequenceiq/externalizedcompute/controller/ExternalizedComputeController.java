package com.sequenceiq.externalizedcompute.controller;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.CREATE_ENVIRONMENT;
import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DELETE_ENVIRONMENT;
import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_ENVIRONMENT;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.CustomPermissionCheck;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.service.CommonPermissionCheckingUtils;
import com.sequenceiq.authorization.service.CustomCheckUtil;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.security.internal.TenantAwareParam;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
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

    @Inject
    private CustomCheckUtil customCheckUtil;

    @Inject
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    @Override
    @CheckPermissionByAccount(action = CREATE_ENVIRONMENT)
    public FlowIdentifier create(@Valid ExternalizedComputeClusterRequest externalizedComputeClusterRequest) {
        LOGGER.info("Externalized Compute Cluster request: {}", externalizedComputeClusterRequest);
        Crn userCrn = Crn.ofUser(ThreadBasedUserCrnProvider.getUserCrn());
        return externalizedComputeClusterService.prepareComputeClusterCreation(externalizedComputeClusterRequest, userCrn);
    }

    @Override
    @CustomPermissionCheck
    public FlowIdentifier delete(@NotEmpty String name) {
        LOGGER.info("Externalized Compute Cluster delete: {}", name);
        ExternalizedComputeCluster externalizedComputeCluster = getExternalizedComputeCluster(name);
        checkPermissionOnEnvironment(DELETE_ENVIRONMENT, externalizedComputeCluster);
        MDCBuilder.buildMdcContext(externalizedComputeCluster);
        return externalizedComputeClusterFlowManager.triggerExternalizedComputeClusterDeletion(externalizedComputeCluster);
    }

    @Override
    public ExternalizedComputeClusterResponse describe(String name) {
        ExternalizedComputeCluster externalizedComputeCluster = getExternalizedComputeCluster(name);
        checkPermissionOnEnvironment(DESCRIBE_ENVIRONMENT, externalizedComputeCluster);
        MDCBuilder.buildMdcContext(externalizedComputeCluster);
        return externalizedComputeClusterConverterService.convertToResponse(externalizedComputeCluster);
    }

    @Override
    @CheckPermissionByResourceCrn(action = DESCRIBE_ENVIRONMENT)
    public List<ExternalizedComputeClusterResponse> list(@TenantAwareParam @ResourceCrn @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) String envCrn) {
        return externalizedComputeClusterService.getAllByEnvironmentCrn(envCrn, ThreadBasedUserCrnProvider.getAccountId())
                .stream()
                .map(externalizedComputeClusterConverterService::convertToResponse)
                .toList();
    }

    private ExternalizedComputeCluster getExternalizedComputeCluster(String name) {
        return externalizedComputeClusterService.getExternalizedComputeCluster(name, ThreadBasedUserCrnProvider.getAccountId());
    }

    private void checkPermissionOnEnvironment(AuthorizationResourceAction action, ExternalizedComputeCluster cluster) {
        customCheckUtil.run(() ->
                commonPermissionCheckingUtils.checkPermissionForUserOnResource(action, ThreadBasedUserCrnProvider.getUserCrn(), cluster.getEnvironmentCrn()));
    }
}
