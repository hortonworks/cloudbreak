package com.sequenceiq.freeipa.service.encryption;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static com.sequenceiq.cloudbreak.util.Benchmark.measure;


import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.EncryptionResources;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKey;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.encryption.EncryptionKeyCreationRequest;
import com.sequenceiq.cloudbreak.cloud.service.ResourceRetriever;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.freeipa.converter.cloud.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackEncryption;
import com.sequenceiq.freeipa.service.CredentialService;
import com.sequenceiq.freeipa.service.StackEncryptionService;
import com.sequenceiq.freeipa.service.client.CachedEnvironmentClientService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class EncryptionKeyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EncryptionKeyService.class);

    private static final String KEY_NAME_LUKS = "%s-%s-freeipa-luks";

    private static final String KEY_NAME_CLOUD_SECRET_MANAGER = "%s-%s-freeipa-cloudSecretManager";

    private static final String KEY_DESC_LUKS = "LUKS KMS Key for %s-%s";

    private static final String KEY_DESC_CLOUD_SECRET_MANAGER = "Cloud Secret Manager KMS Key for %s-%s";

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private CredentialToExtendedCloudCredentialConverter extendedCloudCredentialConverter;

    @Inject
    private CredentialService credentialService;

    @Inject
    private ResourceRetriever resourceRetriever;

    @Inject
    private StackService stackService;

    @Inject
    private StackEncryptionService stackEncryptionService;

    @Inject
    private CachedEnvironmentClientService cachedEnvironmentClientService;

    public void generateEncryptionKeys(Long stackId) {
        Stack stack = stackService.getStackById(stackId);
        DetailedEnvironmentResponse environment = measure(() -> cachedEnvironmentClientService.getByCrn(stack.getEnvironmentCrn()),
                LOGGER, "Environment properties were queried under {} ms for environment {}", stack.getEnvironmentCrn());
        if (environment.isEnableSecretEncryption()) {
            String loggerInstanceProfile = getLoggerInstanceProfile(environment, stack);
            String crossAccountRole = getCrossAccountRole(environment, stack);
            Map<String, String> tags = getTags(stack);
            List<CloudResource> cloudResources = resourceRetriever.findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.AWS_KMS_KEY, stack.getId());
            ExtendedCloudCredential extendedCloudCredential = getExtendedCloudCredential(stack);
            EncryptionKeyCreationRequest.Builder encryptionKeyBuilder = EncryptionKeyCreationRequest.builder()
                    .withCloudCredential(extendedCloudCredential)
                    .withCloudContext(getCloudContext(stack))
                    .withCloudResources(cloudResources)
                    .withTags(tags);
            EncryptionResources encryptionResources = getEncryptionResources(stack);
            String luksKmsKey = generateEncryptionKey(populateStackInformation(KEY_NAME_LUKS, stack), populateStackInformation(KEY_DESC_LUKS, stack),
                    List.of(loggerInstanceProfile), encryptionKeyBuilder, encryptionResources);
            String cloudSecretManagerKmsKey = generateEncryptionKey(populateStackInformation(KEY_NAME_CLOUD_SECRET_MANAGER, stack),
                    populateStackInformation(KEY_DESC_CLOUD_SECRET_MANAGER, stack), List.of(crossAccountRole, loggerInstanceProfile), encryptionKeyBuilder,
                    encryptionResources);
            StackEncryption stackEncryption = new StackEncryption(stack.getId());
            stackEncryption.setAccountId(stack.getAccountId());
            stackEncryption.setEncryptionKeyLuks(luksKmsKey);
            stackEncryption.setEncryptionKeyCloudSecretManager(cloudSecretManagerKmsKey);
            stackEncryptionService.save(stackEncryption);
        } else {
            LOGGER.info("Secret Encryption is not enable for Stack ID {} so encryption keys will not be generated", stackId);
        }
    }

    private String generateEncryptionKey(String keyName, String keyDescription, List<String> cryptographicPrincipals,
            EncryptionKeyCreationRequest.Builder encryptionKeyBuilder, EncryptionResources encryptionResources) {
        EncryptionKeyCreationRequest encryptionKeyCreationRequest = encryptionKeyBuilder
                .withKeyName(keyName)
                .withDescription(keyDescription)
                .withCryptographicPrincipals(cryptographicPrincipals)
                .build();
        CloudEncryptionKey cloudEncryptionKey = encryptionResources.createEncryptionKey(encryptionKeyCreationRequest);
        return cloudEncryptionKey.getName();
    }

    public EncryptionResources getEncryptionResources(Stack stack) {
        CloudPlatformVariant cloudPlatformVariant = new CloudPlatformVariant(
                Platform.platform(stack.getCloudPlatform()),
                Variant.variant(stack.getPlatformvariant()));
        EncryptionResources encryptionResources = cloudPlatformConnectors.get(cloudPlatformVariant).encryptionResources();
        if (encryptionResources == null) {
            throw getCloudBreakServiceException(stack.getName(), String.format("Unsupported cloud platform: %s", stack.getCloudPlatform()));
        }
        return encryptionResources;
    }

    private ExtendedCloudCredential getExtendedCloudCredential(Stack stack) {
        return extendedCloudCredentialConverter
                .convert(credentialService.getCredentialByEnvCrn(stack.getEnvironmentCrn()));
    }

    private CloudContext getCloudContext(Stack stack) {
        Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withLocation(location)
                .withId(stack.getId())
                .build();
        return cloudContext;
    }

    private CloudbreakServiceException getCloudBreakServiceException(String stackName, String additionalInfo) {
        String errorMessage = String.format("Unable to generate encryption keys for %s.%s", stackName, additionalInfo);
        LOGGER.error(errorMessage);
        return new CloudbreakServiceException(errorMessage);
    }

    private String getLoggerInstanceProfile(DetailedEnvironmentResponse environment, Stack stack) {
        if (environment.getTelemetry() != null && environment.getTelemetry().getLogging() != null
                && environment.getTelemetry().getLogging().getS3() != null
                && StringUtils.isNotBlank(environment.getTelemetry().getLogging().getS3().getInstanceProfile())) {
            return environment.getTelemetry().getLogging().getS3().getInstanceProfile();
        } else {
            throw getCloudBreakServiceException(stack.getName(), "Logger instance profile not found");
        }
    }

    private String getCrossAccountRole(DetailedEnvironmentResponse environment, Stack stack) {
        if (environment.getCredential() != null && environment.getCredential().getAws() != null
                && environment.getCredential().getAws().getRoleBased() != null
                && StringUtils.isNotBlank(environment.getCredential().getAws().getRoleBased().getRoleArn())) {
            return environment.getCredential().getAws().getRoleBased().getRoleArn();
        } else {
            throw getCloudBreakServiceException(stack.getName(), "Cross Account role not found");
        }
    }

    private Map<String, String> getTags(Stack stack) {
        if (stack.getTags() != null) {
            try {
                StackTags stackTags = stack.getTags().get(StackTags.class);
                Map<String, String> tags = new HashMap<>(stackTags.getDefaultTags());
                Map<String, String> applicationTags = new HashMap<>(stackTags.getApplicationTags());
                tags.putAll(applicationTags);
                return tags;
            } catch (IOException e) {
                throw getCloudBreakServiceException(stack.getName(), "Unable for fetch tags");
            }
        }
        return Map.of();
    }

    private String populateStackInformation(String token, Stack stack) {
        return String.format(token, stack.getName(), stack.getResourceCrn());
    }
}
