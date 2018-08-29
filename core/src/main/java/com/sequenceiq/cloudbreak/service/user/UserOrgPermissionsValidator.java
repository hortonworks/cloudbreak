package com.sequenceiq.cloudbreak.service.user;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.authorization.OrganizationPermissions;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.cloudbreak.controller.validation.Validator;
import com.sequenceiq.cloudbreak.domain.organization.UserOrgPermissions;

@Component
public class UserOrgPermissionsValidator implements Validator<UserOrgPermissions> {

    @Override
    public ValidationResult validate(UserOrgPermissions subject) {
        ValidationResultBuilder validationResultBuilder = ValidationResult.builder();
        subject.getPermissionSet().stream()
                .filter(OrganizationPermissions::isNotValid)
                .forEach(validationResultBuilder::error);
        return validationResultBuilder.build();
    }
}
