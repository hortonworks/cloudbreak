package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.cloudbreak.service.identitymapping.AccountMappingSubject.DATA_ACCESS_USERS;
import static com.sequenceiq.cloudbreak.service.identitymapping.AccountMappingSubject.RANGER_AUDIT_USERS;
import static com.sequenceiq.datalake.service.sdx.SdxService.WORKSPACE_ID_DEFAULT;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.providerservices.CloudProviderServicesV4Endopint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.model.BackupOperationType;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageValidateRequest;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageValidateResponse;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.service.identitymapping.AccountMappingSubject;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.common.api.cloudstorage.AccountMappingBase;
import com.sequenceiq.common.api.cloudstorage.CloudStorageRequest;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.events.EventSenderService;
import com.sequenceiq.datalake.service.validation.cloudstorage.CloudStorageValidator;
import com.sequenceiq.datalake.service.validation.converter.CredentialResponseToCloudCredentialConverter;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.sdx.api.model.SdxCloudStorageRequest;

@Service
public class StorageValidationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageValidationService.class);

    @Inject
    private CloudStorageManifester cloudStorageManifester;

    @Inject
    private CloudProviderServicesV4Endopint cloudProviderServicesV4Endpoint;

    @Inject
    private EnvironmentService environmentClientService;

    @Inject
    private CloudStorageValidator cloudStorageValidator;

    @Inject
    private SdxNotificationService sdxNotificationService;

    @Inject
    private EventSenderService eventSenderService;

    @Inject
    private EnvironmentService environmentService;

    @Inject
    private CredentialResponseToCloudCredentialConverter credentialResponseToCloudCredentialConverter;

    @Inject
    private StackV4Endpoint stackV4Endpoint;

    public ObjectStorageValidateResponse validateObjectStorage(String credentialCrn, SdxCloudStorageRequest sdxCloudStorageRequest, String blueprintName,
            String clusterName, String dataAccessRole, String rangerAuditRole, String rangerCloudAccessAuthorizerRole) {
        CredentialResponse credentialResponse = environmentClientService.getCredentialByCrn(credentialCrn);
        CloudCredential cloudCredential = credentialResponseToCloudCredentialConverter.convert(credentialResponse);
        CloudStorageRequest cloudStorageRequest = cloudStorageManifester.initSdxCloudStorageRequest(credentialResponse.getCloudPlatform(),
                blueprintName, clusterName, sdxCloudStorageRequest);
        AccountMappingBase accountMapping = new AccountMappingBase();
        Map<String, String> userMapping = getUserMapping(dataAccessRole, rangerAuditRole, rangerCloudAccessAuthorizerRole);
        accountMapping.setUserMappings(userMapping);
        cloudStorageRequest.setAccountMapping(accountMapping);
        ObjectStorageValidateRequest objectStorageValidateRequest = ObjectStorageValidateRequest.builder()
                .withCloudPlatform(credentialResponse.getCloudPlatform())
                .withCredential(cloudCredential)
                .withCloudStorageRequest(cloudStorageRequest)
                .withBackupOperationType(BackupOperationType.NONE)
                .build();
        return ThreadBasedUserCrnProvider.doAsInternalActor(() -> cloudProviderServicesV4Endpoint.validateObjectStorage(objectStorageValidateRequest));
    }

    public ValidationResult validateBackupStorage(SdxCluster sdxCluster, BackupOperationType backupOperationType, String backupLocation) {
        DetailedEnvironmentResponse environmentResponse = environmentService.getDetailedEnvironmentResponseByName(sdxCluster.getEnvName());

        CloudStorageRequest cloudStorageRequest = null;
        try {
            StackV4Response stack = getStack(sdxCluster);
            if (stack != null) {
                cloudStorageRequest = cloudStorageManifester.initCloudStorageRequestFromExistingSdxCluster(stack.getCluster(), sdxCluster);
            }
        } catch (RuntimeException e) {
            throw new BadRequestException("Failed to validate backup storage", e);
        }

        if (environmentResponse == null || cloudStorageRequest == null) {
            throw new BadRequestException("Failed to validate backup storage");
        }
        ValidationResult.ValidationResultBuilder validationResultBuilder = new ValidationResult.ValidationResultBuilder();
        try {
            cloudStorageValidator.validateBackupLocation(cloudStorageRequest, backupOperationType, environmentResponse, backupLocation, sdxCluster.getRuntime(),
                    validationResultBuilder);
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

    private StackV4Response getStack(SdxCluster cluster) {
        try {
            LOGGER.info("Calling cloudbreak for SDX cluster details by name {}", cluster.getName());
            return ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> stackV4Endpoint.get(WORKSPACE_ID_DEFAULT, cluster.getName(), Collections.emptySet(), cluster.getAccountId()));
        } catch (jakarta.ws.rs.NotFoundException e) {
            LOGGER.info("Sdx cluster not found on CB side", e);
            return null;
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
                || !StringUtils.isEmpty(cloudStorage.getAdlsGen2().getAccountKey()) && !StringUtils.isEmpty(cloudStorage.getAdlsGen2().getAccountName()));
    }

    private Map<String, String> getUserMapping(String dataAccessRole, String rangerAuditRole, String rangerCloudAccessAuthorizerRole) {
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
        if (rangerCloudAccessAuthorizerRole != null) {
            userMapping.put(AccountMappingSubject.RANGER_RAZ_USER, rangerCloudAccessAuthorizerRole);
        }
        return userMapping;
    }
}
