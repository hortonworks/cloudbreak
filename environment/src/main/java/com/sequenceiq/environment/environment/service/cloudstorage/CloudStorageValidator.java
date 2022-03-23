package com.sequenceiq.environment.environment.service.cloudstorage;

import static com.sequenceiq.common.model.CredentialType.ENVIRONMENT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.providerservices.CloudProviderServicesV4Endopint;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageValidateRequest;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageValidateResponse;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.common.api.cloudstorage.CloudStorageRequest;
import com.sequenceiq.common.api.cloudstorage.StorageIdentityBase;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.request.LoggingRequest;
import com.sequenceiq.common.api.telemetry.request.TelemetryRequest;
import com.sequenceiq.common.model.CloudIdentityType;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentCloudStorageValidationRequest;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.service.CredentialService;

@Service
public class CloudStorageValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudStorageValidator.class);

    private final CredentialService credentialService;

    private final CloudProviderServicesV4Endopint cloudProviderServicesV4Endpoint;

    private final RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public CloudStorageValidator(CredentialService credentialService,
        CloudProviderServicesV4Endopint cloudProviderServicesV4Endpoint,
        RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        this.credentialService = credentialService;
        this.cloudProviderServicesV4Endpoint = cloudProviderServicesV4Endpoint;
        this.regionAwareInternalCrnGeneratorFactory = regionAwareInternalCrnGeneratorFactory;
    }

    public ObjectStorageValidateResponse validateCloudStorage(String accountId,
            EnvironmentCloudStorageValidationRequest environmentCloudStorageValidationRequest) {
        Credential credential = credentialService.getByCrnForAccountId(environmentCloudStorageValidationRequest.getCredentialCrn(), accountId, ENVIRONMENT,
                false);
        String attributes = credential.getAttributes();
        CloudCredential cloudCredential = new CloudCredential(credential.getResourceCrn(), credential.getName(), new Json(attributes).getMap(),
                credential.getAccountId(), credential.isVerifyPermissions());
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
                .withCloudStorageRequest(cloudStorageRequest);
        if (loggingConfigured) {
            objectStorageValidateBuilder.withLogsLocationBase(telemetryRequest.getLogging().getStorageLocation());
        }
        if (environmentCloudStorageValidationRequest.getBackup() != null) {
            objectStorageValidateBuilder.withBackupLocationBase(environmentCloudStorageValidationRequest.getBackup().getStorageLocation());
        }
        ObjectStorageValidateRequest objectStorageValidateRequest = objectStorageValidateBuilder.build();
        return ThreadBasedUserCrnProvider.doAsInternalActor(
                regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                () ->
                cloudProviderServicesV4Endpoint.validateObjectStorage(objectStorageValidateRequest));
    }

    private void addLogIdentity(CloudStorageRequest cloudStorageRequest,
            TelemetryRequest telemetryRequest) {
        StorageIdentityBase log = new StorageIdentityBase();
        log.setType(CloudIdentityType.LOG);
        LoggingRequest logging = telemetryRequest.getLogging();
        if (logging.getS3() != null) {
            log.setS3(logging.getS3());
        } else if (logging.getAdlsGen2() != null) {
            log.setAdlsGen2(logging.getAdlsGen2());
        } else if (logging.getGcs() != null) {
            log.setGcs(logging.getGcs());
        } else if (logging.getCloudwatch() != null) {
            LOGGER.debug("Cloudwatch will act as s3 storage identity!");
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
