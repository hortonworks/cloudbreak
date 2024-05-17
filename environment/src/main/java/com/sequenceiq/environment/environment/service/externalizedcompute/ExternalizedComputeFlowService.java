package com.sequenceiq.environment.environment.service.externalizedcompute;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.ExternalizedComputeClusterDto;
import com.sequenceiq.environment.environment.flow.EnvironmentReactorFlowManager;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Service
public class ExternalizedComputeFlowService {

    @Inject
    private ExternalizedComputeService externalizedComputeService;

    @Inject
    private EnvironmentReactorFlowManager environmentReactorFlowManager;

    public FlowIdentifier reinitializeDefaultExternalizedComputeCluster(Environment environment, ExternalizedComputeClusterDto externalizedComputeClusterDto,
            boolean force) {
        externalizedComputeService.checkDefaultClusterExists(environment);
        externalizedComputeService.updateDefaultComputeClusterProperties(environment, externalizedComputeClusterDto);
        return environmentReactorFlowManager.triggerExternalizedComputeReinitializationFlow(ThreadBasedUserCrnProvider.getUserCrn(), environment, force);
    }

    public FlowIdentifier createDefaultExternalizedComputeClusterForExistingEnv(Environment environment,
            ExternalizedComputeClusterDto externalizedComputeClusterDto) {
        if (!EnvironmentStatus.AVAILABLE.equals(environment.getStatus())) {
            throw new BadRequestException("Environment is not in AVAILABLE state");
        }
        if (externalizedComputeService.getDefaultCluster(environment).isPresent()) {
            throw new BadRequestException("You can only have one default externalized compute cluster for an environment");
        } else {
            externalizedComputeService.updateDefaultComputeClusterProperties(environment, externalizedComputeClusterDto);
        }
        return environmentReactorFlowManager.triggerExternalizedComputeClusterCreationFlow(ThreadBasedUserCrnProvider.getUserCrn(), environment);
    }
}
