package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.cloudbreak.service.identitymapping.AccountMappingSubject.DATA_ACCESS_USERS;
import static com.sequenceiq.cloudbreak.service.identitymapping.AccountMappingSubject.RANGER_AUDIT_USERS;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.providerservices.CloudProviderServicesV4Endopint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.cloud.model.BackupOperationType;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageValidateRequest;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageValidateResponse;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.common.api.cloudstorage.AccountMappingBase;
import com.sequenceiq.common.api.cloudstorage.CloudStorageRequest;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.events.EventSenderService;
import com.sequenceiq.datalake.service.EnvironmentClientService;
import com.sequenceiq.datalake.service.validation.cloudstorage.CloudStorageValidator;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.sdx.api.model.SdxCloudStorageRequest;

@Service
public class StorageValidationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageValidationService.class);

    @Inject
    private SecretService secretService;

    @Inject
    private CloudStorageManifester cloudStorageManifester;

    @Inject
    private CloudProviderServicesV4Endopint cloudProviderServicesV4Endpoint;

    @Inject
    private EnvironmentClientService environmentClientService;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Inject
    private CloudStorageValidator cloudStorageValidator;

    @Inject
    private SdxNotificationService sdxNotificationService;

    @Inject
    private EventSenderService eventSenderService;

    @Inject
    private EnvironmentService environmentService;

    public ObjectStorageValidateResponse validateObjectStorage(String credentialCrn, SdxCloudStorageRequest sdxCloudStorageRequest, String blueprintName,
            String clusterName, String dataAccessRole, String rangerAuditRole) {
        CredentialResponse credentialResponse = environmentClientService.getCredentialByCrn(credentialCrn);
        String attributes = secretService.getByResponse(credentialResponse.getAttributes());
        CloudCredential cloudCredential = new CloudCredential(credentialResponse.getCrn(), credentialResponse.getName(), new Json(attributes).getMap(),
                credentialResponse.getAccountId(), credentialResponse.isVerifyPermissions());
        CloudStorageRequest cloudStorageRequest = cloudStorageManifester.initSdxCloudStorageRequest(credentialResponse.getCloudPlatform(),
                blueprintName, clusterName, sdxCloudStorageRequest);
        AccountMappingBase accountMapping = new AccountMappingBase();
        Map<String, String> userMapping = getUserMapping(dataAccessRole, rangerAuditRole);
        accountMapping.setUserMappings(userMapping);
        cloudStorageRequest.setAccountMapping(accountMapping);
        ObjectStorageValidateRequest objectStorageValidateRequest = ObjectStorageValidateRequest.builder()
                .withCloudPlatform(credentialResponse.getCloudPlatform())
                .withCredential(cloudCredential)
                .withCloudStorageRequest(cloudStorageRequest)
                .withBackupOperationType(BackupOperationType.NONE)
                .build();
        return ThreadBasedUserCrnProvider.doAsInternalActor(
                regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                () -> cloudProviderServicesV4Endpoint.validateObjectStorage(objectStorageValidateRequest));
    }

    public ValidationResult validateBackupStorage(SdxCluster sdxCluster, BackupOperationType backupOperationType, String backupLocation) {
        DetailedEnvironmentResponse environmentResponse = environmentService.getDetailedEnvironmentResponseByName(sdxCluster.getEnvName());
        CloudStorageRequest cloudStorageRequest = null;
        try {
            if (!StringUtils.isBlank(sdxCluster.getStackRequestToCloudbreak())) {
                StackV4Request stackV4Request = JsonUtil.readValue(sdxCluster.getStackRequestToCloudbreak(), StackV4Request.class);
                cloudStorageRequest = stackV4Request.getCluster().getCloudStorage();
                LOGGER.info("Validating backup Storage: {}", JsonUtil.writeValueAsStringSilent(cloudStorageRequest));
            }
        } catch (IOException ioException) {
            throw new BadRequestException("Failed to validate backup storage");
        }

        if (environmentResponse == null || cloudStorageRequest == null) {
            throw new BadRequestException("Failed to validate backup storage");
        }
        ValidationResult.ValidationResultBuilder validationResultBuilder = new ValidationResult.ValidationResultBuilder();
        try {
            cloudStorageValidator.validateBackupLocation(cloudStorageRequest, backupOperationType, environmentResponse, backupLocation, validationResultBuilder);
        } catch (Exception e) {
            String message = String.format("Error occured during object storage validation, validation skipped. Error: %s", e.getMessage());
            LOGGER.warn(message);
            sdxNotificationService.send(ResourceEvent.SDX_VALIDATION_FAILED_AND_SKIPPED, Set.of(e.getMessage()), sdxCluster);
        }
        ValidationResult validationResult = validationResultBuilder.build();
        if (validationResult.hasError()) {
            LOGGER.error(validationResult.getFormattedErrors());
            eventSenderService.sendEventAndNotification(sdxCluster,
                    ResourceEvent.SDX_VALIDATION_FAILED, Set.of(validationResult.getFormattedErrors()));
        }
        if (validationResult.hasWarning()) {
            LOGGER.info(validationResult.getFormattedWarnings());
            eventSenderService.sendEventAndNotification(sdxCluster,
                    ResourceEvent.SDX_VALIDATION_FAILED_AND_SKIPPED, Set.of(validationResult.getFormattedWarnings()));
        }
        return validationResult;
    }

    public void validateCloudStorage(String cloudPlatform, SdxCloudStorageRequest cloudStorage) {
        if (CloudPlatform.AWS.name().equalsIgnoreCase(cloudPlatform)) {
            if (!isS3AuthenticationConfigured(cloudStorage)) {
                throw new BadRequestException("instance profile must be defined for S3");
            }
            if (!cloudStorage.getBaseLocation().startsWith(FileSystemType.S3.getProtocol() + "://")) {
                throw new BadRequestException("AWS baselocation missing protocol. please specify s3a://");
            }
        } else  if (CloudPlatform.AZURE.name().equalsIgnoreCase(cloudPlatform)) {
            if (!isAzureAuthenticationConfigured(cloudStorage)) {
                throw new BadRequestException("managed identity or account key and account name must be defined for ABFS");
            }
            if (!cloudStorage.getBaseLocation().startsWith(FileSystemType.ADLS_GEN_2.getProtocol() + "://")) {
                throw new BadRequestException("AZURE baselocation missing protocol. please specify abfs://");
            }
        } else if (CloudPlatform.GCP.name().equalsIgnoreCase(cloudPlatform)) {
            if (!isGcsAuthenticationConfigured(cloudStorage)) {
                throw new BadRequestException("service account email must be defined for GCS");
            }
            if (!cloudStorage.getBaseLocation().startsWith(FileSystemType.GCS.getProtocol() + "://")) {
                throw new BadRequestException("GCP baselocation missing protocol. please specify gcs://");
            }
        }
    }

    private boolean isS3AuthenticationConfigured(SdxCloudStorageRequest cloudStorage) {
        return cloudStorage.getS3() != null && !StringUtils.isEmpty(cloudStorage.getS3().getInstanceProfile());
    }

    private boolean isGcsAuthenticationConfigured(SdxCloudStorageRequest cloudStorage) {
        return cloudStorage.getGcs() != null && !StringUtils.isEmpty(cloudStorage.getGcs().getServiceAccountEmail());
    }

    private boolean isAzureAuthenticationConfigured(SdxCloudStorageRequest cloudStorage) {
        return cloudStorage.getAdlsGen2() != null
                && (!StringUtils.isEmpty(cloudStorage.getAdlsGen2().getManagedIdentity())
                || (!StringUtils.isEmpty(cloudStorage.getAdlsGen2().getAccountKey()) && !StringUtils.isEmpty(cloudStorage.getAdlsGen2().getAccountName())));
    }

    private Map<String, String> getUserMapping(String dataAccessRole, String rangerAuditRole) {
        Map<String, String> userMapping = new HashMap<>();
        if (dataAccessRole != null) {
            for (String dataAccessUser : DATA_ACCESS_USERS) {
                userMapping.put(dataAccessUser, dataAccessRole);
            }
        }
        if (rangerAuditRole != null) {
            for (String rangerAuditUser : RANGER_AUDIT_USERS) {
                userMapping.put(rangerAuditUser, rangerAuditRole);
            }
        }
        return userMapping;
    }
}
