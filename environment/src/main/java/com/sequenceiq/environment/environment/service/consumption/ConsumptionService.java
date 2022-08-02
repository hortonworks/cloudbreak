package com.sequenceiq.environment.environment.service.consumption;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.consumption.api.v1.consumption.model.common.ResourceType;
import com.sequenceiq.consumption.api.v1.consumption.model.request.StorageConsumptionRequest;
import com.sequenceiq.environment.environment.dto.EnvironmentBackup;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDtoBase;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentLogging;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentTelemetry;

@Service
public class ConsumptionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumptionService.class);

    private final EntitlementService entitlementService;

    private final ConsumptionClientService consumptionClientService;

    private final boolean consumptionEnabled;

    public ConsumptionService(
            EntitlementService entitlementService,
            ConsumptionClientService consumptionClientService,
            @Value("${environment.consumption.enabled:false}") boolean consumptionEnabled) {
        this.entitlementService = entitlementService;
        this.consumptionClientService = consumptionClientService;
        this.consumptionEnabled = consumptionEnabled;
    }

    public void scheduleStorageConsumptionCollectionIfNeeded(EnvironmentDto environmentDto) {
        String accountId = environmentDto.getAccountId();
        if (consumptionEnabled && entitlementService.isCdpSaasEnabled(accountId)) {
            scheduleStorageConsumptionCollection(environmentDto);
        } else {
            LOGGER.info("Skipping storage consumption collection scheduling because " +
                    (consumptionEnabled ? String.format("CDP_SAAS entitlement is missing for account '%s'", accountId) : "it is disabled for the deployment"));
        }
    }

    private void scheduleStorageConsumptionCollection(EnvironmentDto environmentDto) {
        String loggingStorageLocation = Optional.of(environmentDto)
                .map(EnvironmentDtoBase::getTelemetry)
                .map(EnvironmentTelemetry::getLogging)
                .map(EnvironmentLogging::getStorageLocation)
                .orElse(null);
        if (!Strings.isNullOrEmpty(loggingStorageLocation)) {
            scheduleStorageConsumptionCollectionForStorageLocation(environmentDto, loggingStorageLocation, "logging");
        } else {
            LOGGER.warn("Skipping storage consumption collection scheduling for logging storage location because it is not provided: telemetry='{}'",
                    environmentDto.getTelemetry());
        }

        String backupStorageLocation = Optional.of(environmentDto)
                .map(EnvironmentDtoBase::getBackup)
                .map(EnvironmentBackup::getStorageLocation)
                .orElse(null);
        if (!Strings.isNullOrEmpty(backupStorageLocation)) {
            if (!backupStorageLocation.equals(loggingStorageLocation)) {
                scheduleStorageConsumptionCollectionForStorageLocation(environmentDto, backupStorageLocation, "backup");
            } else {
                LOGGER.warn("Skipping storage consumption collection scheduling for backup storage location because it matches the logging storage location");
            }
        } else {
            LOGGER.warn("Skipping storage consumption collection scheduling for backup storage location because it is not provided: backup='{}'",
                    environmentDto.getBackup());
        }
    }

    private void scheduleStorageConsumptionCollectionForStorageLocation(EnvironmentDto environmentDto, String storageLocation, String locationKind) {
        StorageConsumptionRequest request = new StorageConsumptionRequest();
        String resourceCrn = environmentDto.getResourceCrn();
        request.setEnvironmentCrn(resourceCrn);
        request.setMonitoredResourceCrn(resourceCrn);
        request.setMonitoredResourceName(environmentDto.getName());
        request.setMonitoredResourceType(ResourceType.ENVIRONMENT);
        request.setStorageLocation(storageLocation);
        String accountId = environmentDto.getAccountId();
        LOGGER.info("Executing storage consumption collection scheduling for {} storage location: account '{}' and request '{}'", locationKind, accountId,
                request);
        consumptionClientService.scheduleStorageConsumptionCollection(accountId, request);
    }

    public void unscheduleStorageConsumptionCollectionIfNeeded(EnvironmentDto environmentDto) {
        String accountId = environmentDto.getAccountId();
        if (consumptionEnabled && entitlementService.isCdpSaasEnabled(accountId)) {
            unscheduleStorageConsumptionCollection(environmentDto);
        } else {
            LOGGER.info("Skipping storage consumption collection unscheduling because " +
                    (consumptionEnabled ? String.format("CDP_SAAS entitlement is missing for account '%s'", accountId) : "it is disabled for the deployment"));
        }
    }

    private void unscheduleStorageConsumptionCollection(EnvironmentDto environmentDto) {
        String accountId = environmentDto.getAccountId();
        String monitoredResourceCrn = environmentDto.getResourceCrn();

        String loggingStorageLocation = Optional.of(environmentDto)
                .map(EnvironmentDtoBase::getTelemetry)
                .map(EnvironmentTelemetry::getLogging)
                .map(EnvironmentLogging::getStorageLocation)
                .orElse(null);
        if (!Strings.isNullOrEmpty(loggingStorageLocation)) {
            unscheduleStorageConsumptionCollectionForStorageLocation(accountId, monitoredResourceCrn, loggingStorageLocation, "logging");
        } else {
            LOGGER.warn("Skipping storage consumption collection unscheduling for logging storage location because it is not provided: telemetry='{}'",
                    environmentDto.getTelemetry());
        }

        String backupStorageLocation = Optional.of(environmentDto)
                .map(EnvironmentDtoBase::getBackup)
                .map(EnvironmentBackup::getStorageLocation)
                .orElse(null);
        if (!Strings.isNullOrEmpty(backupStorageLocation)) {
            if (!backupStorageLocation.equals(loggingStorageLocation)) {
                unscheduleStorageConsumptionCollectionForStorageLocation(accountId, monitoredResourceCrn, backupStorageLocation, "backup");
            } else {
                LOGGER.warn("Skipping storage consumption collection unscheduling for backup storage location because it matches the logging storage location");
            }
        } else {
            LOGGER.warn("Skipping storage consumption collection unscheduling for backup storage location because it is not provided: backup='{}'",
                    environmentDto.getBackup());
        }
    }

    private void unscheduleStorageConsumptionCollectionForStorageLocation(String accountId, String monitoredResourceCrn, String storageLocation,
            String locationKind) {
        LOGGER.info("Executing storage consumption collection unscheduling for {} storage location: account '{}', resource '{}' and storage location '{}'",
                locationKind, accountId, monitoredResourceCrn, storageLocation);
        consumptionClientService.unscheduleStorageConsumptionCollection(accountId, monitoredResourceCrn, storageLocation);
    }

}
