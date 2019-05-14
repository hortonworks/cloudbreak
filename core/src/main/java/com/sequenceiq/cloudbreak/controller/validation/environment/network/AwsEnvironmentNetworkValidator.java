package com.sequenceiq.cloudbreak.controller.validation.environment.network;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.EnvironmentNetworkV4Request;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;

@Component
public class AwsEnvironmentNetworkValidator implements EnvironmentNetworkValidator {

    @Override
    public void validate(EnvironmentNetworkV4Request networkV4Request, ValidationResult.ValidationResultBuilder resultBuilder) {
        if (networkV4Request != null) {
            validateNetworkCidr(networkV4Request, resultBuilder);
            validateVpcId(networkV4Request, resultBuilder);
            validateSubnetIds(networkV4Request, resultBuilder);
        } else {
            resultBuilder.error(missingParamsErrorMsg());
        }
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return AWS;
    }

    private void validateNetworkCidr(EnvironmentNetworkV4Request networkV4Request, ValidationResult.ValidationResultBuilder resultBuilder) {
        if (missingExistingNetwork(networkV4Request) && missingNetworkCidr(networkV4Request)) {
            resultBuilder.error(missingParamErrorMessage("networkCidr"));
        }
    }

    private void validateVpcId(EnvironmentNetworkV4Request networkV4Request, ValidationResult.ValidationResultBuilder resultBuilder) {
        if (missingNetworkCidr(networkV4Request) && missingVpcId(networkV4Request)) {
            resultBuilder.error(missingParamErrorMessage("VPC identifier(vpcId)'"));
        }
    }

    private void validateSubnetIds(EnvironmentNetworkV4Request networkV4Request, ValidationResult.ValidationResultBuilder resultBuilder) {
        if (missingNetworkCidr(networkV4Request) && missingSubnetIds(networkV4Request)) {
            resultBuilder.error(missingParamErrorMessage("subnet identifier(subnetIds)'"));
        }
    }

    private boolean missingExistingNetwork(EnvironmentNetworkV4Request networkV4Request) {
        return missingSubnetIds(networkV4Request) || missingVpcId(networkV4Request);
    }

    private boolean missingSubnetIds(EnvironmentNetworkV4Request networkV4Request) {
        return networkV4Request.getSubnetIds() == null || networkV4Request.getSubnetIds().isEmpty();
    }

    private boolean missingVpcId(EnvironmentNetworkV4Request networkV4Request) {
        return networkV4Request.getAws() == null || !StringUtils.isNotEmpty(networkV4Request.getAws().getVpcId());
    }

    private boolean missingNetworkCidr(EnvironmentNetworkV4Request networkV4Request) {
        return !StringUtils.isNotEmpty(networkV4Request.getNetworkCidr());
    }

}
