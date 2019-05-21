package com.sequenceiq.environment.environment.validation.network;

import static com.sequenceiq.environment.CloudPlatform.AZURE;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.util.ValidationResult;
import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.network.dto.AzureParams;
import com.sequenceiq.environment.network.dto.NetworkDto;

@Component
public class AzureEnvironmentNetworkValidator implements EnvironmentNetworkValidator {

    @Override
    public void validate(NetworkDto networkDto, ValidationResult.ValidationResultBuilder resultBuilder) {
        AzureParams azureParams = networkDto.getAzure();
        if (azureParams != null) {
            if (StringUtils.isEmpty(azureParams.getNetworkId())) {
                resultBuilder.error(missingParamErrorMessage("network identifier(networkId)", getCloudPlatform().name()));
            }
            if (StringUtils.isEmpty(azureParams.getResourceGroupName())) {
                resultBuilder.error(missingParamErrorMessage("resource group's name(resourceGroupName)", getCloudPlatform().name()));
            }
        } else {
            resultBuilder.error(missingParamsErrorMsg(AZURE));
        }
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return AZURE;
    }
}
