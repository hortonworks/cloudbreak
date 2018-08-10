package com.sequenceiq.cloudbreak.validation;

import java.util.Set;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.sequenceiq.cloudbreak.api.model.users.ChangeOrganizationUsersJson;
import com.sequenceiq.cloudbreak.authorization.OrganizationPermissions;

public class ChangeOrganizationUsersJsonValidator implements ConstraintValidator<ValidChangeOrganizationUsersJson, ChangeOrganizationUsersJson> {
    @Override
    public boolean isValid(ChangeOrganizationUsersJson value, ConstraintValidatorContext context) {
        if (!isPermissionsValid(value.getPermissions())) {
            ValidatorUtil.addConstraintViolation(context, "permissions must be valid", "status")
                    .disableDefaultConstraintViolation();
            return false;
        }
        return true;
    }

    private boolean isPermissionsValid(Set<String> permissions) {
        return permissions.stream().allMatch(OrganizationPermissions::isValid);
    }

}
