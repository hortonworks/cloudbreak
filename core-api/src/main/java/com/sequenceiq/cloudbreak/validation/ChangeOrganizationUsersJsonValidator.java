package com.sequenceiq.cloudbreak.validation;

import java.util.Set;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.sequenceiq.cloudbreak.api.model.users.ChangeOrganizationUsersJson;

public class ChangeOrganizationUsersJsonValidator implements ConstraintValidator<ValidChangeOrganizationUsersJson, ChangeOrganizationUsersJson> {
    @Override
    public boolean isValid(ChangeOrganizationUsersJson value, ConstraintValidatorContext context) {
        boolean result;
        if (!isPermissionsValid(value.getPermissions())) {
            ValidatorUtil.addConstraintViolation(context, "permissions must be valid", "status")
                    .disableDefaultConstraintViolation();
            result = false;
        } else {
            result = true;
        }
        return result;
    }

    private boolean isPermissionsValid(Set<String> permissions) {
        return permissions.stream().allMatch(Permissions::isValid);
    }

}
