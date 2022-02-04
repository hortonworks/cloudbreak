package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.INTERNAL_ACTOR_CRN;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.KeyEncryptionMethod;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.YarnStackV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AwsEncryptionV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AwsInstanceTemplateV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AzureEncryptionV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AzureInstanceTemplateV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.GcpEncryptionV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.GcpInstanceTemplateV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.authentication.StackAuthenticationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.TagsV4Request;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.idbmms.GrpcIdbmmsClient;
import com.sequenceiq.cloudbreak.idbmms.exception.IdbmmsOperationException;
import com.sequenceiq.cloudbreak.idbmms.model.MappingsConfig;
import com.sequenceiq.cloudbreak.telemetry.TelemetryClusterDetails;
import com.sequenceiq.cloudbreak.telemetry.fluent.FluentConfigView;
import com.sequenceiq.cloudbreak.util.PasswordUtil;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.common.api.cloudstorage.AccountMappingBase;
import com.sequenceiq.common.api.cloudstorage.CloudStorageRequest;
import com.sequenceiq.common.api.cloudstorage.StorageIdentityBase;
import com.sequenceiq.common.api.telemetry.request.FeaturesRequest;
import com.sequenceiq.common.api.telemetry.request.LoggingRequest;
import com.sequenceiq.common.api.telemetry.request.TelemetryRequest;
import com.sequenceiq.common.api.telemetry.response.LoggingResponse;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import com.sequenceiq.common.api.type.EncryptionType;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.model.CloudIdentityType;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.events.EventSenderService;
import com.sequenceiq.datalake.service.validation.cloudstorage.CloudStorageLocationValidator;
import com.sequenceiq.datalake.service.validation.cloudstorage.CloudStorageValidator;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkYarnParams;
import com.sequenceiq.environment.api.v1.environment.model.base.IdBrokerMappingSource;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsDiskEncryptionParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureResourceEncryptionParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.gcp.GcpEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.gcp.GcpResourceEncryptionParameters;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SecurityAccessResponse;

@Service
public class StackRequestManifester {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackRequestManifester.class);

    @Inject
    private GatewayManifester gatewayManifester;

    @Inject
    private CloudStorageValidator cloudStorageValidator;

    @Inject
    private CloudStorageLocationValidator cloudStorageLocationValidator;

    @Inject
    private GrpcIdbmmsClient idbmmsClient;

    @Inject
    private SecurityAccessManifester securityAccessManifester;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private SdxNotificationService sdxNotificationService;

    @Inject
    private EventSenderService eventSenderService;

    @Inject
    private MultiAzDecorator multiAzDecorator;

    public void configureStackForSdxCluster(SdxCluster sdxCluster, DetailedEnvironmentResponse environment) {
        StackV4Request generatedStackV4Request = setupStackRequestForCloudbreak(sdxCluster, environment);
        gatewayManifester.configureGatewayForSdxCluster(generatedStackV4Request);
        addStackV4RequestAsString(sdxCluster, generatedStackV4Request);
    }

    private void addStackV4RequestAsString(SdxCluster sdxCluster, StackV4Request internalRequest) {
        try {
            LOGGER.info("Forming request from Internal Request");
            sdxCluster.setStackRequestToCloudbreak(JsonUtil.writeValueAsString(internalRequest));
        } catch (JsonProcessingException e) {
            LOGGER.error("Can not serialize stack request as JSON");
            throw new BadRequestException("Can not serialize stack request as JSON", e);
        }
    }

    private StackV4Request setupStackRequestForCloudbreak(SdxCluster sdxCluster, DetailedEnvironmentResponse environment) {
        try {
            LOGGER.info("Setting up stack request of SDX {} for cloudbreak", sdxCluster.getClusterName());
            StackV4Request stackRequest = JsonUtil.readValue(sdxCluster.getStackRequest(), StackV4Request.class);
            stackRequest.setName(sdxCluster.getClusterName());
            stackRequest.setType(StackType.DATALAKE);
            if (stackRequest.getTags() == null) {
                Map<String, String> userDefined = environment.getTags().getUserDefined();
                TagsV4Request tags = new TagsV4Request();
                try {
                    Map<String, String> sdxUserDefined = new HashMap<>();
                    // TODO currently the sdx app is putting 'null' if the user send us a null this is now fixed but we need to support old DL's
                    if (sdxCluster.getTags() != null
                            && !Strings.isNullOrEmpty(sdxCluster.getTags().getValue())
                            && !"null".equals(sdxCluster.getTags().getValue())) {
                        sdxUserDefined = sdxCluster.getTags().get(HashMap.class);
                    }
                    userDefined.putAll(sdxUserDefined);
                    tags.setUserDefined(userDefined);
                } catch (IOException e) {
                    LOGGER.error("Can not parse JSON to tags");
                    throw new BadRequestException("Can not parse JSON to tags", e);
                }
                stackRequest.setTags(tags);
            }
            stackRequest.setEnvironmentCrn(sdxCluster.getEnvCrn());

            if (CloudPlatform.YARN.name().equals(environment.getCloudPlatform())) {
                setupYarnDetails(environment, stackRequest);
            }

            setupAuthentication(environment, stackRequest);
            setupSecurityAccess(environment, stackRequest);
            setupClusterRequest(stackRequest);
            prepareTelemetryForStack(stackRequest, environment, sdxCluster);
            setupCloudStorageAccountMapping(stackRequest, environment.getCrn(), environment.getIdBrokerMappingSource(), environment.getCloudPlatform());
            validateCloudStorage(sdxCluster, environment, stackRequest);
            setupInstanceVolumeEncryption(stackRequest, environment);
            setupMultiAz(sdxCluster, environment, stackRequest);
            return stackRequest;
        } catch (IOException e) {
            LOGGER.error("Can not parse JSON to stack request");
            throw new IllegalStateException("Can not parse JSON to stack request", e);
        }
    }

    private void validateCloudStorage(SdxCluster sdxCluster, DetailedEnvironmentResponse environment, StackV4Request stackRequest) {
        validateCloudStorageLocation(sdxCluster, environment);
        validateCloudStorageAndHandleTimeout(sdxCluster, environment, stackRequest);
    }

    private void validateCloudStorageAndHandleTimeout(SdxCluster sdxCluster, DetailedEnvironmentResponse environment, StackV4Request stackRequest) {
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        try {
            cloudStorageValidator.validate(stackRequest.getCluster().getCloudStorage(), environment, validationResultBuilder);
        } catch (Exception e) {
            String message = String.format("Error occured during object storage validation, validation skipped. Error: %s", e.getMessage());
            LOGGER.warn(message);
            sdxNotificationService.send(ResourceEvent.SDX_VALIDATION_FAILED_AND_SKIPPED, Set.of(e.getMessage()), sdxCluster);
        }
        ValidationResult validationResult = validationResultBuilder.build();
        if (validationResult.hasError()) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
        if (validationResult.hasWarning()) {
            LOGGER.info(validationResult.getFormattedWarnings());
            eventSenderService.sendEventAndNotification(sdxCluster, sdxCluster.getInitiatorUserCrn(),
                    ResourceEvent.SDX_VALIDATION_FAILED_AND_SKIPPED, Set.of(validationResult.getFormattedWarnings()));
            sdxNotificationService.send(ResourceEvent.SDX_VALIDATION_FAILED_AND_SKIPPED,
                    Set.of(validationResult.getFormattedWarnings()), sdxCluster);
        }
    }

    private void validateCloudStorageLocation(SdxCluster sdxCluster, DetailedEnvironmentResponse environment) {
        if (FileSystemType.S3.equals(sdxCluster.getCloudStorageFileSystemType())
                && !Strings.isNullOrEmpty(sdxCluster.getCloudStorageBaseLocation())) {
            ValidationResultBuilder validationBuilder = new ValidationResultBuilder();
            cloudStorageLocationValidator.validate(sdxCluster.getCloudStorageBaseLocation(), FileSystemType.S3, environment,
                    validationBuilder);
            ValidationResult validationResult = validationBuilder.build();
            if (validationResult.hasError()) {
                throw new BadRequestException(validationResult.getFormattedErrors());
            }
        }
    }

    private void setupYarnDetails(DetailedEnvironmentResponse environment, StackV4Request stackRequest) {
        if (stackRequest.getYarn() == null || stackRequest.getYarn().getYarnQueue() == null) {
            if (environment.getNetwork() == null
                    || environment.getNetwork().getYarn() == null
                    || environment.getNetwork().getYarn().getQueue() == null) {
                throw new BadRequestException("There is no queue defined in your environment, please create a new yarn environment with queue");
            } else {
                EnvironmentNetworkYarnParams yarnParams = environment.getNetwork().getYarn();
                YarnStackV4Parameters yarnStackV4Parameters = new YarnStackV4Parameters();
                yarnStackV4Parameters.setYarnQueue(yarnParams.getQueue());
                yarnStackV4Parameters.setLifetime(yarnParams.getLifetime());
                stackRequest.setYarn(yarnStackV4Parameters);
            }
        }
    }

    private void setupAuthentication(DetailedEnvironmentResponse environment, StackV4Request stackRequest) {
        if (stackRequest.getAuthentication() == null) {
            StackAuthenticationV4Request stackAuthenticationV4Request = new StackAuthenticationV4Request();
            stackAuthenticationV4Request.setPublicKey(environment.getAuthentication().getPublicKey());
            stackAuthenticationV4Request.setPublicKeyId(environment.getAuthentication().getPublicKeyId());
            stackRequest.setAuthentication(stackAuthenticationV4Request);
        }
    }

    private void setupSecurityAccess(DetailedEnvironmentResponse environment, StackV4Request stackRequest) {
        List<InstanceGroupV4Request> instanceGroups = stackRequest.getInstanceGroups();
        SecurityAccessResponse securityAccess = environment.getSecurityAccess();
        if (instanceGroups != null && securityAccess != null) {
            String securityGroupIdForKnox = securityAccess.getSecurityGroupIdForKnox();
            String defaultSecurityGroupId = securityAccess.getDefaultSecurityGroupId();
            String cidrs = securityAccess.getCidr();
            securityAccessManifester.overrideSecurityAccess(InstanceGroupType.GATEWAY, instanceGroups, securityGroupIdForKnox, cidrs);
            securityAccessManifester.overrideSecurityAccess(InstanceGroupType.CORE, instanceGroups, defaultSecurityGroupId, cidrs);
        }
    }

    private void setupClusterRequest(StackV4Request stackRequest) {
        ClusterV4Request cluster = stackRequest.getCluster();
        if (cluster != null && cluster.getBlueprintName() == null) {
            throw new BadRequestException("BlueprintName not defined, should only happen on private API");
        }
        if (cluster != null && cluster.getPassword() == null) {
            // default password needed for ranger
            cluster.setPassword(PasswordUtil.generatePassword());
        }
    }

    private void prepareTelemetryForStack(StackV4Request stackV4Request,
            DetailedEnvironmentResponse environment, SdxCluster sdxCluster) {
        TelemetryResponse envTelemetry = environment.getTelemetry();
        if (envTelemetry != null && envTelemetry.getLogging() != null) {
            TelemetryRequest telemetryRequest = new TelemetryRequest();
            LoggingRequest loggingRequest = new LoggingRequest();
            LoggingResponse envLogging = envTelemetry.getLogging();
            loggingRequest.setS3(envLogging.getS3());
            loggingRequest.setAdlsGen2(envLogging.getAdlsGen2());
            loggingRequest.setGcs(envLogging.getGcs());
            loggingRequest.setCloudwatch(envLogging.getCloudwatch());
            loggingRequest.setStorageLocation(envLogging.getStorageLocation());
            telemetryRequest.setLogging(loggingRequest);
            if (envTelemetry.getFeatures() != null) {
                FeaturesRequest featuresRequest = new FeaturesRequest();
                featuresRequest.setClusterLogsCollection(envTelemetry.getFeatures().getClusterLogsCollection());
                featuresRequest.setMonitoring(envTelemetry.getFeatures().getMonitoring());
                if (envTelemetry.getFeatures().getCloudStorageLogging() != null) {
                    featuresRequest.setCloudStorageLogging(envTelemetry.getFeatures().getCloudStorageLogging());
                } else {
                    featuresRequest.addCloudStorageLogging(true);
                }
                telemetryRequest.setFeatures(featuresRequest);
            }
            if (envTelemetry.getFluentAttributes() != null) {
                Map<String, Object> fluentAttributes = envTelemetry.getFluentAttributes();
                if (!fluentAttributes.containsKey(TelemetryClusterDetails.CLUSTER_CRN_KEY)) {
                    fluentAttributes.put(TelemetryClusterDetails.CLUSTER_CRN_KEY, sdxCluster.getCrn());
                }
                addAzureIdbrokerMsiToTelemetry(fluentAttributes, stackV4Request);
                telemetryRequest.setFluentAttributes(fluentAttributes);
            }
            stackV4Request.setTelemetry(telemetryRequest);
        }
    }

    void addAzureIdbrokerMsiToTelemetry(Map<String, Object> fluentAttributes, StackV4Request stackRequest) {
        if (stackRequest.getCluster() != null && stackRequest.getCluster().getCloudStorage() != null
                && stackRequest.getCluster().getCloudStorage().getIdentities() != null) {
            List<StorageIdentityBase> identities = stackRequest.getCluster().getCloudStorage().getIdentities();
            for (StorageIdentityBase identity : identities) {
                if (CloudIdentityType.ID_BROKER.equals(identity.getType()) && identity.getAdlsGen2() != null) {
                    if (!fluentAttributes.containsKey(FluentConfigView.AZURE_IDBROKER_INSTANCE_MSI)) {
                        String idBrokerInstanceMsi = identity.getAdlsGen2().getManagedIdentity();
                        LOGGER.info("Setting idbroker instance msi for telemetry: {}", idBrokerInstanceMsi);
                        fluentAttributes.put(FluentConfigView.AZURE_IDBROKER_INSTANCE_MSI, idBrokerInstanceMsi);
                    }
                }
            }
        }
    }

    @VisibleForTesting
    void setupCloudStorageAccountMapping(StackV4Request stackRequest, String environmentCrn, IdBrokerMappingSource mappingSource, String cloudPlatform) {
        String stackName = stackRequest.getName();
        CloudStorageRequest cloudStorage = stackRequest.getCluster().getCloudStorage();
        if (cloudStorage != null && cloudStorage.getAccountMapping() == null) {
            // In case of SdxClusterRequest with cloud storage, or SdxInternalClusterRequest with cloud storage but missing "accountMapping" property,
            // getAccountMapping() == null means we need to fetch mappings from IDBMMS.
            if (mappingSource == IdBrokerMappingSource.IDBMMS) {
                LOGGER.info("Fetching account mappings from IDBMMS associated with environment {} for stack {}.", environmentCrn, stackName);
                MappingsConfig mappingsConfig;
                try {
                    // Must pass the internal actor here as this operation is internal-use only; requests with other actors will be always rejected.
                    mappingsConfig = idbmmsClient.getMappingsConfig(INTERNAL_ACTOR_CRN, environmentCrn, Optional.empty());
                    validateMappingsConfig(mappingsConfig, stackRequest);
                } catch (IdbmmsOperationException e) {
                    throw new BadRequestException(String.format("Unable to get mappings: %s", e.getMessage()), e);
                }
                AccountMappingBase accountMapping = new AccountMappingBase();
                accountMapping.setGroupMappings(mappingsConfig.getGroupMappings());
                accountMapping.setUserMappings(mappingsConfig.getActorMappings());
                cloudStorage.setAccountMapping(accountMapping);
                LOGGER.info("Initial account mappings fetched from IDBMMS: {}", JsonUtil.writeValueAsStringSilent(accountMapping));
            } else {
                LOGGER.info("IDBMMS usage is disabled for environment {}. Proceeding with {} mappings for stack {}.", environmentCrn,
                        mappingSource == IdBrokerMappingSource.MOCK
                                && (CloudPlatform.AWS.name().equals(cloudPlatform)
                                || CloudPlatform.AZURE.name().equals(cloudPlatform)
                                || CloudPlatform.GCP.name().equals(cloudPlatform)) ? "mock" : "missing", stackName);
            }
        } else {
            // getAccountMapping() != null is possible only in case of SdxInternalClusterRequest, in which case the user-given values will be honored.
            LOGGER.info("{} for stack {} in environment {}.", cloudStorage == null ? "Cloud storage is disabled" : "Applying user-provided mappings",
                    stackName, environmentCrn);
        }
    }

    void validateMappingsConfig(MappingsConfig mappingsConfig, StackV4Request stackRequest) {
        LOGGER.debug("Validating IDBMMS mapping for this cluster: {}", JsonUtil.writeValueAsStringSilent(mappingsConfig));
        // Just to make it a bit more defensive
        if (mappingsConfig == null || mappingsConfig.getActorMappings() == null || mappingsConfig.getActorMappings().isEmpty()) {
            LOGGER.error("We have not found cloud storage access mapping for this cluster! {}", JsonUtil.writeValueAsStringSilent(mappingsConfig));
            throw new BadRequestException("We have not found cloudstorage access mapping for this cluster!");
        }
        // Validate RAZ if enabled, making sure that the RAZ mapping exists when it is required.
        if (stackRequest.getCluster().isRangerRazEnabled()) {
            if (!mappingsConfig.getActorMappings().containsKey("rangerraz")) {
                LOGGER.error("Cloud storage access (IDBroker) mapping must contain the RAZ role if RAZ is to be created!");
                throw new BadRequestException("Cloud storage access (IDBroker) mapping must contain the RAZ role if RAZ is to be created!");
            }
        }
    }

    @VisibleForTesting
    void setupInstanceVolumeEncryption(StackV4Request stackRequest, DetailedEnvironmentResponse environmentResponse) {
        if (CloudPlatform.AWS.name().equals(environmentResponse.getCloudPlatform())) {
            setupInstanceVolumeEncryptionForAws(stackRequest, environmentResponse);
        } else if (CloudPlatform.AZURE.name().equals(environmentResponse.getCloudPlatform())) {
            setupInstanceVolumeEncryptionForAzure(stackRequest, environmentResponse);
        } else if (CloudPlatform.GCP.name().equals(environmentResponse.getCloudPlatform())) {
            setupInstanceVolumeEncryptionForGcp(stackRequest, environmentResponse);
        }
    }

    @VisibleForTesting
    void setupInstanceVolumeEncryptionForAws(StackV4Request stackRequest, DetailedEnvironmentResponse environmentResponse) {
        String encryptionKeyArn = Optional.of(environmentResponse)
                .map(DetailedEnvironmentResponse::getAws)
                .map(AwsEnvironmentParameters::getAwsDiskEncryptionParameters)
                .map(AwsDiskEncryptionParameters::getEncryptionKeyArn)
                .orElse(null);
        stackRequest.getInstanceGroups().forEach(ig -> {
            AwsInstanceTemplateV4Parameters aws = ig.getTemplate().createAws();
            AwsEncryptionV4Parameters encryption = aws.getEncryption();
            if (encryption == null) {
                encryption = new AwsEncryptionV4Parameters();
                aws.setEncryption(encryption);
            }
            if (encryption.getType() == null) {
                aws.getEncryption().setType(EncryptionType.DEFAULT);
            }
            if (encryptionKeyArn != null) {
                aws.getEncryption().setType(EncryptionType.CUSTOM);
                aws.getEncryption().setKey(encryptionKeyArn);
            }

        });
    }

    @VisibleForTesting
    void setupInstanceVolumeEncryptionForAzure(StackV4Request stackRequest, DetailedEnvironmentResponse environmentResponse) {
        Optional<String> encryptionKeyUrl = Optional.of(environmentResponse)
                .map(DetailedEnvironmentResponse::getAzure)
                .map(AzureEnvironmentParameters::getResourceEncryptionParameters)
                .map(AzureResourceEncryptionParameters::getEncryptionKeyUrl);
        Optional<String> diskEncryptionSetId = Optional.of(environmentResponse)
                .map(DetailedEnvironmentResponse::getAzure)
                .map(AzureEnvironmentParameters::getResourceEncryptionParameters)
                .map(AzureResourceEncryptionParameters::getDiskEncryptionSetId);
        if (encryptionKeyUrl.isPresent() && diskEncryptionSetId.isPresent()) {
            stackRequest.getInstanceGroups().forEach(ig -> {
                AzureInstanceTemplateV4Parameters azure = ig.getTemplate().createAzure();
                AzureEncryptionV4Parameters encryption = azure.getEncryption();
                if (encryption == null) {
                    encryption = new AzureEncryptionV4Parameters();
                    azure.setEncryption(encryption);
                }
                azure.getEncryption().setKey(encryptionKeyUrl.get());
                azure.getEncryption().setType(EncryptionType.CUSTOM);
                azure.getEncryption().setDiskEncryptionSetId(diskEncryptionSetId.get());
            });
        }
    }

    @VisibleForTesting
    void setupInstanceVolumeEncryptionForGcp(StackV4Request stackRequest, DetailedEnvironmentResponse environmentResponse) {
        String encryptionKey = Optional.of(environmentResponse)
                .map(DetailedEnvironmentResponse::getGcp)
                .map(GcpEnvironmentParameters::getGcpResourceEncryptionParameters)
                .map(GcpResourceEncryptionParameters::getEncryptionKey)
                .orElse(null);
        if (encryptionKey != null) {
            stackRequest.getInstanceGroups().forEach(ig -> {
                GcpInstanceTemplateV4Parameters gcp = ig.getTemplate().createGcp();
                GcpEncryptionV4Parameters encryption = gcp.getEncryption();
                if (encryption == null) {
                    encryption = new GcpEncryptionV4Parameters();
                    gcp.setEncryption(encryption);
                }
                gcp.getEncryption().setType(EncryptionType.CUSTOM);
                gcp.getEncryption().setKey(encryptionKey);
                gcp.getEncryption().setKeyEncryptionMethod(KeyEncryptionMethod.KMS);
            });
        }
    }

    private void setupMultiAz(SdxCluster sdxCluster, DetailedEnvironmentResponse environment, StackV4Request stackRequest) {
        if (entitlementService.awsNativeDataLakeEnabled(ThreadBasedUserCrnProvider.getAccountId()) && sdxCluster.isEnableMultiAz()) {
            multiAzDecorator.decorateStackRequestWithAwsNative(stackRequest, environment);
            multiAzDecorator.decorateStackRequestWithMultiAz(stackRequest, environment, sdxCluster.getClusterShape());
        }
    }
}

