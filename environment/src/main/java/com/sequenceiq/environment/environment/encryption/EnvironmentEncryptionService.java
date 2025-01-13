package com.sequenceiq.environment.environment.encryption;

import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.EncryptionResources;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.encryption.CreatedDiskEncryptionSet;
import com.sequenceiq.cloudbreak.cloud.model.encryption.DiskEncryptionSetCreationRequest;
import com.sequenceiq.cloudbreak.cloud.model.encryption.DiskEncryptionSetDeletionRequest;
import com.sequenceiq.cloudbreak.cloud.model.encryption.EncryptionParametersValidationRequest;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.service.EnvironmentTagProvider;
import com.sequenceiq.environment.parameter.dto.AzureParametersDto;
import com.sequenceiq.environment.parameter.dto.AzureResourceEncryptionParametersDto;
import com.sequenceiq.environment.parameter.dto.AzureResourceGroupDto;
import com.sequenceiq.environment.parameter.dto.ParametersDto;
import com.sequenceiq.environment.resourcepersister.CloudResourceRetrieverService;

@Component
public class EnvironmentEncryptionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentEncryptionService.class);

    private final CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    private final CloudPlatformConnectors cloudPlatformConnectors;

    private final EnvironmentTagProvider environmentTagProvider;

    private final CloudResourceRetrieverService resourceRetriever;

    public EnvironmentEncryptionService(CredentialToCloudCredentialConverter credentialToCloudCredentialConverter,
            CloudPlatformConnectors cloudPlatformConnectors, EnvironmentTagProvider environmentTagProvider,
            CloudResourceRetrieverService resourceRetriever) {
        this.credentialToCloudCredentialConverter = credentialToCloudCredentialConverter;
        this.cloudPlatformConnectors = cloudPlatformConnectors;
        this.environmentTagProvider = environmentTagProvider;
        this.resourceRetriever = resourceRetriever;
    }

    public void validateEncryptionParameters(EnvironmentDto environmentDto) {
        EncryptionResources encryptionResources = getEncryptionResources(environmentDto.getCloudPlatform());
        EncryptionParametersValidationRequest validationRequest = createEncryptionParametersValidationRequest(environmentDto);
        encryptionResources.validateEncryptionParameters(validationRequest);
    }

    public CreatedDiskEncryptionSet createEncryptionResources(EnvironmentDto environmentDto) {
        EncryptionResources encryptionResources = getEncryptionResources(environmentDto.getCloudPlatform());
        return encryptionResources.createDiskEncryptionSet(createEncryptionResourcesCreationRequest(environmentDto));
    }

    public void deleteEncryptionResources(EnvironmentDto environmentDto) {
        EncryptionResources encryptionResources = getEncryptionResources(environmentDto.getCloudPlatform());
        encryptionResources.deleteDiskEncryptionSet(createEncryptionResourcesDeletionRequest(environmentDto));
    }

    @VisibleForTesting
    EncryptionResources getEncryptionResources(String cloudPlatform) {
        CloudPlatformVariant cloudPlatformVariant = new CloudPlatformVariant(Platform.platform(cloudPlatform), Variant.variant(cloudPlatform));
        return Optional.ofNullable(cloudPlatformConnectors.get(cloudPlatformVariant))
                .map(CloudConnector::encryptionResources)
                .orElseThrow(() -> new EncryptionResourcesNotFoundException("No Encryption resources component found for cloud platform: " + cloudPlatform));
    }

    @VisibleForTesting
    DiskEncryptionSetCreationRequest createEncryptionResourcesCreationRequest(EnvironmentDto environment) {
        String encryptionKeyResourceGroupName = getVaultResourceGroup(environment);
        String diskEncryptionSetResourceGroupName = getEnvironmentResourceGroupName(environment);
        DiskEncryptionSetCreationRequest.Builder builder = new DiskEncryptionSetCreationRequest.Builder()
                .withId(Crn.safeFromString(environment.getResourceCrn()).getResource())
                .withCloudCredential(credentialToCloudCredentialConverter.convert(environment.getCredential()))
                .withTags(environmentTagProvider.getTags(environment, environment.getResourceCrn()))
                .withCloudContext(getCloudContext(environment))
                .withEncryptionKeyUrl(environment.getParameters().getAzureParametersDto().getAzureResourceEncryptionParametersDto().getEncryptionKeyUrl())
                .withUserManagedIdentity(environment.getParameters().getAzureParametersDto().getAzureResourceEncryptionParametersDto().getUserManagedIdentity())
                .withDiskEncryptionSetResourceGroupName(diskEncryptionSetResourceGroupName);
        if (StringUtils.isNotEmpty(encryptionKeyResourceGroupName)) {
            builder.withEncryptionKeyResourceGroupName(encryptionKeyResourceGroupName);
        } else {
            builder.withEncryptionKeyResourceGroupName(diskEncryptionSetResourceGroupName);
        }
        return builder.build();
    }

    @VisibleForTesting
    DiskEncryptionSetDeletionRequest createEncryptionResourcesDeletionRequest(EnvironmentDto environment) {
        return new DiskEncryptionSetDeletionRequest.Builder()
                .withCloudCredential(credentialToCloudCredentialConverter.convert(environment.getCredential()))
                .withCloudContext(getCloudContext(environment))
                .withCloudResources(getResourcesForDeletion(environment))
                .build();
    }

    private EncryptionParametersValidationRequest createEncryptionParametersValidationRequest(EnvironmentDto environmentDto) {
        String resourceGroupName = getEnvironmentResourceGroupName(environmentDto);
        Map<ResourceType, CloudResource> cloudResourceMap = Optional.ofNullable(environmentDto.getParameters())
                .map(ParametersDto::getAzureParametersDto)
                .map(AzureParametersDto::getAzureResourceEncryptionParametersDto)
                .map(encryptionParameters -> createAzureEncryptionCloudResources(encryptionParameters, resourceGroupName))
                .orElse(Map.of());
        return new EncryptionParametersValidationRequest(getCloudContext(environmentDto),
                credentialToCloudCredentialConverter.convert(environmentDto.getCredential()), cloudResourceMap);
    }

    private Map<ResourceType, CloudResource> createAzureEncryptionCloudResources(AzureResourceEncryptionParametersDto encryptionParameters,
            String resourceGroupName) {
        Map<ResourceType, CloudResource> cloudResourceMap = new EnumMap<>(ResourceType.class);
        cloudResourceMap.put(ResourceType.AZURE_RESOURCE_GROUP, CloudResource.builder()
                .withType(ResourceType.AZURE_RESOURCE_GROUP)
                .withName(StringUtils.isNotEmpty(encryptionParameters.getEncryptionKeyResourceGroupName()) ?
                        encryptionParameters.getEncryptionKeyResourceGroupName() : resourceGroupName)
                .build());
        cloudResourceMap.put(ResourceType.AZURE_KEYVAULT_KEY, CloudResource.builder()
                .withType(ResourceType.AZURE_KEYVAULT_KEY)
                .withName(encryptionParameters.getEncryptionKeyUrl())
                .withReference(encryptionParameters.getEncryptionKeyUrl())
                .build());
        if (StringUtils.isNotEmpty(encryptionParameters.getUserManagedIdentity())) {
            cloudResourceMap.put(ResourceType.AZURE_MANAGED_IDENTITY, CloudResource.builder()
                    .withType(ResourceType.AZURE_MANAGED_IDENTITY)
                    .withName(encryptionParameters.getUserManagedIdentity())
                    .withReference(encryptionParameters.getUserManagedIdentity())
                    .build());
        }
        LOGGER.debug("Cloud resources for Azure encryption: {}", cloudResourceMap);
        return cloudResourceMap;
    }

    private CloudContext getCloudContext(EnvironmentDto environment) {
        return CloudContext.Builder.builder()
                .withId(environment.getId())
                .withName(environment.getName())
                .withCrn(environment.getResourceCrn())
                .withPlatform(environment.getCloudPlatform())
                .withVariant(environment.getCloudPlatform())
                .withLocation(location(region(environment.getLocation().getName())))
                .withUserName(environment.getCreator())
                .withAccountId(environment.getAccountId())
                .build();
    }

    private List<CloudResource> getResourcesForDeletion(EnvironmentDto environment) {
        List<CloudResource> resources = new ArrayList<>();
        Optional<CloudResource> desCloudResourceOptional = resourceRetriever.findByEnvironmentIdAndType(environment.getId(),
                ResourceType.AZURE_DISK_ENCRYPTION_SET);
        desCloudResourceOptional.ifPresent(resources::add);

        // Resource group is persisted in cloudResource only when it is created by CDP, as part of disk encryption set creation in case of
        // multi-resource group.
        Optional<CloudResource> rgCloudResourceOptional = resourceRetriever.findByEnvironmentIdAndType(environment.getId(),
                ResourceType.AZURE_RESOURCE_GROUP);
        rgCloudResourceOptional.ifPresent(resources::add);
        return resources;
    }

    private String getEnvironmentResourceGroupName(EnvironmentDto environmentDto) {
        return Optional.ofNullable(environmentDto.getParameters())
                .map(ParametersDto::getAzureParametersDto)
                .map(AzureParametersDto::getAzureResourceGroupDto)
                .map(AzureResourceGroupDto::getName).orElse(null);
    }

    private String getVaultResourceGroup(EnvironmentDto environmentDto) {
        return Optional.ofNullable(environmentDto.getParameters())
                .map(ParametersDto::getAzureParametersDto)
                .map(AzureParametersDto::getAzureResourceEncryptionParametersDto)
                .map(AzureResourceEncryptionParametersDto::getEncryptionKeyResourceGroupName).orElse(null);
    }
}
