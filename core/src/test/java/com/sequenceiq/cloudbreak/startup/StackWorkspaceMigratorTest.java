package com.sequenceiq.cloudbreak.startup;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.common.service.user.UserDetailsService;
import com.sequenceiq.cloudbreak.domain.projection.StackWorkspaceView;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.repository.workspace.TenantRepository;
import com.sequenceiq.cloudbreak.repository.workspace.WorkspaceRepository;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.user.UserProfileService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.user.UserWorkspacePermissionsService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

@RunWith(MockitoJUnitRunner.class)
public class StackWorkspaceMigratorTest {

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
    private UserAndWorkspaceMigrator userAndWorkspaceMigrator;

    private UserMigrationResults userMigrationResults;

    @Mock
    private StackRepository stackRepository;

    @Mock
    private ClusterRepository clusterRepository;

    @Mock
    private StackService stackService;

    @Mock
    private WorkspaceService workspaceService;

    @InjectMocks
    private StackWorkspaceMigrator underTest;

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

        List<CloudbreakUser> cloudbreakUsers = List.of(
                new CloudbreakUser("1", "1@hw.com", "1"),
                new CloudbreakUser("2", "2@hw.com", "2")
        );

        userAndWorkspaceMigrator.setUaaStartupTimeoutSec(300L);
        when(userDetailsService.getAllUsers(null)).thenReturn(cloudbreakUsers);
        userMigrationResults = userAndWorkspaceMigrator.migrateUsersAndWorkspaces();

        when(workspaceService.getDefaultWorkspaceForUser(any(User.class))).thenAnswer((Answer<Workspace>) invocation -> {
            User user = invocation.getArgument(0);
            Workspace workspace = new Workspace();
            workspace.setName(user.getUserId());
            return workspace;
        });
    }

    @Test
    public void testStackMigration() throws TransactionExecutionException {
        StackWorkspaceView stack1 = new StackWorkspaceViewImpl(1L, "1", null);
        Cluster cluster1 = new Cluster();
        cluster1.setId(1L);

        StackWorkspaceView stack2 = new StackWorkspaceViewImpl(2L, "1", null);
        Cluster cluster2 = new Cluster();
        cluster2.setId(2L);

        StackWorkspaceView stack3 = new StackWorkspaceViewImpl(3L, "2", null);
        Cluster cluster3 = new Cluster();
        cluster3.setId(3L);

        // stack not set for cluster
        StackWorkspaceView stack5 = new StackWorkspaceViewImpl(5L, "2", null);
        Cluster cluster5 = new Cluster();
        cluster5.setId(5L);

        Set<StackWorkspaceView> stacks = Set.of(stack1, stack2, stack3, stack5);
        Set<Cluster> clusters = Set.of(cluster1, cluster2, cluster3, cluster5);
        when(stackRepository.findAllAliveWithNoWorkspaceOrUser()).thenReturn(stacks);
        when(clusterRepository.findAllWithNoWorkspace()).thenReturn(clusters);

        underTest.migrateStackWorkspaceAndCreator(userMigrationResults);

        ArgumentCaptor<Long> stackSaveCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<User> userSaveCaptor = ArgumentCaptor.forClass(User.class);
        ArgumentCaptor<Workspace> workspaceSaveCaptor = ArgumentCaptor.forClass(Workspace.class);
        verify(stackRepository, times(4)).updateCreatorAndWorkspaceById(stackSaveCaptor.capture(), userSaveCaptor.capture(), workspaceSaveCaptor.capture());

        List<Long> savedStacks = stackSaveCaptor.getAllValues();
        List<User> savedUsers = userSaveCaptor.getAllValues();
        List<Workspace> savedWorkspaces = workspaceSaveCaptor.getAllValues();
        assertEquals("1@hw.com", savedUsers.get(0).getUserId());
        assertEquals("1@hw.com", savedWorkspaces.get(0).getName());
        assertEquals(Long.valueOf(1L), savedStacks.get(0));

        assertEquals("1@hw.com", savedUsers.get(1).getUserId());
        assertEquals("1@hw.com", savedWorkspaces.get(1).getName());
        assertEquals(Long.valueOf(2L), savedStacks.get(1));

        assertEquals("2@hw.com", savedUsers.get(2).getUserId());
        assertEquals("2@hw.com", savedWorkspaces.get(2).getName());
        assertEquals(Long.valueOf(3), savedStacks.get(2));
    }

    private static class StackWorkspaceViewImpl implements StackWorkspaceView {

        private Long id;

        private String owner;

        private Workspace workspace;

        StackWorkspaceViewImpl(Long id, String owner, Workspace workspace) {
            this.id = id;
            this.owner = owner;
            this.workspace = workspace;
        }

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public String getOwner() {
            return owner;
        }

        @Override
        public Workspace getWorkspace() {
            return workspace;
        }
    }
}