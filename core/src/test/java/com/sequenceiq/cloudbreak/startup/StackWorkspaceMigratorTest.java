package com.sequenceiq.cloudbreak.startup;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.service.user.UserDetailsService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
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

    @Captor
    private ArgumentCaptor<Stack> stackSaveCaptor;

    @InjectMocks
    private StackWorkspaceMigrator underTest;

    @Before
    public void setup() throws TransactionExecutionException {
        doAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get()).when(transactionService).required(any());
        when(userService.getOrCreate(any(IdentityUser.class))).thenAnswer((Answer<User>) invocation -> {
            IdentityUser identityUser = invocation.getArgument(0);
            User user = new User();
            user.setUserId(identityUser.getUsername());
            return user;
        });
        when(workspaceRepository.save(any(Workspace.class)))
                .thenAnswer((Answer<Workspace>) invocation -> invocation.getArgument(0));
        when(workspaceRepository.getByName(anyString(), any())).thenReturn(null);
        when(userWorkspacePermissionsService.findForUserAndWorkspace(any(), any())).thenReturn(null);

        List<IdentityUser> identityUsers = List.of(
                new IdentityUser("1", "1@hw.com", "1",
                        Collections.emptyList(), "1", "1", Date.from(Instant.now())),
                new IdentityUser("2", "2@hw.com", "2",
                        Collections.emptyList(), "1", "1", Date.from(Instant.now()))
        );

        userAndWorkspaceMigrator.setUaaStartupTimeoutSec(300L);
        when(userDetailsService.getAllUsers(null)).thenReturn(identityUsers);
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
        Stack stack1 = new Stack();
        stack1.setId(1L);
        stack1.setName("stack1");
        stack1.setOwner("1");
        stack1.setAccount("1");
        Cluster cluster1 = new Cluster();
        cluster1.setId(1L);
        cluster1.setStack(stack1);
        stack1.setCluster(cluster1);

        Stack stack2 = new Stack();
        stack2.setId(2L);
        stack2.setName("stack2");
        stack2.setOwner("1");
        stack2.setAccount("1");
        Cluster cluster2 = new Cluster();
        cluster2.setId(2L);
        cluster2.setStack(stack2);
        stack2.setCluster(cluster2);

        Stack stack3 = new Stack();
        stack3.setId(3L);
        stack3.setName("stack3");
        stack3.setOwner("2");
        stack3.setAccount("2");
        Cluster cluster3 = new Cluster();
        cluster3.setId(3L);
        cluster3.setStack(stack3);
        stack3.setCluster(cluster3);

        // stack not set for cluster
        Stack stack5 = new Stack();
        stack5.setId(5L);
        stack5.setName("stack5");
        stack5.setOwner("2");
        stack5.setAccount("2");
        Cluster cluster5 = new Cluster();
        cluster5.setId(5L);
        stack5.setCluster(cluster5);

        Set<Stack> stacks = Set.of(stack1, stack2, stack3, stack5);
        Set<Cluster> clusters = Set.of(cluster1, cluster2, cluster3, cluster5);
        when(stackRepository.findAllAliveWithNoWorkspaceOrUser()).thenReturn(stacks);
        when(clusterRepository.findAllWithNoWorkspace()).thenReturn(clusters);

        underTest.migrateStackWorkspaceAndCreator(userMigrationResults);

        verify(stackRepository, times(4)).save(stackSaveCaptor.capture());

        List<Stack> savedStacks = stackSaveCaptor.getAllValues();
        assertEquals("1@hw.com", savedStacks.get(0).getCreator().getUserId());
        assertEquals("1@hw.com", savedStacks.get(0).getCluster().getWorkspace().getName());
        assertEquals("1@hw.com", savedStacks.get(0).getWorkspace().getName());
        assertEquals("stack1", savedStacks.get(0).getName());

        assertEquals("1@hw.com", savedStacks.get(1).getCreator().getUserId());
        assertEquals("1@hw.com", savedStacks.get(1).getWorkspace().getName());
        assertEquals("1@hw.com", savedStacks.get(1).getCluster().getWorkspace().getName());
        assertEquals("stack2", savedStacks.get(1).getName());

        assertEquals("2@hw.com", savedStacks.get(2).getCreator().getUserId());
        assertEquals("2@hw.com", savedStacks.get(2).getWorkspace().getName());
        assertEquals("2@hw.com", savedStacks.get(2).getCluster().getWorkspace().getName());
        assertEquals("stack3", savedStacks.get(2).getName());
    }
}