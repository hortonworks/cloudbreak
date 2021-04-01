package com.sequenceiq.environment.environment.encryption;

import static com.sequenceiq.environment.parameter.dto.ResourceGroupUsagePattern.USE_MULTIPLE;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.EncryptionResources;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.encryption.CreatedEncryptionResources;
import com.sequenceiq.cloudbreak.cloud.model.encryption.EncryptionResourcesCreationRequest;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.parameter.dto.AzureParametersDto;
import com.sequenceiq.environment.parameter.dto.AzureResourceGroupDto;
import com.sequenceiq.environment.parameter.dto.ParametersDto;
import com.sequenceiq.environment.parameter.dto.ResourceGroupUsagePattern;

@Component
public class EnvironmentEncryptionService {

    private final CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    private final CloudPlatformConnectors cloudPlatformConnectors;

    public EnvironmentEncryptionService(CredentialToCloudCredentialConverter credentialToCloudCredentialConverter,
            CloudPlatformConnectors cloudPlatformConnectors) {
        this.credentialToCloudCredentialConverter = credentialToCloudCredentialConverter;
        this.cloudPlatformConnectors = cloudPlatformConnectors;
    }

    public CreatedEncryptionResources createEncryptionResources(EnvironmentDto environmentDto, Environment environment) {
        EncryptionResources encryptionResources = getCloudConnector(environment.getCloudPlatform());
        return encryptionResources.createDiskEncryptionSet(createEncryptionResourcesCreationRequest(environmentDto));
    }

    private EncryptionResources getCloudConnector(String cloudPlatform) {
        CloudPlatformVariant cloudPlatformVariant = new CloudPlatformVariant(Platform.platform(cloudPlatform), Variant.variant(cloudPlatform));
        return Optional.ofNullable(cloudPlatformConnectors.get(cloudPlatformVariant).encryptionResources())
                .orElseThrow(() -> new NotFoundException("Encryption resources component not found!"));
    }

    private EncryptionResourcesCreationRequest createEncryptionResourcesCreationRequest(EnvironmentDto environment) {
        CloudCredential cloudCredential = credentialToCloudCredentialConverter.convert(environment.getCredential());
        EncryptionResourcesCreationRequest.Builder builder = new EncryptionResourcesCreationRequest.Builder()
                .withCloudCredential(cloudCredential)
                .withRegion(environment.getLocation().getName())
                .withEnvName(environment.getName())
                .withEnvId(environment.getId())
                .withTags(environment.getTags().getUserDefinedTags())
                .withEncryptionKeyUrl(environment.getParameters().getAzureParametersDto()
                        .getAzureResourceEncryptionParametersDto().getEncryptionKeyUrl());
        if (isSingleResourceGroup(environment)) {
            builder.withSingleResourceGroup(true);
            builder.withResourceGroupName(environment.getParameters().getAzureParametersDto().getAzureResourceGroupDto().getName());
        } else {
            builder.withSingleResourceGroup(false);
        }
        return builder.build();
    }

    private boolean isSingleResourceGroup(EnvironmentDto environmentDto) {
        ResourceGroupUsagePattern resourceGroupUsagePattern = Optional.ofNullable(environmentDto.getParameters())
                .map(ParametersDto::azureParametersDto)
                .map(AzureParametersDto::getAzureResourceGroupDto)
                .map(AzureResourceGroupDto::getResourceGroupUsagePattern)
                .orElse(USE_MULTIPLE);
        return resourceGroupUsagePattern.isSingleResourceGroup();
    }

}