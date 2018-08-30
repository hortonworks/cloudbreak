package com.sequenceiq.cloudbreak.service.user;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anySet;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.authorization.OrganizationPermissions;
import com.sequenceiq.cloudbreak.domain.organization.UserOrgPermissions;
import com.sequenceiq.cloudbreak.repository.organization.UserOrgPermissionsRepository;

@RunWith(MockitoJUnitRunner.class)
public class UserOrgPermissionsServiceTest {

    @Mock
    private UserOrgPermissionsRepository userOrgPermissionsRepository;

    @Spy
    private UserOrgPermissionsValidator userOrgPermissionsValidator;

    @InjectMocks
    private UserOrgPermissionsService underTest;

    @Test
    public void saveAllWithValidPermissions() {
        Set<String> permissions = getPermissions();
        UserOrgPermissions userOrgPermissions1 = new UserOrgPermissions();
        userOrgPermissions1.setPermissionSet(permissions);
        UserOrgPermissions userOrgPermissions2 = new UserOrgPermissions();
        userOrgPermissions2.setPermissionSet(permissions);
        Set<UserOrgPermissions> userOrgPermissionSet = Set.of(userOrgPermissions1, userOrgPermissions2);

        when(userOrgPermissionsRepository.saveAll(anySet())).thenReturn(userOrgPermissionSet);

        Iterable<UserOrgPermissions> result = underTest.saveAll(userOrgPermissionSet);

        assertNotNull(result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void saveAllWithInvalidPermissions() {
        Set<String> permissions = getPermissions();
        permissions.add("INVALID:PERMISSION");
        UserOrgPermissions userOrgPermissions1 = new UserOrgPermissions();
        userOrgPermissions1.setPermissionSet(permissions);
        UserOrgPermissions userOrgPermissions2 = new UserOrgPermissions();
        userOrgPermissions2.setPermissionSet(permissions);
        Set<UserOrgPermissions> userOrgPermissionSet = Set.of(userOrgPermissions1, userOrgPermissions2);

        Iterable<UserOrgPermissions> result = underTest.saveAll(userOrgPermissionSet);

        assertNotNull(result);
    }

    private Set<String> getPermissions() {
        return Arrays.stream(OrganizationPermissions.values()).map(OrganizationPermissions::value)
                .collect(Collectors.toSet());
    }
}