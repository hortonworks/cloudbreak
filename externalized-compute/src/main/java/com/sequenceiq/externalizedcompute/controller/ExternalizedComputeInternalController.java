package com.sequenceiq.externalizedcompute.controller;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.validation.constraints.NotEmpty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.security.internal.InitiatorUserCrn;
import com.sequenceiq.cloudbreak.auth.security.internal.TenantAwareParam;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.externalizedcompute.api.endpoint.ExternalizedComputeClusterInternalEndpoint;
import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterInternalRequest;
import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterResponse;
import com.sequenceiq.externalizedcompute.entity.ExternalizedComputeCluster;
import com.sequenceiq.externalizedcompute.flow.ExternalizedComputeClusterFlowManager;
import com.sequenceiq.externalizedcompute.service.ExternalizedComputeClusterConverterService;
import com.sequenceiq.externalizedcompute.service.ExternalizedComputeClusterService;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@InternalOnly
@Controller
public class ExternalizedComputeInternalController implements ExternalizedComputeClusterInternalEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalizedComputeInternalController.class);

    @Inject
    private ExternalizedComputeClusterService externalizedComputeClusterService;

    @Inject
    private ExternalizedComputeClusterFlowManager externalizedComputeClusterFlowManager;

    @Inject
    private ExternalizedComputeClusterConverterService externalizedComputeClusterConverterService;

    @Override
    public FlowIdentifier create(@TenantAwareParam ExternalizedComputeClusterInternalRequest request, @InitiatorUserCrn String initiatorUserCrn) {
        LOGGER.info("Externalized Compute Cluster internal request: {}", request);
        Crn userCrn = Crn.ofUser(ThreadBasedUserCrnProvider.getUserCrn());
        return externalizedComputeClusterService.prepareComputeClusterCreation(request, request.isDefaultCluster(), userCrn);
    }

    @Override
    public FlowIdentifier delete(
            @TenantAwareParam @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) String environmentCrn,
            @NotEmpty String name) {
        LOGGER.info("Externalized Compute Cluster internal delete: {}", name);
        ExternalizedComputeCluster externalizedComputeCluster = getExternalizedComputeCluster(environmentCrn, name);
        MDCBuilder.buildMdcContext(externalizedComputeCluster);
        return externalizedComputeClusterFlowManager.triggerExternalizedComputeClusterDeletion(externalizedComputeCluster);
    }

    @Override
    public ExternalizedComputeClusterResponse describe(
            @TenantAwareParam @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) String environmentCrn,
            @NotEmpty String name) {
        ExternalizedComputeCluster externalizedComputeCluster = getExternalizedComputeCluster(environmentCrn, name);
        MDCBuilder.buildMdcContext(externalizedComputeCluster);
        return externalizedComputeClusterConverterService.convertToResponse(externalizedComputeCluster);
    }

    @Override
    public List<ExternalizedComputeClusterResponse> list(
            @TenantAwareParam @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) String environmentCrn) {
        return externalizedComputeClusterService.getAllByEnvironmentCrn(environmentCrn, ThreadBasedUserCrnProvider.getAccountId())
                .stream()
                .map(externalizedComputeClusterConverterService::convertToResponse)
                .toList();
    }

    private ExternalizedComputeCluster getExternalizedComputeCluster(String environmentCrn, String name) {
        return externalizedComputeClusterService.getExternalizedComputeCluster(environmentCrn, name);
    }
}
