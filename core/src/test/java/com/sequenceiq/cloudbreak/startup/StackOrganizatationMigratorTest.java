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
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.organization.User;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.repository.organization.OrganizationRepository;
import com.sequenceiq.cloudbreak.repository.organization.TenantRepository;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.organization.OrganizationService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.user.UserOrgPermissionsService;
import com.sequenceiq.cloudbreak.service.user.UserProfileService;
import com.sequenceiq.cloudbreak.service.user.UserService;

@RunWith(MockitoJUnitRunner.class)
public class StackOrganizatationMigratorTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private UserOrgPermissionsService userOrgPermissionsService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private UserService userService;

    @Mock
    private UserProfileService userProfileService;

    @Spy
    private final TransactionService transactionService = new TransactionService();

    @InjectMocks
    private UserAndOrganizationMigrator userAndOrganizationMigrator;

    private UserMigrationResults userMigrationResults;

    @Mock
    private StackRepository stackRepository;

    @Mock
    private ClusterRepository clusterRepository;

    @Mock
    private StackService stackService;

    @Mock
    private OrganizationService organizationService;

    @Captor
    private ArgumentCaptor<Stack> stackSaveCaptor;

    @InjectMocks
    private StackOrganizatationMigrator underTest;

    @Before
    public void setup() throws TransactionExecutionException {
        doAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get()).when(transactionService).required(any());
        when(userService.getOrCreate(any(IdentityUser.class))).thenAnswer((Answer<User>) invocation -> {
            IdentityUser identityUser = invocation.getArgument(0);
            User user = new User();
            user.setUserId(identityUser.getUsername());
            return user;
        });
        when(organizationRepository.save(any(Organization.class)))
                .thenAnswer((Answer<Organization>) invocation -> invocation.getArgument(0));
        when(organizationRepository.getByName(anyString(), any())).thenReturn(null);
        when(userOrgPermissionsService.findForUserAndOrganization(any(), any())).thenReturn(null);

        List<IdentityUser> identityUsers = List.of(
                new IdentityUser("1", "1@hw.com", "1",
                        Collections.emptyList(), "1", "1", Date.from(Instant.now())),
                new IdentityUser("2", "2@hw.com", "2",
                        Collections.emptyList(), "1", "1", Date.from(Instant.now()))
        );

        userAndOrganizationMigrator.setUaaStartupTimeoutSec(300L);
        when(userDetailsService.getAllUsers(null)).thenReturn(identityUsers);
        userMigrationResults = userAndOrganizationMigrator.migrateUsersAndOrgs();

        when(organizationService.getDefaultOrganizationForUser(any(User.class))).thenAnswer((Answer<Organization>) invocation -> {
            User user = invocation.getArgument(0);
            Organization organization = new Organization();
            organization.setName(user.getUserId());
            return organization;
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

        // orphaned
        Stack stack4 = new Stack();
        stack4.setId(4L);
        stack4.setName("stack4");
        stack4.setOwner("3");
        stack4.setAccount("3");
        Cluster cluster4 = new Cluster();
        cluster4.setId(4L);
        cluster4.setStack(stack4);
        stack4.setCluster(cluster4);

        Set<Stack> stacks = Set.of(stack1, stack2, stack3, stack4);
        Set<Cluster> clusters = Set.of(cluster1, cluster2, cluster3, cluster4);
        when(stackRepository.findAllAliveWithNoOrganizationOrUser()).thenReturn(stacks);
        when(clusterRepository.findAllWithNoOrganization()).thenReturn(clusters);

        underTest.migrateStackOrgAndCreator(userMigrationResults);

        verify(stackRepository, times(4)).save(stackSaveCaptor.capture());

        List<Stack> savedStacks = stackSaveCaptor.getAllValues();
        assertEquals("1@hw.com", savedStacks.get(0).getCreator().getUserId());
        assertEquals("1@hw.com", savedStacks.get(0).getCluster().getOrganization().getName());
        assertEquals("1@hw.com", savedStacks.get(0).getOrganization().getName());
        assertEquals("stack1", savedStacks.get(0).getName());

        assertEquals("1@hw.com", savedStacks.get(1).getCreator().getUserId());
        assertEquals("1@hw.com", savedStacks.get(1).getOrganization().getName());
        assertEquals("1@hw.com", savedStacks.get(1).getCluster().getOrganization().getName());
        assertEquals("stack2", savedStacks.get(1).getName());

        assertEquals("2@hw.com", savedStacks.get(2).getCreator().getUserId());
        assertEquals("2@hw.com", savedStacks.get(2).getOrganization().getName());
        assertEquals("2@hw.com", savedStacks.get(2).getCluster().getOrganization().getName());
        assertEquals("stack3", savedStacks.get(2).getName());

        assertEquals("1@hw.com", savedStacks.get(3).getCreator().getUserId());
        assertEquals("OrphanedResources", savedStacks.get(3).getOrganization().getName());
        assertEquals("OrphanedResources", savedStacks.get(3).getCluster().getOrganization().getName());
        assertEquals("stack4", savedStacks.get(3).getName());
    }
}