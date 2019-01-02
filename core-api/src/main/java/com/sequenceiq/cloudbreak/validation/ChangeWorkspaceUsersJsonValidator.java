package com.sequenceiq.cloudbreak.validation;

import java.util.Set;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.requests.ChangeWorkspaceUsersV4Request;
import com.sequenceiq.cloudbreak.authorization.WorkspacePermissions;

public class ChangeWorkspaceUsersJsonValidator implements ConstraintValidator<ValidChangeWorkspaceUsersJson, ChangeWorkspaceUsersV4Request> {
    @Override
    public boolean isValid(ChangeWorkspaceUsersV4Request value, ConstraintValidatorContext context) {
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
