package com.sequenceiq.cloudbreak.service.user;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

import com.sequenceiq.cloudbreak.authorization.OrganizationPermissions;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.domain.organization.UserOrgPermissions;

public class UserOrgPermissionsValidatorTest {

    private final UserOrgPermissionsValidator underTest = new UserOrgPermissionsValidator();

    @Test
    public void validateValidateWithInvalidPermissions() {
        UserOrgPermissions userOrgPermissions = new UserOrgPermissions();
        Set<String> permissions = Arrays.stream(OrganizationPermissions.values()).map(OrganizationPermissions::value)
                .collect(Collectors.toSet());
        String invalidPermission = "RANDOM:MODIFY";
        permissions.add(invalidPermission);
        userOrgPermissions.setPermissionSet(permissions);

        ValidationResult validationResult = underTest.validate(userOrgPermissions);

        assertTrue(validationResult.getFormattedErrors().contains(invalidPermission));
    }

    @Test
    public void validateValidateWithValidPermissions() {
        UserOrgPermissions userOrgPermissions = new UserOrgPermissions();
        Set<String> permissions = Arrays.stream(OrganizationPermissions.values()).map(OrganizationPermissions::value)
                .collect(Collectors.toSet());
        userOrgPermissions.setPermissionSet(permissions);

        ValidationResult validationResult = underTest.validate(userOrgPermissions);

        assertFalse(validationResult.hasError());
    }
}