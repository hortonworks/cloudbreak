package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.dyngr.Polling;
import com.dyngr.core.AttemptResults;
import com.google.common.collect.Maps;
import com.sequenceiq.authorization.service.CompositeAuthResourcePropertyProvider;
import com.sequenceiq.authorization.service.EnvironmentPropertyProvider;
import com.sequenceiq.authorization.service.list.Resource;
import com.sequenceiq.authorization.service.list.ResourceWithId;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionHandler;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.common.api.backup.response.BackupResponse;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import com.sequenceiq.common.api.type.CdpResourceType;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxClusterView;
import com.sequenceiq.datalake.flow.statestore.DatalakeInMemoryStateStore;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.repository.SdxClusterViewRepository;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.environment.api.v1.credential.endpoint.CredentialEndpoint;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.environment.api.v1.platformresource.CredentialPlatformResourceEndpoint;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformVmtypesResponse;

@Service
public class EnvironmentService implements EnvironmentPropertyProvider, CompositeAuthResourcePropertyProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentService.class);

    @Value("${sdx.environment.sleeptime_sec:10}")
    private int sleepTimeInSec;

    @Value("${sdx.environment.duration_min:60}")
    private int durationInMinutes;

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    @Inject
    private SdxClusterViewRepository sdxClusterViewRepository;

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private EnvironmentEndpoint environmentEndpoint;

    @Inject
    private CredentialEndpoint credentialEndpoint;

    @Inject
    private CredentialPlatformResourceEndpoint credentialPlatformResourceEndpoint;

    @Inject
    private WebApplicationExceptionHandler webApplicationExceptionHandler;

    @Inject
    private EntitlementService entitlementService;

    public DetailedEnvironmentResponse getByName(String environment) {
        try {
            return environmentEndpoint.getByName(environment);
        } catch (WebApplicationException e) {
            throw webApplicationExceptionHandler.handleException(e);
        }
    }

    public DetailedEnvironmentResponse getByCrn(String environmentCrn) {
        try {
            return environmentEndpoint.getByCrn(environmentCrn);
        } catch (WebApplicationException e) {
            throw webApplicationExceptionHandler.handleException(e);
        }
    }

    public CredentialResponse getCredentialByCrn(String credentialCrn) {
        return credentialEndpoint.getByResourceCrn(credentialCrn);
    }

    /**
     * Get the backup location.
     * @param envCrn Environemnt CRN.
     * @return backuplocation configured for the environment, If not, returns the log location.
     */
    public String getBackupLocation(String environmentCrn) {
        DetailedEnvironmentResponse detailedEnvironmentResponse = getByCrn(environmentCrn);
        BackupResponse backupResponse = detailedEnvironmentResponse.getBackup();
        TelemetryResponse telemetryResponse = detailedEnvironmentResponse.getTelemetry();
        if (backupResponse != null && backupResponse.getStorageLocation() != null) {
            LOGGER.info("Using the backup location to store the datalake backup");
            return backupResponse.getStorageLocation();
        } else if (telemetryResponse != null && telemetryResponse.getLogging() != null) {
            LOGGER.info("Backup location not configured. Using the log location to store the datalake backup");
            return telemetryResponse.getLogging().getStorageLocation();
        } else {
            LOGGER.error("Could not identify the location to store the backup");
            throw new BadRequestException("Backup Location is empty. Datalake backup is not triggered.");
        }
    }

    public PlatformVmtypesResponse getVmTypesByCredential(String credentialCrn, String region, String platformVariant,
            CdpResourceType resourceType, String availabilityZone, String architecture) {
        try {
            return credentialPlatformResourceEndpoint.getVmTypesByCredential(null, credentialCrn, region, platformVariant, availabilityZone,
                    resourceType, architecture);
        } catch (WebApplicationException e) {
            throw webApplicationExceptionHandler.handleException(e);
        }
    }

    public DetailedEnvironmentResponse waitAndGetEnvironment(Long sdxId) {
        PollingConfig pollingConfig = new PollingConfig(sleepTimeInSec, TimeUnit.SECONDS,
                durationInMinutes, TimeUnit.MINUTES);
        return waitAndGetEnvironment(sdxId, pollingConfig, EnvironmentStatus::isAvailable);
    }

    public DetailedEnvironmentResponse waitNetworkAndGetEnvironment(Long sdxId) {
        PollingConfig pollingConfig = new PollingConfig(sleepTimeInSec, TimeUnit.SECONDS,
                durationInMinutes, TimeUnit.MINUTES);
        return waitAndGetEnvironment(sdxId, pollingConfig, EnvironmentStatus::isNetworkCreationFinished);
    }

    public DetailedEnvironmentResponse waitAndGetEnvironment(Long sdxId, PollingConfig pollingConfig, Function<EnvironmentStatus, Boolean> statusCheck) {
        Optional<SdxCluster> sdxClusterOptional = sdxClusterRepository.findById(sdxId);
        if (sdxClusterOptional.isPresent()) {
            SdxCluster sdxCluster = sdxClusterOptional.get();
            sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.WAIT_FOR_ENVIRONMENT, "Waiting for environment creation", sdxCluster);
            DetailedEnvironmentResponse environmentResponse = Polling.waitPeriodly(pollingConfig.getSleepTime(), pollingConfig.getSleepTimeUnit())
                    .stopIfException(pollingConfig.getStopPollingIfExceptionOccurred())
                    .stopAfterDelay(pollingConfig.getDuration(), pollingConfig.getDurationTimeUnit())
                    .run(() -> {
                        if (PollGroup.CANCELLED.equals(DatalakeInMemoryStateStore.get(sdxCluster.getId()))) {
                            LOGGER.info("Environment wait polling cancelled in inmemory store, id: " + sdxCluster.getId());
                            return AttemptResults.breakFor("Environment wait polling cancelled in inmemory store, id: " + sdxCluster.getId());
                        }
                        LOGGER.info("Creation polling environment for environment status: '{}' in '{}' env",
                                sdxCluster.getClusterName(), sdxCluster.getEnvName());
                        DetailedEnvironmentResponse environment = getDetailedEnvironmentResponse(sdxCluster.getEnvCrn());
                        LOGGER.info("Response from environment: {}", JsonUtil.writeValueAsString(environment));
                        if (statusCheck.apply(environment.getEnvironmentStatus())) {
                            return AttemptResults.finishWith(environment);
                        } else {
                            if (environment.getEnvironmentStatus().isFailed()) {
                                return AttemptResults.breakFor("Environment creation failed: " + sdxCluster.getEnvName());
                            } else if (environment.getEnvironmentStatus().isStopInProgressOrStopped()) {
                                return AttemptResults.breakFor("Environment is in stopped status: " + sdxCluster.getEnvName());
                            } else if (environment.getEnvironmentStatus().isStartInProgress()) {
                                return AttemptResults.breakFor("The environment is starting. Please wait until finished: " + sdxCluster.getEnvName());
                            } else {
                                return AttemptResults.justContinue();
                            }
                        }
                    });
            return environmentResponse;
        } else {
            throw notFound("SDX cluster", sdxId).get();
        }
    }

    private DetailedEnvironmentResponse getDetailedEnvironmentResponse(String environmentCrn) {
        return getByCrn(environmentCrn);
    }

    public DetailedEnvironmentResponse getDetailedEnvironmentResponseByName(String environmentName) {
        return getByName(environmentName);
    }

    @Override
    public String getResourceCrnByResourceName(String resourceName) {
        List<ResourceWithId> dlsByEnv = sdxClusterRepository
                .findAuthorizationResourcesByAccountIdAndEnvName(ThreadBasedUserCrnProvider.getAccountId(), resourceName);
        return dlsByEnv
                .stream()
                .filter(dl -> dl.getParentResourceCrn().isPresent())
                .findFirst().orElseThrow(() -> new NotFoundException(String.format("There is no environment with name %s", resourceName)))
                .getParentResourceCrn().orElseThrow(() -> new NotFoundException(String.format("There is no environment with name %s", resourceName)));
    }

    @Override
    public List<String> getResourceCrnListByResourceNameList(List<String> resourceNames) {
        List<ResourceWithId> dlsByEnvs = sdxClusterRepository
                .findAuthorizationResourcesByAccountIdAndEnvNames(ThreadBasedUserCrnProvider.getAccountId(), resourceNames);
        return dlsByEnvs.stream()
                .map(Resource::getParentResourceCrn)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Optional<String>> getNamesByCrnsForMessage(Collection<String> crns) {
        Set<SdxClusterView> dlByEnvCrns = sdxClusterViewRepository.findByAccountIdAndEnvCrnIn(ThreadBasedUserCrnProvider.getAccountId(), crns);
        Map<String, Optional<String>> result = Maps.newHashMap();
        dlByEnvCrns.stream()
                .filter(dl -> StringUtils.isNotBlank(dl.getEnvCrn()) && StringUtils.isNotBlank(dl.getEnvName()))
                .forEach(dl -> result.putIfAbsent(dl.getEnvCrn(), Optional.of(dl.getEnvName())));
        return result;
    }

    public boolean isGovCloudEnvironment(DetailedEnvironmentResponse environmentResponse) {
        return environmentResponse.getCredential() != null &&
                environmentResponse.getCredential().getGovCloud() != null &&
                environmentResponse.getCredential().getGovCloud().booleanValue();
    }
}
