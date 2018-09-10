package com.sequenceiq.cloudbreak.validation;

import java.util.Set;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.sequenceiq.cloudbreak.api.model.users.ChangeWorkspaceUsersJson;
import com.sequenceiq.cloudbreak.authorization.WorkspacePermissions;

public class ChangeWorkspaceUsersJsonValidator implements ConstraintValidator<ValidChangeWorkspaceUsersJson, ChangeWorkspaceUsersJson> {
    @Override
    public boolean isValid(ChangeWorkspaceUsersJson value, ConstraintValidatorContext context) {
        if (!isPermissionsValid(value.getPermissions())) {
            ValidatorUtil.addConstraintViolation(context, "permissions must be valid", "status")
                    .disableDefaultConstraintViolation();
            return false;
        }
        return true;
    }

    private boolean isPermissionsValid(Set<String> permissions) {
        return permissions.stream().allMatch(WorkspacePermissions::isValid);
    }
}
