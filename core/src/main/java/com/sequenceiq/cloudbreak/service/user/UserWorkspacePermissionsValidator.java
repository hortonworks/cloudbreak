package com.sequenceiq.cloudbreak.service.user;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.authorization.WorkspacePermissions;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.cloudbreak.controller.validation.Validator;
import com.sequenceiq.cloudbreak.domain.workspace.UserWorkspacePermissions;

@Component
public class UserWorkspacePermissionsValidator implements Validator<UserWorkspacePermissions> {

    @Override
    public ValidationResult validate(UserWorkspacePermissions subject) {
        ValidationResultBuilder validationResultBuilder = ValidationResult.builder();
        subject.getPermissionSet().stream()
                .filter(WorkspacePermissions::isNotValid)
                .forEach(validationResultBuilder::error);
        return validationResultBuilder.build();
    }
}
