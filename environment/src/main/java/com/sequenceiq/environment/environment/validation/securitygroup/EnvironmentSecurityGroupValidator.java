package com.sequenceiq.environment.environment.validation.securitygroup;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.util.ValidationResult;
import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.environment.dto.EnvironmentCreationDto;
import com.sequenceiq.environment.environment.dto.SecurityAccessDto;

public interface EnvironmentSecurityGroupValidator {

    void validate(EnvironmentCreationDto environmentDto, ValidationResult.ValidationResultBuilder resultBuilder);

    CloudPlatform getCloudPlatform();

    default String networkIdMustBePresented(String cloudPlatform) {
        return String.format("The '%s' network id has to be presented!", cloudPlatform);
    }

    default String securityGroupNotInTheSameVpc(String securityGroupId) {
        return String.format("The '%s' security group must be in the same vpc what you defined in the request!", securityGroupId);
    }

    default boolean isSecurityGroupIdDefined(SecurityAccessDto securityAccessDto) {
        return !Strings.isNullOrEmpty(securityAccessDto.getSecurityGroupIdForKnox())
                && !Strings.isNullOrEmpty(securityAccessDto.getDefaultSecurityGroupId());
    }
}
