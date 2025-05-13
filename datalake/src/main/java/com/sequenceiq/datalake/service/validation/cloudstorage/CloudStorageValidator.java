package com.sequenceiq.datalake.service.validation.cloudstorage;

import static com.sequenceiq.environment.api.v1.environment.model.base.IdBrokerMappingSource.MOCK;
import static com.sequenceiq.environment.api.v1.environment.model.request.azure.ResourceGroupUsage.SINGLE;
import static com.sequenceiq.environment.api.v1.environment.model.request.azure.ResourceGroupUsage.SINGLE_WITH_DEDICATED_STORAGE_ACCOUNT;

import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.providerservices.CloudProviderServicesV4Endopint;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.BackupOperationType;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.base.ResponseStatus;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageValidateRequest;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageValidateResponse;
import com.sequenceiq.cloudbreak.common.anonymizer.AnonymizerUtil;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.util.VersionComparator;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.common.api.cloudstorage.CloudStorageRequest;
import com.sequenceiq.common.api.cloudstorage.StorageLocationBase;
import com.sequenceiq.common.api.telemetry.base.LoggingBase;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import com.sequenceiq.datalake.service.validation.converter.CredentialResponseToCloudCredentialConverter;
import com.sequenceiq.environment.api.v1.environment.model.base.CloudStorageValidation;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureResourceGroup;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Component
public class CloudStorageValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudStorageValidator.class);

    private static final String CLOUDERA_RUNTIME_7_2_17 = "7.2.17";

    private final CredentialResponseToCloudCredentialConverter credentialResponseToCloudCredentialConverter;

    private final EntitlementService entitlementService;

    private final CloudProviderServicesV4Endopint cloudProviderServicesV4Endpoint;

    public CloudStorageValidator(CredentialResponseToCloudCredentialConverter credentialResponseToCloudCredentialConverter,
            EntitlementService entitlementService,
            CloudProviderServicesV4Endopint cloudProviderServicesV4Endpoint) {
        this.credentialResponseToCloudCredentialConverter = credentialResponseToCloudCredentialConverter;
        this.entitlementService = entitlementService;
        this.cloudProviderServicesV4Endpoint = cloudProviderServicesV4Endpoint;
    }

    public void validate(CloudStorageRequest cloudStorageRequest, DetailedEnvironmentResponse environment,
            ValidationResult.ValidationResultBuilder validationResultBuilder) {
        if (CloudStorageValidation.DISABLED.equals(environment.getCloudStorageValidation())) {
            LOGGER.info("Due to cloud storage validation not being enabled, not validating cloudStorageRequest: {}",
                    JsonUtil.writeValueAsStringSilent(cloudStorageRequest));
            return;
        }

        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        if (!entitlementService.cloudStorageValidationEnabled(accountId)) {
            LOGGER.info("Cloud storage validation entitlement is missing, not validating cloudStorageRequest: {}",
                    JsonUtil.writeValueAsStringSilent(cloudStorageRequest));
            return;
        }

        LOGGER.info("Validating cloudStorageRequest: {}", JsonUtil.writeValueAsStringSilent(cloudStorageRequest));
        if (cloudStorageRequest != null) {
            CloudCredential cloudCredential = credentialResponseToCloudCredentialConverter.convert(environment.getCredential());

            ObjectStorageValidateRequest request = createObjectStorageValidateRequest(cloudCredential, cloudStorageRequest, environment);
            ObjectStorageValidateResponse response = ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> cloudProviderServicesV4Endpoint.validateObjectStorage(request));

            LOGGER.info("ValidateObjectStorage: request: {}, response: {}", AnonymizerUtil.anonymize(JsonUtil.writeValueAsStringSilent(request)),
                    JsonUtil.writeValueAsStringSilent(response));

            if (ResponseStatus.ERROR.equals(response.getStatus())) {
                validationResultBuilder.error(response.getError());
            } else if (StringUtils.isNotBlank(response.getError())) {
                validationResultBuilder.warning(response.getError());
            }
        }
    }

    /**
     * Validates if the required permissions are configured on the backup location on cloud provider.
     *
     * @param cloudStorageRequest     Information on storage configuration used for datalake.
     * @param backupOperationType     Type of the operation.
     * @param environment             Details of the environment on which validation is performed.
     * @param customBackupLocation    Used when a backup location is not the default one.
     * @param clouderaRuntime         Cloudera runtime version.
     * @param validationResultBuilder Builder to gather the failures.
     */
    public void validateBackupLocation(CloudStorageRequest cloudStorageRequest, BackupOperationType backupOperationType,
            DetailedEnvironmentResponse environment, String customBackupLocation, String clouderaRuntime,
            ValidationResultBuilder validationResultBuilder) {
        String backupLocation = !Strings.isNullOrEmpty(customBackupLocation) ? customBackupLocation : getBackupLocationBase(environment);
        LOGGER.info("Validating backup Location: {}", backupLocation);
        CloudCredential cloudCredential = credentialResponseToCloudCredentialConverter.convert(environment.getCredential());
        List<StorageLocationBase> locations = cloudStorageRequest.getLocations();
        ObjectStorageValidateRequest request = createBackupLocationValidateRequest(cloudCredential, backupOperationType, cloudStorageRequest,
                environment, backupLocation, clouderaRuntime);
        ObjectStorageValidateResponse response = ThreadBasedUserCrnProvider.doAsInternalActor(
                () -> cloudProviderServicesV4Endpoint.validateObjectStorage(request));
        cloudStorageRequest.setLocations(locations);

        LOGGER.info("Validate backup storage: request: {}, response: {}", AnonymizerUtil.anonymize(JsonUtil.writeValueAsStringSilent(request)),
                JsonUtil.writeValueAsStringSilent(response));

        if (ResponseStatus.ERROR.equals(response.getStatus())) {
            validationResultBuilder.error(response.getError());
        } else if (StringUtils.isNotBlank(response.getError())) {
            validationResultBuilder.warning(response.getError());
        }
    }

    private String getSingleResourceGroupName(DetailedEnvironmentResponse env) {
        return Optional.ofNullable(env.getAzure())
                .map(AzureEnvironmentParameters::getResourceGroup)
                .filter(rg -> EnumSet.of(SINGLE, SINGLE_WITH_DEDICATED_STORAGE_ACCOUNT).contains(rg.getResourceGroupUsage()))
                .map(AzureResourceGroup::getName)
                .orElse(null);
    }

    private ObjectStorageValidateRequest createObjectStorageValidateRequest(
            CloudCredential credential,
            CloudStorageRequest cloudStorageRequest,
            DetailedEnvironmentResponse environment) {
        ObjectStorageValidateRequest.Builder result = ObjectStorageValidateRequest.builder()
                .withCloudPlatform(environment.getCloudPlatform())
                .withCredential(credential)
                .withCloudStorageRequest(cloudStorageRequest)
                .withLogsLocationBase(getLogsLocationBase(environment))
                .withBackupLocationBase(getBackupLocationBase(environment))
                .withBackupOperationType(BackupOperationType.NONE)
                .withAzureParameters(getSingleResourceGroupName(environment));

        if (environment.getIdBrokerMappingSource() == MOCK) {
            result.withMockSettings(environment.getLocation().getName(), environment.getAdminGroupName());
        }
        return result.build();
    }

    private ObjectStorageValidateRequest createBackupLocationValidateRequest(
            CloudCredential credential, BackupOperationType backupOperationType,
            CloudStorageRequest cloudStorageRequest,
            DetailedEnvironmentResponse environment, String backupLocation,
            String clouderaRuntime) {
        cloudStorageRequest.setLocations(Collections.emptyList());
        ObjectStorageValidateRequest.Builder result = ObjectStorageValidateRequest.builder()
                .withCloudPlatform(environment.getCloudPlatform())
                .withCredential(credential)
                .withCloudStorageRequest(cloudStorageRequest)
                .withBackupLocationBase(backupLocation)
                .withBackupOperationType(backupOperationType)
                .withAzureParameters(getSingleResourceGroupName(environment))
                .withSkipLogRoleValidationforBackup(!isLogRoleValidationRequiredForBackup(clouderaRuntime));

        if (environment.getIdBrokerMappingSource() == MOCK) {
            result.withMockSettings(environment.getLocation().getName(), environment.getAdminGroupName());
        }
        return result.build();
    }

    private String getLogsLocationBase(DetailedEnvironmentResponse env) {
        return Optional.ofNullable(env.getTelemetry())
                .map(TelemetryResponse::getLogging)
                .map(LoggingBase::getStorageLocation)
                .orElse(null);
    }

    private String getBackupLocationBase(DetailedEnvironmentResponse environment) {
        return Optional.ofNullable(environment.getBackupLocation())
                .orElse(getLogsLocationBase(environment));
    }

    private boolean isLogRoleValidationRequiredForBackup(String clouderaRuntime) {
        Comparator<Versioned> versionComparator = new VersionComparator();
        return versionComparator.compare(() -> clouderaRuntime, () -> CLOUDERA_RUNTIME_7_2_17) < 0;
    }
}
