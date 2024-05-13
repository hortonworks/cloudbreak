package com.sequenceiq.cloudbreak.service.encryption;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static com.sequenceiq.cloudbreak.util.Benchmark.measure;


import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
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
import com.sequenceiq.cloudbreak.converter.spi.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackEncryption;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialConverter;
import com.sequenceiq.cloudbreak.service.stack.StackEncryptionService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Service
public class EncryptionKeyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EncryptionKeyService.class);

    private static final String KEY_NAME_LUKS = "%s-%s-%s-luks";

    private static final String KEY_NAME_CLOUD_SECRET_MANAGER = "%s-%s-%s-cloudSecretManager";

    private static final String KEY_DESC_LUKS = "LUKS KMS Key for %s-%s-%s";

    private static final String KEY_DESC_CLOUD_SECRET_MANAGER = "Cloud Secret Manager KMS Key for %s-%s-%s";

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private CredentialToExtendedCloudCredentialConverter extendedCloudCredentialConverter;

    @Inject
    private CredentialConverter credentialConverter;

    @Inject
    private ResourceRetriever resourceRetriever;

    @Inject
    private StackService stackService;

    @Inject
    private StackEncryptionService stackEncryptionService;

    @Inject
    private EnvironmentClientService environmentClientService;

    @Inject
    private List<CloudInformationDecorator> cloudInformationDecorators;

    private Map<CloudPlatformVariant, CloudInformationDecorator> cloudInformationDecoratorMap = new HashMap<>();

    @PostConstruct
    public void init() {
        for (CloudInformationDecorator cloudInformationDecorator : cloudInformationDecorators) {
            CloudPlatformVariant cloudPlatformVariant = new CloudPlatformVariant(cloudInformationDecorator.platform(), cloudInformationDecorator.variant());
            cloudInformationDecoratorMap.put(cloudPlatformVariant, cloudInformationDecorator);
        }
    }

    public void generateEncryptionKeys(Long stackId) {
        Stack stack = stackService.get(stackId);
        DetailedEnvironmentResponse environment = measure(() -> environmentClientService.getByCrn(stack.getEnvironmentCrn()),
                LOGGER, "Environment properties were queried under {} ms for environment {}", stack.getEnvironmentCrn());
        if (environment.isEnableSecretEncryption()) {
            CloudPlatformVariant cloudPlatformVariant = getCloudPlatformVariant(stack);
            CloudInformationDecorator cloudInformationDecorator = cloudInformationDecoratorMap.get(cloudPlatformVariant);
            if (cloudInformationDecorator == null) {
                throw getCloudBreakServiceException(stack.getName(), String.format("Unsupported cloud platform and variant: %s", cloudPlatformVariant));
            }
            String credentialPrincipal = cloudInformationDecorator.getCredentialPrincipal(environment, stack).orElseThrow(() ->
                    getCloudBreakServiceException(stack.getName(), "Credential principal not found"));
            List<String> principalIds = principalsForLuksKey(stack, environment, cloudInformationDecorator);
            Map<String, String> tags = getTags(stack);
            List<CloudResource> cloudResources = resourceRetriever.findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.AWS_KMS_KEY, stack.getId());
            ExtendedCloudCredential extendedCloudCredential = getExtendedCloudCredential(environment);
            EncryptionKeyCreationRequest.Builder encryptionKeyBuilder = EncryptionKeyCreationRequest.builder()
                    .withCloudCredential(extendedCloudCredential)
                    .withCloudContext(getCloudContext(stack))
                    .withCloudResources(cloudResources)
                    .withTags(tags);
            EncryptionResources encryptionResources = getEncryptionResources(stack);
            String luksKmsKey = generateEncryptionKey(populateStackInformation(KEY_NAME_LUKS, stack), populateStackInformation(KEY_DESC_LUKS, stack),
                    principalIds, encryptionKeyBuilder, encryptionResources);
            String cloudSecretManagerKmsKey = generateEncryptionKey(populateStackInformation(KEY_NAME_CLOUD_SECRET_MANAGER, stack),
                    populateStackInformation(KEY_DESC_CLOUD_SECRET_MANAGER, stack), List.of(credentialPrincipal), encryptionKeyBuilder, encryptionResources);
            StackEncryption stackEncryption = new StackEncryption(stack.getId());
            stackEncryption.setAccountId(stack.getTenantName());
            stackEncryption.setEncryptionKeyLuks(luksKmsKey);
            stackEncryption.setEncryptionKeyCloudSecretManager(cloudSecretManagerKmsKey);
            stackEncryptionService.save(stackEncryption);
        } else {
            LOGGER.info("Secret Encryption is not enabled for Stack Id {} so encryption keys will not be generated", stackId);
        }
    }

    private String generateEncryptionKey(String keyName, String keyDescription, List<String> principalIds,
            EncryptionKeyCreationRequest.Builder encryptionKeyBuilder,
            EncryptionResources encryptionResources) {
        EncryptionKeyCreationRequest encryptionKeyCreationRequest = encryptionKeyBuilder
                .withKeyName(keyName)
                .withDescription(keyDescription)
                .withCryptographicPrincipals(principalIds)
                .build();
        CloudEncryptionKey cloudEncryptionKey = encryptionResources.createEncryptionKey(encryptionKeyCreationRequest);
        return cloudEncryptionKey.getName();
    }

    public EncryptionResources getEncryptionResources(Stack stack) {
        CloudPlatformVariant cloudPlatformVariant = getCloudPlatformVariant(stack);
        EncryptionResources encryptionResources = cloudPlatformConnectors.get(cloudPlatformVariant).encryptionResources();
        if (encryptionResources == null) {
            throw getCloudBreakServiceException(stack.getName(), String.format("Unsupported cloud platform: %s and variant: %s",
                    stack.getCloudPlatform(), stack.getPlatformVariant()));
        }
        return encryptionResources;
    }

    private ExtendedCloudCredential getExtendedCloudCredential(DetailedEnvironmentResponse detailedEnvironmentResponse) {
        return extendedCloudCredentialConverter
                .convert(credentialConverter.convert(detailedEnvironmentResponse.getCredential()));
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
        return String.format(token, stack.getName(), stack.getResourceCrn(), stack.getType().getResourceType());
    }

    private CloudPlatformVariant getCloudPlatformVariant(Stack stack) {
        return new CloudPlatformVariant(
                Platform.platform(stack.getCloudPlatform()),
                Variant.variant(stack.getPlatformVariant()));
    }

    private List<String> principalsForLuksKey(Stack stack, DetailedEnvironmentResponse environment, CloudInformationDecorator cloudInformationDecorator) {
        List<String> principalIds;
        if (stack.getType() == StackType.DATALAKE) {
            principalIds = cloudInformationDecorator.getCloudIdentities(environment, stack);
        } else if (stack.getType() == StackType.WORKLOAD) {
            principalIds = cloudInformationDecorator.getLoggerInstances(environment, stack);
        } else {
            throw getCloudBreakServiceException(stack.getName(), String.format("Unsupported cluster type: %s", stack.getType()));
        }
        if (CollectionUtils.isEmpty(principalIds)) {
            throw getCloudBreakServiceException(stack.getName(), "Unable to determine principal Ids");
        }
        return principalIds;
    }
}
