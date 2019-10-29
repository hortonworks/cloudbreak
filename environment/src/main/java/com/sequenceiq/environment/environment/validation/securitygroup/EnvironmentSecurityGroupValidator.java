package com.sequenceiq.environment.environment.validation.securitygroup;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.util.ValidationResult;
import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.SecurityAccessDto;

public interface EnvironmentSecurityGroupValidator {

    void validate(EnvironmentDto environmentDto, ValidationResult.ValidationResultBuilder resultBuilder);

    CloudPlatform getCloudPlatform();

    default String networkIdMustBePresented(String cloudPlatform) {
        return String.format("The '%s' network id has to be presented!", cloudPlatform);
    }

    default String securityGroupIdsMustBePresented() {
        return String.format("You must present two securitygroupId!");
    }

    default String securityGroupNotInTheSameVpc(String securityGroupId) {
        return String.format("The '%s' security group must be in the same vpc what you defined in the request!", securityGroupId);
    }

    default boolean isSecurityGroupIdDefined(SecurityAccessDto securityAccessDto) {
        return !Strings.isNullOrEmpty(securityAccessDto.getSecurityGroupIdForKnox())
                && !Strings.isNullOrEmpty(securityAccessDto.getDefaultSecurityGroupId());
    }

    default boolean onlyOneSecurityGroupIdDefined(SecurityAccessDto securityAccessDto) {
        return defaultGroupNotDefined(securityAccessDto) || knoxGroupNotDefined(securityAccessDto);
    }

    default boolean defaultGroupNotDefined(SecurityAccessDto securityAccessDto) {
        return !Strings.isNullOrEmpty(securityAccessDto.getSecurityGroupIdForKnox())
                && Strings.isNullOrEmpty(securityAccessDto.getDefaultSecurityGroupId());
    }

    default boolean knoxGroupNotDefined(SecurityAccessDto securityAccessDto) {
        return Strings.isNullOrEmpty(securityAccessDto.getSecurityGroupIdForKnox())
                && !Strings.isNullOrEmpty(securityAccessDto.getDefaultSecurityGroupId());
    }
}
