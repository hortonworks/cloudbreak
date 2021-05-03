package com.sequenceiq.environment.environment.encryption;

import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static com.sequenceiq.environment.parameter.dto.ResourceGroupUsagePattern.USE_MULTIPLE;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.cloudera.cdp.shaded.com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
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
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.service.EnvironmentTagProvider;
import com.sequenceiq.environment.parameter.dto.AzureParametersDto;
import com.sequenceiq.environment.parameter.dto.AzureResourceGroupDto;
import com.sequenceiq.environment.parameter.dto.ParametersDto;
import com.sequenceiq.environment.parameter.dto.ResourceGroupUsagePattern;
import com.sequenceiq.environment.resourcepersister.CloudResourceRetrieverService;

@Component
public class EnvironmentEncryptionService {

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
        DiskEncryptionSetCreationRequest.Builder builder = new DiskEncryptionSetCreationRequest.Builder()
                .withId(Crn.safeFromString(environment.getResourceCrn()).getResource())
                .withCloudCredential(credentialToCloudCredentialConverter.convert(environment.getCredential()))
                .withTags(environmentTagProvider.getTags(environment, environment.getResourceCrn()))
                .withCloudContext(getCloudContext(environment))
                .withEncryptionKeyUrl(environment.getParameters().getAzureParametersDto().getAzureResourceEncryptionParametersDto().getEncryptionKeyUrl());
        if (isSingleResourceGroup(environment)) {
            builder.withSingleResourceGroup(true);
            builder.withResourceGroupName(environment.getParameters().getAzureParametersDto().getAzureResourceGroupDto().getName());
        } else {
            builder.withSingleResourceGroup(false);
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

    private boolean isSingleResourceGroup(EnvironmentDto environmentDto) {
        ResourceGroupUsagePattern resourceGroupUsagePattern = Optional.ofNullable(environmentDto.getParameters())
                .map(ParametersDto::azureParametersDto)
                .map(AzureParametersDto::getAzureResourceGroupDto)
                .map(AzureResourceGroupDto::getResourceGroupUsagePattern)
                .orElse(USE_MULTIPLE);
        return resourceGroupUsagePattern.isSingleResourceGroup();
    }

    private CloudContext getCloudContext(EnvironmentDto environment) {
        return CloudContext.Builder.builder()
                .withId(environment.getId())
                .withName(environment.getName())
                .withCrn(environment.getResourceCrn())
                .withPlatform(environment.getCloudPlatform())
                .withVariant(environment.getCloudPlatform())
                .withLocation(location(region(environment.getLocation().getName())))
                .withUserId(environment.getCreator())
                .withUserName(environment.getCreator())
                .withAccountId(environment.getAccountId())
                .build();
    }

    private List<CloudResource> getResourcesForDeletion(EnvironmentDto environment) {
        Optional<CloudResource> desCloudResourceOptional = resourceRetriever.findByResourceReferenceAndStatusAndType(
                environment.getParameters().getAzureParametersDto().getAzureResourceEncryptionParametersDto().getDiskEncryptionSetId(),
                CommonStatus.CREATED, ResourceType.AZURE_DISK_ENCRYPTION_SET);
        return desCloudResourceOptional.map(List::of).orElse(List.of());
    }

}