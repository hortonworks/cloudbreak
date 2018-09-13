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

import com.sequenceiq.cloudbreak.authorization.WorkspacePermissions;
import com.sequenceiq.cloudbreak.domain.workspace.UserWorkspacePermissions;
import com.sequenceiq.cloudbreak.repository.workspace.UserWorkspacePermissionsRepository;

@RunWith(MockitoJUnitRunner.class)
public class UserWorkspacePermissionsServiceTest {

    @Mock
    private UserWorkspacePermissionsRepository userWorkspacePermissionsRepository;

    @Mock
    private CachedUserService cachedUserService;

    @Spy
    private UserWorkspacePermissionsValidator userWorkspacePermissionsValidator;

    @InjectMocks
    private UserWorkspacePermissionsService underTest;

    @Test
    public void saveAllWithValidPermissions() {
        Set<String> permissions = getPermissions();
        UserWorkspacePermissions userWorkspacePermissions1 = new UserWorkspacePermissions();
        userWorkspacePermissions1.setPermissionSet(permissions);
        UserWorkspacePermissions userWorkspacePermissions2 = new UserWorkspacePermissions();
        userWorkspacePermissions2.setPermissionSet(permissions);
        Set<UserWorkspacePermissions> userWorkspacePermissionSet = Set.of(userWorkspacePermissions1, userWorkspacePermissions2);

        when(userWorkspacePermissionsRepository.saveAll(anySet())).thenReturn(userWorkspacePermissionSet);

        Iterable<UserWorkspacePermissions> result = underTest.saveAll(userWorkspacePermissionSet);

        assertNotNull(result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void saveAllWithInvalidPermissions() {
        Set<String> permissions = getPermissions();
        permissions.add("INVALID:PERMISSION");
        UserWorkspacePermissions userWorkspacePermissions1 = new UserWorkspacePermissions();
        userWorkspacePermissions1.setPermissionSet(permissions);
        UserWorkspacePermissions userWorkspacePermissions2 = new UserWorkspacePermissions();
        userWorkspacePermissions2.setPermissionSet(permissions);
        Set<UserWorkspacePermissions> userWorkspacePermissionSet = Set.of(userWorkspacePermissions1, userWorkspacePermissions2);

        Iterable<UserWorkspacePermissions> result = underTest.saveAll(userWorkspacePermissionSet);

        assertNotNull(result);
    }

    private Set<String> getPermissions() {
        return Arrays.stream(WorkspacePermissions.values()).map(WorkspacePermissions::value)
                .collect(Collectors.toSet());
    }
}