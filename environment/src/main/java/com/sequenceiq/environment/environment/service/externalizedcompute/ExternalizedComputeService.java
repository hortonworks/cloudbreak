package com.sequenceiq.environment.environment.service.externalizedcompute;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.dyngr.Polling;
import com.dyngr.core.AttemptMaker;
import com.dyngr.core.AttemptResults;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.polling.ExtendedPollingResult;
import com.sequenceiq.cloudbreak.polling.PollingService;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.environment.domain.DefaultComputeCluster;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.EnvironmentView;
import com.sequenceiq.environment.environment.dto.EnvironmentValidationDto;
import com.sequenceiq.environment.environment.dto.ExternalizedComputeClusterDto;
import com.sequenceiq.environment.environment.flow.creation.handler.computecluster.ComputeClusterCreationRetrievalTask;
import com.sequenceiq.environment.environment.flow.creation.handler.computecluster.ComputeClusterPollerObject;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.validation.EnvironmentFlowValidatorService;
import com.sequenceiq.environment.events.EventSenderService;
import com.sequenceiq.environment.exception.EnvironmentServiceException;
import com.sequenceiq.environment.exception.ExternalizedComputeOperationFailedException;
import com.sequenceiq.environment.exception.ExternalizedComputeValidationFailedException;
import com.sequenceiq.environment.util.PollingConfig;
import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterApiStatus;
import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterBase;
import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterCredentialValidationResponse;
import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterInternalRequest;
import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterResponse;

@Component
public class ExternalizedComputeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalizedComputeService.class);

    private static final String DEFAULT_COMPUTE_CLUSTER_NAME_FORMAT = "default-%s-compute-cluster";

    @Value("${environment.externalizedCompute.enabled}")
    private boolean externalizedComputeEnabled;

    @Inject
    private ExternalizedComputeClientService externalizedComputeClientService;

    @Inject
    private EnvironmentService environmentService;

    @Inject
    private PollingService<ComputeClusterPollerObject> computeClusterPollingService;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private EnvironmentFlowValidatorService validatorService;

    @Inject
    private EventSenderService eventSenderService;

    public void createComputeCluster(Environment environment) {
        try {
            if (environment.getDefaultComputeCluster().isCreate()) {
                externalizedComputeValidation(environment.getAccountId());
                credentialValidation(environment);
                String computeClusterName = getDefaultComputeClusterName(environment.getName());
                if (externalizedComputeClientService.getComputeCluster(environment.getResourceCrn(), computeClusterName).isPresent()) {
                    LOGGER.info("Externalized compute cluster already exists with name: {}", computeClusterName);
                } else {
                    LOGGER.info("Creating compute cluster with name {}", computeClusterName);
                    ExternalizedComputeClusterInternalRequest request = new ExternalizedComputeClusterInternalRequest();
                    request.setEnvironmentCrn(environment.getResourceCrn());
                    request.setName(computeClusterName);
                    request.setDefaultCluster(true);
                    externalizedComputeClientService.createComputeCluster(request);
                }
            } else {
                LOGGER.info("Creating compute cluster is skipped because it was not configured");
            }
        } catch (ExternalizedComputeOperationFailedException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("Could not create compute cluster due to:", e);
            throw new ExternalizedComputeOperationFailedException("Could not create compute cluster: " + e.getMessage(), e);
        }
    }

    public void reInitializeComputeCluster(Environment environment, boolean force) {
        try {
            externalizedComputeValidation(environment.getAccountId());
            String computeClusterName = getDefaultComputeClusterName(environment.getName());
            boolean creationInProgress = externalizedComputeClientService.getComputeCluster(environment.getResourceCrn(), computeClusterName)
                    .map(cluster -> cluster.getStatus().isCreationInProgress()).orElse(false);
            if (creationInProgress) {
                LOGGER.info("Compute cluster is in progress, skipping reinitialization.");
            } else {
                LOGGER.info("Reinitialize compute cluster with name {}", computeClusterName);
                ExternalizedComputeClusterInternalRequest request = new ExternalizedComputeClusterInternalRequest();
                request.setEnvironmentCrn(environment.getResourceCrn());
                request.setName(computeClusterName);
                request.setDefaultCluster(true);
                externalizedComputeClientService.reInitializeComputeCluster(request, force);
            }
        } catch (ExternalizedComputeOperationFailedException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("Could not reinitialize compute cluster due to:", e);
            throw new ExternalizedComputeOperationFailedException("Could not reinitialize compute cluster: " + e.getMessage(), e);
        }
    }

    public void externalizedComputeValidation(String accountId) {
        if (!externalizedComputeEnabled) {
            throw new BadRequestException("Externalized compute not enabled");
        }
    }

    public void credentialValidation(Environment environment) {
        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder()
                .withEnvironmentDto(environmentService.getEnvironmentDto(environment))
                .build();
        ValidationResult validationResult = validatorService.validateCredentialForExternalizedComputeCluster(environmentValidationDto);
        if (validationResult.hasError()) {
            String formattedErrors = validationResult.getFormattedErrors();
            LOGGER.debug("Externalized compute cluster credential validation failed for environment {} with {}.", environment.getId(), formattedErrors);
            throw new EnvironmentServiceException(formattedErrors);
        }
    }

    public Optional<ExternalizedComputeClusterResponse> getComputeCluster(String environmentCrn, String name) {
        return externalizedComputeClientService.getComputeCluster(environmentCrn, name);
    }

    public Optional<ExternalizedComputeClusterResponse> getDefaultCluster(Environment environment) {
        String defaultComputeClusterName = getDefaultComputeClusterName(environment.getName());
        return externalizedComputeClientService.getComputeCluster(environment.getResourceCrn(),
                defaultComputeClusterName);
    }

    public String getDefaultComputeClusterName(String environmentName) {
        return String.format(DEFAULT_COMPUTE_CLUSTER_NAME_FORMAT, environmentName);
    }

    public void deleteComputeCluster(String envCrn, PollingConfig pollingConfig, boolean force) {
        if (externalizedComputeEnabled) {
            List<ExternalizedComputeClusterResponse> clusters = externalizedComputeClientService.list(envCrn);
            LOGGER.debug("Compute clusters for the environment: {}", clusters);
            for (ExternalizedComputeClusterResponse cluster : clusters) {
                externalizedComputeClientService.deleteComputeCluster(envCrn, cluster.getName(), force);
            }
            try {
                pollingDeletion(pollingConfig, () -> {
                    List<ExternalizedComputeClusterResponse> clustersUnderDeletion = externalizedComputeClientService.list(envCrn);
                    LOGGER.debug("Compute clusters under deletion: {}", clustersUnderDeletion);
                    if (!clustersUnderDeletion.isEmpty()) {
                        for (ExternalizedComputeClusterResponse clusterResponse : clustersUnderDeletion) {
                            if (ExternalizedComputeClusterApiStatus.DELETE_FAILED.equals(clusterResponse.getStatus())) {
                                return AttemptResults.breakFor("Found a compute cluster with delete failed status: " + clusterResponse.getName() +
                                        ". Failure reason: " + clusterResponse.getStatusReason());
                            }
                        }
                        return AttemptResults.justContinue();
                    } else {
                        return AttemptResults.finishWith(null);
                    }
                });
            } catch (UserBreakException e) {
                LOGGER.warn("Compute clusters deletion failed.", e);
                throw new EnvironmentServiceException("Compute clusters deletion failed. Reason: " + e.getMessage());
            }
        } else {
            LOGGER.info("Externalized compute is disabled, skipping delete.");
        }
    }

    public Set<String> getComputeClusterNames(EnvironmentView environment) {
        LOGGER.debug("Get compute cluster names of the environment: '{}'", environment.getName());
        if (externalizedComputeEnabled) {
            return externalizedComputeClientService.list(environment.getResourceCrn()).stream()
                    .map(ExternalizedComputeClusterBase::getName)
                    .collect(Collectors.toSet());
        } else {
            return Set.of();
        }
    }

    private void pollingDeletion(PollingConfig pollingConfig, AttemptMaker<Object> pollingAttempt) {
        try {
            Polling.stopAfterDelay(pollingConfig.getTimeout(), pollingConfig.getTimeoutTimeUnit())
                    .stopIfException(pollingConfig.getStopPollingIfExceptionOccured())
                    .waitPeriodly(pollingConfig.getSleepTime(), pollingConfig.getSleepTimeUnit())
                    .run(pollingAttempt);
        } catch (PollerStoppedException e) {
            LOGGER.info("Compute cluster deletion timed out");
            throw new EnvironmentServiceException("Compute cluster deletion timed out");
        }
    }

    public void updateDefaultComputeClusterProperties(Environment environment, ExternalizedComputeClusterDto externalizedComputeClusterDto) {
        DefaultComputeCluster defaultComputeCluster = new DefaultComputeCluster();
        defaultComputeCluster.setCreate(externalizedComputeClusterDto.isCreate());
        defaultComputeCluster.setPrivateCluster(externalizedComputeClusterDto.isPrivateCluster());
        defaultComputeCluster.setKubeApiAuthorizedIpRanges(externalizedComputeClusterDto.getKubeApiAuthorizedIpRanges());
        defaultComputeCluster.setOutboundType(externalizedComputeClusterDto.getOutboundType());
        defaultComputeCluster.setWorkerNodeSubnetIds(externalizedComputeClusterDto.getWorkerNodeSubnetIds());
        environment.setDefaultComputeCluster(defaultComputeCluster);
        environmentService.save(environment);
    }

    public void awaitComputeClusterCreation(Environment environment, String computeClusterName) throws ExternalizedComputeOperationFailedException {
        try {
            ExtendedPollingResult pollWithTimeout = computeClusterPollingService.pollWithTimeout(
                    new ComputeClusterCreationRetrievalTask(this, eventSenderService, environmentService),
                    new ComputeClusterPollerObject(environment.getId(), environment.getResourceCrn(), computeClusterName),
                    ComputeClusterCreationRetrievalTask.COMPUTE_CLUSTER_RETRYING_INTERVAL,
                    ComputeClusterCreationRetrievalTask.COMPUTE_CLUSTER_RETRYING_COUNT,
                    ComputeClusterCreationRetrievalTask.COMPUTE_CLUSTER_FAILURE_COUNT);
            if (!pollWithTimeout.isSuccess()) {
                LOGGER.info("Compute cluster creation polling has stopped due to the unsuccessful result: {}", pollWithTimeout.getPollingResult());
                Optional.ofNullable(pollWithTimeout.getException()).ifPresentOrElse(e -> {
                    throw new ExternalizedComputeOperationFailedException(e.getMessage());
                }, () -> {
                    throw new ExternalizedComputeOperationFailedException("Polling result was: " + pollWithTimeout.getPollingResult());
                });
            }
        } catch (Exception e) {
            if (ExceptionUtils.getRootCause(e) instanceof ExternalizedComputeValidationFailedException) {
                clearDefaultComputeClusterProperties(environment);
            }
            LOGGER.error("Compute cluster creation failed", e);
            throw new ExternalizedComputeOperationFailedException(ExceptionUtils.getRootCause(e).getMessage(), e);
        }
    }

    public ExternalizedComputeClusterCredentialValidationResponse validateCredential(String credentialName, String region) {
        ExternalizedComputeClusterCredentialValidationResponse validateCredentialResult = externalizedComputeClientService.validateCredential(
                credentialName, region);
        LOGGER.debug("Validate credential result: {}", validateCredentialResult);
        return validateCredentialResult;
    }

    private void clearDefaultComputeClusterProperties(Environment environment) {
        LOGGER.info("Clearing default compute cluster properties for environment: '{}'", environment.getName());
        environment.setDefaultComputeCluster(new DefaultComputeCluster());
        environmentService.save(environment);
    }
}
