package com.sequenceiq.cloudbreak.controller.validation.environment.network;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform.AZURE;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.base.EnvironmentNetworkAzureV4Params;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.EnvironmentNetworkV4Request;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;

@Component
public class AzureEnvironmentNetworkValidator implements EnvironmentNetworkValidator {

    @Override
    public void validate(EnvironmentNetworkV4Request networkV4Request, ValidationResult.ValidationResultBuilder resultBuilder) {
        EnvironmentNetworkAzureV4Params azureV4Params = networkV4Request.getAzure();
        if (azureV4Params != null) {
            if (StringUtils.isEmpty(azureV4Params.getNetworkId())) {
                resultBuilder.error(missingParamErrorMessage("network identifier(networkId)", getCloudPlatform().name()));
            }
            if (StringUtils.isEmpty(azureV4Params.getResourceGroupName())) {
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
