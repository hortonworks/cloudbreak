package com.sequenceiq.datalake.service.validation.cloudstorage;

import static com.sequenceiq.environment.api.v1.environment.model.base.IdBrokerMappingSource.MOCK;
import static com.sequenceiq.environment.api.v1.environment.model.request.azure.ResourceGroupUsage.SINGLE;
import static com.sequenceiq.environment.api.v1.environment.model.request.azure.ResourceGroupUsage.SINGLE_WITH_DEDICATED_STORAGE_ACCOUNT;

import java.util.EnumSet;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.providerservices.CloudProviderServicesV4Endopint;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.base.ResponseStatus;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageValidateRequest;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageValidateResponse;
import com.sequenceiq.cloudbreak.common.anonymizer.AnonymizerUtil;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.common.api.backup.response.BackupResponse;
import com.sequenceiq.common.api.cloudstorage.CloudStorageRequest;
import com.sequenceiq.common.api.telemetry.base.LoggingBase;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import com.sequenceiq.datalake.entity.Credential;
import com.sequenceiq.datalake.service.validation.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.environment.api.v1.environment.model.base.CloudStorageValidation;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureResourceGroup;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Component
public class CloudStorageValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudStorageValidator.class);

    private final CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    private final EntitlementService entitlementService;

    private final SecretService secretService;

    private final CloudProviderServicesV4Endopint cloudProviderServicesV4Endpoint;

    private final RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public CloudStorageValidator(CredentialToCloudCredentialConverter credentialToCloudCredentialConverter,
            EntitlementService entitlementService, SecretService secretService,
            CloudProviderServicesV4Endopint cloudProviderServicesV4Endpoint,
            RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        this.credentialToCloudCredentialConverter = credentialToCloudCredentialConverter;
        this.entitlementService = entitlementService;
        this.secretService = secretService;
        this.cloudProviderServicesV4Endpoint = cloudProviderServicesV4Endpoint;
        this.regionAwareInternalCrnGeneratorFactory = regionAwareInternalCrnGeneratorFactory;
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
            Credential credential = getCredential(environment);
            CloudCredential cloudCredential = credentialToCloudCredentialConverter.convert(credential);

            ObjectStorageValidateRequest request = createObjectStorageValidateRequest(cloudCredential, cloudStorageRequest, environment);
            ObjectStorageValidateResponse response = ThreadBasedUserCrnProvider.doAsInternalActor(
                    regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
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
                .withAzureParameters(getSingleResourceGroupName(environment));
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
        return Optional.ofNullable(environment.getBackup())
                .map(BackupResponse::getStorageLocation)
                .orElse(null);
    }

    private Credential getCredential(DetailedEnvironmentResponse environment) {
        return new Credential(environment.getCloudPlatform(),
                environment.getCredential().getName(),
                secretService.getByResponse(environment.getCredential().getAttributes()),
                environment.getCredential().getCrn(),
                environment.getCredential().getAccountId());
    }
}
