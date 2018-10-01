package com.sequenceiq.cloudbreak.startup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.common.service.user.UserDetailsService;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.repository.workspace.TenantRepository;
import com.sequenceiq.cloudbreak.repository.workspace.WorkspaceRepository;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.user.UserProfileService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.user.UserWorkspacePermissionsService;

@RunWith(MockitoJUnitRunner.class)
public class UserAndWorkspaceMigratorTest {

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private UserWorkspacePermissionsService userWorkspacePermissionsService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private UserService userService;

    @Mock
    private UserProfileService userProfileService;

    @Spy
    private final TransactionService transactionService = new TransactionService();

    @InjectMocks
    private UserAndWorkspaceMigrator underTest;

    @Before
    public void setup() throws TransactionExecutionException {
        doAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get()).when(transactionService).required(any());
        when(userService.getOrCreate(any(CloudbreakUser.class))).thenAnswer((Answer<User>) invocation -> {
            CloudbreakUser cloudbreakUser = invocation.getArgument(0);
            User user = new User();
            user.setUserId(cloudbreakUser.getUsername());
            return user;
        });
        when(workspaceRepository.save(any(Workspace.class)))
                .thenAnswer((Answer<Workspace>) invocation -> invocation.getArgument(0));
        when(workspaceRepository.getByName(anyString(), any())).thenReturn(null);
        when(userWorkspacePermissionsService.findForUserAndWorkspace(any(), any())).thenReturn(null);
        underTest.setUaaStartupTimeoutSec(300L);
    }

    @Test
    public void testWithUsersInCommonAccounts() throws TransactionExecutionException {
        List<CloudbreakUser> cloudbreakUsers = List.of(
                new CloudbreakUser("1", "1@hw.com", "1"),
                new CloudbreakUser("2", "2@hw.com", "1"),
                new CloudbreakUser("3", "3@hw.com", "3"),
                new CloudbreakUser("4", "4@hw.com", "4"),
                new CloudbreakUser("5", "5@hw.com", "4"),
                new CloudbreakUser("6", "6@hw.com", "6"));


        when(userDetailsService.getAllUsers(null)).thenReturn(cloudbreakUsers);

        UserMigrationResults userMigrationResults = underTest.migrateUsersAndWorkspaces();
        assertEquals(6, userMigrationResults.getOwnerIdToUser().size());
        assertNotNull(userMigrationResults.getWorkspaceForOrphanedResources());
    }
}