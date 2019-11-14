package com.sequenceiq.environment.environment.validation.securitygroup;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.SecurityAccessDto;

public interface EnvironmentSecurityGroupValidator {

    void validate(EnvironmentDto environmentDto, ValidationResult.ValidationResultBuilder resultBuilder);

    CloudPlatform getCloudPlatform();

    default String networkIdMustBePresent(String cloudPlatform) {
        return String.format("The '%s' network id has to be defined!", cloudPlatform);
    }

    default String securityGroupIdsMustBePresent() {
        return String.format("You must define two security group id-s, one for Knox and a default one!");
    }

    default String securityGroupNotInTheSameVpc(String securityGroupId) {
        return String.format("The '%s' security group must be in the same vpc that you defined in the request!", securityGroupId);
    }

    default String securityGroupNotInTheSameRegion(String securityGroupId, String region) {
        return String.format("The '%s' security group must be in the same region that you defined in the request (%s)!", securityGroupId, region);
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
