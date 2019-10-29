package com.sequenceiq.environment.environment.validation.securitygroup;

import static com.sequenceiq.environment.CloudPlatform.AZURE;

import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.util.ValidationResult;
import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.SecurityAccessDto;

@Component
public class AzureEnvironmentSecurityGroupValidator implements EnvironmentSecurityGroupValidator {

    public AzureEnvironmentSecurityGroupValidator() {
    }

    @Override
    public void validate(EnvironmentDto environmentDto, ValidationResult.ValidationResultBuilder resultBuilder) {
        SecurityAccessDto securityAccessDto = environmentDto.getSecurityAccess();
        if (securityAccessDto != null) {
            if (onlyOneSecurityGroupIdDefined(securityAccessDto)) {
                resultBuilder.error(securityGroupIdsMustBePresented());
                return;
            } else if (isSecurityGroupIdDefined(securityAccessDto)) {
                if (!Strings.isNullOrEmpty(environmentDto.getNetwork().getNetworkCidr())) {
                    resultBuilder.error(networkIdMustBePresented(getCloudPlatform().name()));
                    return;
                }
            }
        }
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return AZURE;
    }

}
