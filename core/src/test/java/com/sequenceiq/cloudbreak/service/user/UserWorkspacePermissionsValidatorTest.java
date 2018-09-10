package com.sequenceiq.cloudbreak.service.user;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

import com.sequenceiq.cloudbreak.authorization.WorkspacePermissions;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.domain.workspace.UserWorkspacePermissions;

public class UserWorkspacePermissionsValidatorTest {

    private final UserWorkspacePermissionsValidator underTest = new UserWorkspacePermissionsValidator();

    @Test
    public void validateValidateWithInvalidPermissions() {
        UserWorkspacePermissions userWorkspacePermissions = new UserWorkspacePermissions();
        Set<String> permissions = Arrays.stream(WorkspacePermissions.values()).map(WorkspacePermissions::value)
                .collect(Collectors.toSet());
        String invalidPermission = "RANDOM:MODIFY";
        permissions.add(invalidPermission);
        userWorkspacePermissions.setPermissionSet(permissions);

        ValidationResult validationResult = underTest.validate(userWorkspacePermissions);

        assertTrue(validationResult.getFormattedErrors().contains(invalidPermission));
    }

    @Test
    public void validateValidateWithValidPermissions() {
        UserWorkspacePermissions userWorkspacePermissions = new UserWorkspacePermissions();
        Set<String> permissions = Arrays.stream(WorkspacePermissions.values()).map(WorkspacePermissions::value)
                .collect(Collectors.toSet());
        userWorkspacePermissions.setPermissionSet(permissions);

        ValidationResult validationResult = underTest.validate(userWorkspacePermissions);

        assertFalse(validationResult.hasError());
    }
}