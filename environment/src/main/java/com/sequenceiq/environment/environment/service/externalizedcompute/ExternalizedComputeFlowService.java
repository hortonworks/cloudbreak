package com.sequenceiq.environment.environment.service.externalizedcompute;

import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.ExternalizedComputeClusterDto;
import com.sequenceiq.environment.environment.flow.EnvironmentReactorFlowManager;
import com.sequenceiq.environment.environment.validation.EnvironmentValidatorService;
import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterApiStatus;
import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Service
public class ExternalizedComputeFlowService {

    @Inject
    private ExternalizedComputeService externalizedComputeService;

    @Inject
    private EnvironmentReactorFlowManager environmentReactorFlowManager;

    @Inject
    private EnvironmentValidatorService environmentValidatorService;

    public FlowIdentifier initializeDefaultExternalizedComputeCluster(Environment environment, ExternalizedComputeClusterDto externalizedComputeClusterDto,
            boolean force) {
        if (!EnvironmentStatus.AVAILABLE.equals(environment.getStatus())) {
            throw new BadRequestException("Environment is not in AVAILABLE state");
        }
        if (!externalizedComputeClusterDto.isCreate()) {
            throw new BadRequestException("Create field is disabled in externalized compute cluster request!");
        }
        ValidationResult validationResult = environmentValidatorService.validateExternalizedComputeCluster(externalizedComputeClusterDto,
                environment.getAccountId(), environment.getNetwork().getSubnetMetas().keySet());
        if (validationResult.hasError()) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
        Optional<ExternalizedComputeClusterResponse> defaultCluster = externalizedComputeService.getDefaultCluster(environment);
        if (defaultCluster.isPresent() && ExternalizedComputeClusterApiStatus.AVAILABLE.equals(defaultCluster.get().getStatus()) && !force) {
            throw new BadRequestException("Your default compute cluster is in Available state!");
        }
        externalizedComputeService.updateDefaultComputeClusterProperties(environment, externalizedComputeClusterDto);
        if (defaultCluster.isPresent()) {
            return environmentReactorFlowManager.triggerExternalizedComputeReinitializationFlow(ThreadBasedUserCrnProvider.getUserCrn(), environment, force);
        } else {
            return environmentReactorFlowManager.triggerExternalizedComputeClusterCreationFlow(ThreadBasedUserCrnProvider.getUserCrn(), environment);
        }
    }
}
