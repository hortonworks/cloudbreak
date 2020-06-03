package com.sequenceiq.environment.environment.flow.deletion.handler.converter;

import static com.sequenceiq.environment.parameters.dao.domain.ResourceGroupCreation.CREATE_NEW;
import static com.sequenceiq.environment.parameters.dao.domain.ResourceGroupUsagePattern.USE_MULTIPLE;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.prerequisite.AzurePrerequisiteDeleteRequest;
import com.sequenceiq.cloudbreak.cloud.model.prerequisite.EnvironmentPrerequisiteDeleteRequest;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.parameters.dto.AzureParametersDto;
import com.sequenceiq.environment.parameters.dto.AzureResourceGroupDto;
import com.sequenceiq.environment.parameters.dto.ParametersDto;

@Component
public class AzureEnvironmentPrerequisiteDeleteRequestParameterSetter implements EnvironmentPrerequisiteDeleteRequestParameterSetter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureEnvironmentPrerequisiteDeleteRequestParameterSetter.class);

    @Override
    public EnvironmentPrerequisiteDeleteRequest setParameters(EnvironmentPrerequisiteDeleteRequest environmentPrerequisiteDeleteRequest,
            EnvironmentDto environmentDto) {
        Optional<AzureResourceGroupDto> azureResourceGroupDtoOptional = getAzureResourceGroupDto(environmentDto);
        if (azureResourceGroupDtoOptional.isEmpty()) {
            LOGGER.debug("No azure resource group dto defined, not deleting resource group.");
            return environmentPrerequisiteDeleteRequest;
        }

        AzureResourceGroupDto azureResourceGroupDto = azureResourceGroupDtoOptional.get();
        LOGGER.debug("Azure resource group dto: {}", azureResourceGroupDto);
        if (USE_MULTIPLE.equals(azureResourceGroupDto.getResourceGroupUsagePattern()) || !CREATE_NEW.equals(azureResourceGroupDto.getResourceGroupCreation())) {
            LOGGER.debug("Not deleting resource group.");
            return environmentPrerequisiteDeleteRequest;
        }

        String resourceGroupName = azureResourceGroupDto.getName();
        return environmentPrerequisiteDeleteRequest
                .withAzurePrerequisiteDeleteRequest(AzurePrerequisiteDeleteRequest.builder().withResourceGroupName(resourceGroupName).build());
    }

    @Override
    public String getCloudPlatform() {
        return CloudPlatform.AZURE.name();
    }

    private Optional<AzureResourceGroupDto> getAzureResourceGroupDto(EnvironmentDto environmentDto) {
        return Optional.ofNullable(environmentDto.getParameters())
                .map(ParametersDto::getAzureParametersDto)
                .map(AzureParametersDto::getAzureResourceGroupDto);
    }
}
