package com.sequenceiq.cloudbreak.service.workspace;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
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

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.requests.ChangeWorkspaceUsersV4Request;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.authorization.UmsAuthorizationService;
import com.sequenceiq.cloudbreak.authorization.WorkspaceRole;
import com.sequenceiq.cloudbreak.domain.workspace.Tenant;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.repository.workspace.WorkspaceRepository;
import com.sequenceiq.cloudbreak.service.Clock;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.user.UserService;

@RunWith(MockitoJUnitRunner.class)
public class WorkspaceServiceTest {

    private static final String WORKSPACE_NAME = "test-workspace";

    private static final String TENANT_NAME = "test-tenant";

    private static final String USER_ID_2 = "user2";

    private static final String USER_ID_3 = "user3";

    @Spy
    private final TransactionService transactionService = new TransactionService();

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private UserService userService;

    @Mock
    private WorkspaceModificationVerifierService verifierService;

    @Spy
    private UmsAuthorizationService umsAuthorizationService;

    @Mock
    private Clock clock;

    @InjectMocks
    private final WorkspaceService underTest = new WorkspaceService();

    private final Tenant testTenant = new Tenant();

    private final Workspace testWorkspace = new Workspace();

    private final User initiator = new User();

    @Before
    public void setup() throws TransactionExecutionException {
        initiator.setId(1L);
        initiator.setUserId("initiator");
        initiator.setTenant(testTenant);
        initiator.setUserCrn("crn:altus:iam:us-west-1:1:user:1");
        testTenant.setId(1L);
        testTenant.setName(TENANT_NAME);
        testWorkspace.setName(WORKSPACE_NAME);
        testWorkspace.setId(1L);
        testWorkspace.setTenant(testTenant);
        doAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get()).when(transactionService).required(any());
        when(workspaceRepository.getByName(anyString(), eq(testTenant))).thenReturn(testWorkspace);
    }

    @Test
    public void testRemoveUsers() {
        doNothing().when(umsAuthorizationService).removeResourceRolesOfUserInWorkspace(anySet(), any());

        Set<String> userIds = new HashSet<>();
        userIds.add(USER_ID_2);
        userIds.add(USER_ID_3);

        User user2 = TestUtil.user(2L, USER_ID_2);
        User user3 = TestUtil.user(3L, USER_ID_3);
        Set<User> users = Set.of(user2, user3);

        when(userService.getByUsersIds(userIds)).thenReturn(users);

        Set<User> result = underTest.removeUsers(WORKSPACE_NAME, userIds, initiator);

        assertEquals(2L, result.size());
        assertTrue(result.contains(user2));
        assertTrue(result.contains(user3));

        verify(umsAuthorizationService, times(1)).removeResourceRolesOfUserInWorkspace(anySet(), any());
    }

    @Test
    public void testAddUsers() {
        User user2 = TestUtil.user(2L, USER_ID_2);
        User user3 = TestUtil.user(3L, USER_ID_3);
        Set<ChangeWorkspaceUsersV4Request> changeWorkspaceUsersV4Requests = changeWorkspaceUsersJsons(WorkspaceRole.WORKSPACEMANAGER);

        when(userService.getByUsersIds(anySet())).thenReturn(Sets.newHashSet(user2, user3));
        doNothing().when(verifierService).authorizeWorkspaceManipulation(any(), any(), any(), anyString());
        doNothing().when(verifierService).validateUsersAreNotInTheWorkspaceYet(any(), any(), anySet());
        doNothing().when(umsAuthorizationService).assignResourceRoleToUserInWorkspace(any(), any(), eq(WorkspaceRole.WORKSPACEMANAGER));

        Set<User> users = underTest.addUsers(WORKSPACE_NAME, changeWorkspaceUsersV4Requests, initiator);

        assertEquals(2L, users.size());
        verify(umsAuthorizationService, times(2)).assignResourceRoleToUserInWorkspace(any(), any(), eq(WorkspaceRole.WORKSPACEMANAGER));
    }

    @Test
    public void testUpdateUsersWithSameRoles() {
        User user2 = TestUtil.user(2L, USER_ID_2);
        User user3 = TestUtil.user(3L, USER_ID_3);
        Set<ChangeWorkspaceUsersV4Request> changeWorkspaceUsersV4Requests = changeWorkspaceUsersJsons(WorkspaceRole.WORKSPACEMANAGER);

        when(userService.getByUsersIds(anySet())).thenReturn(Sets.newHashSet(user2, user3));
        doNothing().when(verifierService).authorizeWorkspaceManipulation(any(), any(), any(), anyString());
        doNothing().when(verifierService).ensureWorkspaceManagementForUserUpdates(any(), any(), anySet());
        doNothing().when(verifierService).validateAllUsersAreAlreadyInTheWorkspace(any(), any(), anySet());
        doNothing().when(verifierService).verifyDefaultWorkspaceUserUpdates(any(), any(), anySet());
        doReturn(Sets.newHashSet(initiator, user2, user3)).when(umsAuthorizationService).getUsersOfWorkspace(any(), any());
        doReturn(Sets.newHashSet(WorkspaceRole.WORKSPACEMANAGER)).when(umsAuthorizationService).getUserRolesInWorkspace(any(), any());

        Set<User> result = underTest.updateUsers(WORKSPACE_NAME, changeWorkspaceUsersV4Requests, initiator);

        assertEquals(2L, result.size());
        assertTrue(result.stream().anyMatch(u -> u.getUserId().equals(USER_ID_2)));
        assertTrue(result.stream().anyMatch(u -> u.getUserId().equals(USER_ID_3)));
        verify(umsAuthorizationService, times(0)).assignResourceRoleToUserInWorkspace(any(), any(), any());
        verify(umsAuthorizationService, times(0)).unassignResourceRoleFromUserInWorkspace(any(), any(), any());
    }

    @Test
    public void testUpdateUsersWithDifferentRolesForOneOfTheUser() {
        User user2 = TestUtil.user(2L, USER_ID_2);
        User user3 = TestUtil.user(3L, USER_ID_3);
        Set<ChangeWorkspaceUsersV4Request> changeWorkspaceUsersV4Requests = changeWorkspaceUsersJsons(WorkspaceRole.WORKSPACEMANAGER);

        when(userService.getByUsersIds(anySet())).thenReturn(Sets.newHashSet(user2, user3));
        doNothing().when(verifierService).authorizeWorkspaceManipulation(any(), any(), any(), anyString());
        doNothing().when(verifierService).ensureWorkspaceManagementForUserUpdates(any(), any(), anySet());
        doNothing().when(verifierService).validateAllUsersAreAlreadyInTheWorkspace(any(), any(), anySet());
        doNothing().when(verifierService).verifyDefaultWorkspaceUserUpdates(any(), any(), anySet());
        doReturn(Sets.newHashSet(initiator, user2, user3)).when(umsAuthorizationService).getUsersOfWorkspace(any(), any());
        doReturn(Sets.newHashSet(WorkspaceRole.WORKSPACEMANAGER)).when(umsAuthorizationService).getUserRolesInWorkspace(eq(user2), any());
        doReturn(Sets.newHashSet(WorkspaceRole.WORKSPACEREADER)).when(umsAuthorizationService).getUserRolesInWorkspace(eq(user3), any());
        doNothing().when(umsAuthorizationService).assignResourceRoleToUserInWorkspace(any(), any(), any());
        doNothing().when(umsAuthorizationService).unassignResourceRoleFromUserInWorkspace(any(), any(), any());

        Set<User> result = underTest.updateUsers(WORKSPACE_NAME, changeWorkspaceUsersV4Requests, initiator);

        assertEquals(2L, result.size());
        assertTrue(result.stream().anyMatch(u -> u.getUserId().equals(USER_ID_2)));
        assertTrue(result.stream().anyMatch(u -> u.getUserId().equals(USER_ID_3)));
        verify(umsAuthorizationService, times(0)).assignResourceRoleToUserInWorkspace(eq(user2), any(), any());
        verify(umsAuthorizationService, times(0)).unassignResourceRoleFromUserInWorkspace(eq(user2), any(), any());
        verify(umsAuthorizationService, times(1)).assignResourceRoleToUserInWorkspace(eq(user3), any(), any());
        verify(umsAuthorizationService, times(1)).unassignResourceRoleFromUserInWorkspace(eq(user3), any(), any());
    }

    @Test
    public void testUpdateUsersWithDifferentRolesForEveryUser() {
        User user2 = TestUtil.user(2L, USER_ID_2);
        User user3 = TestUtil.user(3L, USER_ID_3);
        Set<ChangeWorkspaceUsersV4Request> changeWorkspaceUsersV4Requests = changeWorkspaceUsersJsons(WorkspaceRole.WORKSPACEMANAGER);

        when(userService.getByUsersIds(anySet())).thenReturn(Sets.newHashSet(user2, user3));
        doNothing().when(verifierService).authorizeWorkspaceManipulation(any(), any(), any(), anyString());
        doNothing().when(verifierService).ensureWorkspaceManagementForUserUpdates(any(), any(), anySet());
        doNothing().when(verifierService).validateAllUsersAreAlreadyInTheWorkspace(any(), any(), anySet());
        doNothing().when(verifierService).verifyDefaultWorkspaceUserUpdates(any(), any(), anySet());
        doReturn(Sets.newHashSet(initiator, user2, user3)).when(umsAuthorizationService).getUsersOfWorkspace(any(), any());
        doReturn(Sets.newHashSet(WorkspaceRole.WORKSPACEREADER)).when(umsAuthorizationService).getUserRolesInWorkspace(any(), any());
        doNothing().when(umsAuthorizationService).assignResourceRoleToUserInWorkspace(any(), any(), any());
        doNothing().when(umsAuthorizationService).unassignResourceRoleFromUserInWorkspace(any(), any(), any());

        Set<User> result = underTest.updateUsers(WORKSPACE_NAME, changeWorkspaceUsersV4Requests, initiator);

        assertEquals(2L, result.size());
        assertTrue(result.stream().anyMatch(u -> u.getUserId().equals(USER_ID_2)));
        assertTrue(result.stream().anyMatch(u -> u.getUserId().equals(USER_ID_3)));
        verify(umsAuthorizationService, times(1)).assignResourceRoleToUserInWorkspace(eq(user2), any(), any());
        verify(umsAuthorizationService, times(1)).unassignResourceRoleFromUserInWorkspace(eq(user2), any(), any());
        verify(umsAuthorizationService, times(1)).assignResourceRoleToUserInWorkspace(eq(user3), any(), any());
        verify(umsAuthorizationService, times(1)).unassignResourceRoleFromUserInWorkspace(eq(user3), any(), any());
    }

    @Test
    public void testChangeUsers() {
        User userToBeUpdated = TestUtil.user(2L, USER_ID_2);
        User userToBeAdded = TestUtil.user(3L, USER_ID_3);
        User userToBeDeleted = TestUtil.user(4L, "user4");

        when(userService.getByUsersIds(anySet())).thenReturn(Set.of(userToBeUpdated, userToBeAdded));
        doNothing().when(verifierService).authorizeWorkspaceManipulation(any(), any(), any(), anyString());
        doNothing().when(verifierService).ensureWorkspaceManagementForChangeUsers(anySet());
        doNothing().when(verifierService).verifyDefaultWorkspaceUserUpdates(any(), any(), anySet());
        doReturn(Sets.newHashSet(initiator, userToBeUpdated, userToBeDeleted)).when(umsAuthorizationService).getUsersOfWorkspace(any(), any());
        doReturn(Sets.newHashSet(WorkspaceRole.WORKSPACEMANAGER)).when(umsAuthorizationService).getUserRolesInWorkspace(any(), any());
        doNothing().when(umsAuthorizationService).assignResourceRoleToUserInWorkspace(any(), any(), eq(WorkspaceRole.WORKSPACEMANAGER));
        ArgumentCaptor<Set<User>> removableUsersCaptor = ArgumentCaptor.forClass(Set.class);
        doNothing().when(umsAuthorizationService).removeResourceRolesOfUserInWorkspace(removableUsersCaptor.capture(), any());

        Set<ChangeWorkspaceUsersV4Request> changeWorkspaceUsersV4Requests = changeWorkspaceUsersJsons(WorkspaceRole.WORKSPACEMANAGER);

        Set<User> result = underTest.changeUsers(WORKSPACE_NAME, changeWorkspaceUsersV4Requests, initiator);

        assertEquals(2L, result.size());
        assertTrue(result.contains(userToBeUpdated));
        assertTrue(result.contains(userToBeAdded));
        assertTrue(removableUsersCaptor.getValue().contains(userToBeDeleted));
        verify(umsAuthorizationService, times(1)).assignResourceRoleToUserInWorkspace(eq(userToBeAdded), any(), any());
        verify(umsAuthorizationService, times(1)).removeResourceRolesOfUserInWorkspace(anySet(), any());
        verify(umsAuthorizationService, times(1)).getUserRolesInWorkspace(eq(userToBeUpdated), any());
        verify(umsAuthorizationService, times(0)).assignResourceRoleToUserInWorkspace(eq(userToBeUpdated), any(), any());
        verify(umsAuthorizationService, times(0)).unassignResourceRoleFromUserInWorkspace(eq(userToBeUpdated), any(), any());
    }

    @Test
    public void testWorkspaceDeletion() {
        doNothing().when(verifierService).authorizeWorkspaceManipulation(any(), any(), any(), anyString());
        doNothing().when(verifierService).checkThatWorkspaceIsDeletable(any(), any(), any());
        doNothing().when(umsAuthorizationService).notifyAltusAboutResourceDeletion(any(), any());
        when(workspaceRepository.save(any())).thenReturn(testWorkspace);
        when(clock.getCurrentTimeMillis()).thenReturn(1L);

        underTest.deleteByNameForUser(WORKSPACE_NAME, initiator, testWorkspace);

        verify(workspaceRepository, times(1)).save(eq(testWorkspace));
    }

    @Test
    public void testWorkspaceCreation() {
        doNothing().when(umsAuthorizationService).assignResourceRoleToUserInWorkspace(any(), any(), eq(WorkspaceRole.WORKSPACEMANAGER));
        when(workspaceRepository.save(any())).thenReturn(testWorkspace);

        underTest.create(initiator, testWorkspace);

        verify(umsAuthorizationService, times(1)).assignResourceRoleToUserInWorkspace(eq(initiator), eq(testWorkspace), eq(WorkspaceRole.WORKSPACEMANAGER));
        assertNotNull(testWorkspace.getResourceCrn());
        assertTrue(testWorkspace.getResourceCrn().contains(testWorkspace.getId().toString() + ":" + Crn.ResourceType.WORKSPACE.toString()));
    }

    private Set<ChangeWorkspaceUsersV4Request> changeWorkspaceUsersJsons(WorkspaceRole role) {
        ChangeWorkspaceUsersV4Request json1 = TestUtil.changeWorkspaceUsersJson(USER_ID_2, Sets.newHashSet(role));
        ChangeWorkspaceUsersV4Request json2 = TestUtil.changeWorkspaceUsersJson(USER_ID_3, Sets.newHashSet(role));
        return Set.of(json1, json2);
    }
}