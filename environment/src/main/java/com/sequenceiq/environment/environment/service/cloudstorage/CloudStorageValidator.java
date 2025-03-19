package com.sequenceiq.environment.environment.service.cloudstorage;

import static com.sequenceiq.common.model.CredentialType.ENVIRONMENT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.providerservices.CloudProviderServicesV4Endopint;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.model.BackupOperationType;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageValidateRequest;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageValidateResponse;
import com.sequenceiq.common.api.cloudstorage.CloudStorageRequest;
import com.sequenceiq.common.api.cloudstorage.StorageIdentityBase;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.request.LoggingRequest;
import com.sequenceiq.common.api.telemetry.request.TelemetryRequest;
import com.sequenceiq.common.model.CloudIdentityType;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentCloudStorageValidationRequest;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.service.CredentialService;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCloudCredentialConverter;

@Service
public class CloudStorageValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudStorageValidator.class);

    private final CredentialService credentialService;

    private final CloudProviderServicesV4Endopint cloudProviderServicesV4Endpoint;

    private final CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    public CloudStorageValidator(CredentialService credentialService,
            CloudProviderServicesV4Endopint cloudProviderServicesV4Endpoint,
            CredentialToCloudCredentialConverter credentialToCloudCredentialConverter) {
        this.credentialService = credentialService;
        this.cloudProviderServicesV4Endpoint = cloudProviderServicesV4Endpoint;
        this.credentialToCloudCredentialConverter = credentialToCloudCredentialConverter;
    }

    public ObjectStorageValidateResponse validateCloudStorage(String accountId,
            EnvironmentCloudStorageValidationRequest environmentCloudStorageValidationRequest) {
        Credential credential = credentialService.getByCrnForAccountId(environmentCloudStorageValidationRequest.getCredentialCrn(), accountId, ENVIRONMENT,
                false);
        CloudCredential cloudCredential = credentialToCloudCredentialConverter.convert(credential);
        CloudStorageRequest cloudStorageRequest = new CloudStorageRequest();

        TelemetryRequest telemetryRequest = environmentCloudStorageValidationRequest.getTelemetry();
        boolean loggingConfigured = isLoggingConfigured(telemetryRequest);

        if (loggingConfigured) {
            LOGGER.debug("Cloud storage logging is enabled.");
            addLogIdentity(cloudStorageRequest, telemetryRequest);
        }

        ObjectStorageValidateRequest.Builder objectStorageValidateBuilder = ObjectStorageValidateRequest.builder()
                .withCloudPlatform(credential.getCloudPlatform())
                .withCredential(cloudCredential)
                .withBackupOperationType(BackupOperationType.NONE)
                .withCloudStorageRequest(cloudStorageRequest);
        if (loggingConfigured) {
            objectStorageValidateBuilder.withLogsLocationBase(telemetryRequest.getLogging().getStorageLocation());
        }
        if (environmentCloudStorageValidationRequest.getBackup() != null) {
            objectStorageValidateBuilder.withBackupLocationBase(environmentCloudStorageValidationRequest.getBackup().getStorageLocation());
        }
        ObjectStorageValidateRequest objectStorageValidateRequest = objectStorageValidateBuilder.build();
        return ThreadBasedUserCrnProvider.doAsInternalActor(
                () -> cloudProviderServicesV4Endpoint.validateObjectStorage(objectStorageValidateRequest));
    }

    private void addLogIdentity(CloudStorageRequest cloudStorageRequest,
            TelemetryRequest telemetryRequest) {
        StorageIdentityBase log = new StorageIdentityBase();
        log.setType(CloudIdentityType.LOG);
        LoggingRequest logging = telemetryRequest.getLogging();
        if (logging.getS3() != null) {
            LOGGER.debug("Setting up S3 location for logging configuration.");
            log.setS3(logging.getS3());
        } else if (logging.getAdlsGen2() != null) {
            LOGGER.debug("Setting up Adls Gen2 location for logging configuration.");
            log.setAdlsGen2(logging.getAdlsGen2());
        } else if (logging.getGcs() != null) {
            LOGGER.debug("Setting up GCS location for logging configuration.");
            log.setGcs(logging.getGcs());
        } else if (logging.getCloudwatch() != null) {
            LOGGER.debug("Cloudwatch will act as S3 storage identity!");
            S3CloudStorageV1Parameters s3CloudwatchParams = new S3CloudStorageV1Parameters();
            s3CloudwatchParams.setInstanceProfile(logging.getCloudwatch().getInstanceProfile());
            log.setS3(s3CloudwatchParams);
        }
        cloudStorageRequest.getIdentities().add(log);
    }

    private boolean isLoggingConfigured(TelemetryRequest telemetryRequest) {
        return telemetryRequest != null && telemetryRequest.getLogging() != null;
    }

}
