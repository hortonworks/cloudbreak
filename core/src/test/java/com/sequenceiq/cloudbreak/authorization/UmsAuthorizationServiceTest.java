package com.sequenceiq.cloudbreak.authorization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.access.AccessDeniedException;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.repository.workspace.WorkspaceRepository;
import com.sequenceiq.cloudbreak.service.user.UserService;

@RunWith(MockitoJUnitRunner.class)
public class UmsAuthorizationServiceTest {

    private static final String WORKSPACE_READER_ROLE_CRN = "crn:altus:iam:us-west-1:1234:resourceRole:WorkspaceReader";

    private static final String WORKSAPCE_WRITER_ROLE_CRN = "crn:altus:iam:us-west-1:1234:resourceRole:WorkspaceWriter";

    private static final String WORKSPACE_CRN = "crn:altus:iam:us-west-1:1234:workspace:1234";

    private static final String USER_ID = "userId";

    private static final String USER_CRN = "crn:altus:iam:us-west-1:1234:user:" + USER_ID;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private GrpcUmsClient umsClient;

    @Spy
    private WorkspaceRepository workspaceRepository;

    @Spy
    private UserService userService;

    @InjectMocks
    private UmsAuthorizationService underTest;

    @Before
    public void setup() {
        when(umsClient.isUmsUsable(anyString())).thenReturn(true);
    }

    @Test
    public void testCheckRight() {
        when(umsClient.checkRight(anyString(), anyString(), anyString(), anyString())).thenReturn(false);

        thrown.expect(AccessDeniedException.class);
        thrown.expectMessage("You have no right to perform write on database config in workspace workspace@ws.com.");

        underTest.checkRightOfUserForResource(createUser(), createWorkspace(), WorkspaceResource.DATABASE, ResourceAction.WRITE);
    }

    @Test
    public void testGetUserRoles() {
        User user = createUser();
        when(umsClient.listResourceRoleAssigments(eq(user.getUserCrn()), anyString())).thenReturn(Lists.newArrayList(
                createResourceRoleAssigment(WORKSPACE_READER_ROLE_CRN, WORKSPACE_CRN),
                createResourceRoleAssigment(WORKSPACE_READER_ROLE_CRN, "crn:altus:iam:us-west-1:1234:other:1234"),
                createResourceRoleAssigment(WORKSAPCE_WRITER_ROLE_CRN, WORKSPACE_CRN)
        ));

        Set<WorkspaceRole> userRolesInWorkspace = underTest.getUserRolesInWorkspace(user, createWorkspace());

        assertEquals(2L, userRolesInWorkspace.size());
        assertTrue(userRolesInWorkspace.contains(WorkspaceRole.WORKSPACEWRITER));
        assertTrue(userRolesInWorkspace.contains(WorkspaceRole.WORKSPACEREADER));
    }

    @Test
    public void testRemoveResourceRolesOfUser() {
        doNothing().when(umsClient).unassignResourceRole(anyString(), anyString(), anyString(), anyString());
        when(umsClient.listResourceRoleAssigments(anyString(), anyString())).thenReturn(Lists.newArrayList(
                createResourceRoleAssigment(WORKSPACE_READER_ROLE_CRN, WORKSPACE_CRN),
                createResourceRoleAssigment(WORKSAPCE_WRITER_ROLE_CRN, WORKSPACE_CRN)
        ));

        underTest.removeResourceRolesOfUserInWorkspace(Sets.newHashSet(createUser()), createWorkspace());

        verify(umsClient, times(1)).unassignResourceRole(eq(USER_CRN), eq(WORKSPACE_CRN), eq(WORKSPACE_READER_ROLE_CRN), anyString());
        verify(umsClient, times(1)).unassignResourceRole(eq(USER_CRN), eq(WORKSPACE_CRN), eq(WORKSAPCE_WRITER_ROLE_CRN), anyString());
    }

    @Test
    public void testGetWorkspacesForUser() {
        when(umsClient.listResourceRoleAssigments(anyString(), anyString())).thenReturn(Lists.newArrayList(
                createResourceRoleAssigment(WORKSPACE_READER_ROLE_CRN, WORKSPACE_CRN),
                createResourceRoleAssigment(WORKSAPCE_WRITER_ROLE_CRN, WORKSPACE_CRN)
        ));
        ArgumentCaptor<Set<String>> findCaptor = ArgumentCaptor.forClass(Set.class);
        doReturn(Sets.newHashSet(createWorkspace())).when(workspaceRepository).findAllByCrn(findCaptor.capture());

        Set<Workspace> workspacesOfCurrentUser = underTest.getWorkspacesOfCurrentUser(createUser());

        assertEquals(1, findCaptor.getValue().size());
        assertEquals(WORKSPACE_CRN, findCaptor.getValue().iterator().next());
        assertEquals(1, workspacesOfCurrentUser.size());
    }

    @Test
    public void testGetUsersOfWorkspace() {
        User user = createUser();
        when(umsClient.listAssigneesOfResource(eq(USER_CRN), eq(WORKSPACE_CRN), anyString())).thenReturn(Lists.newArrayList(createResourceAssignee(USER_CRN)));
        ArgumentCaptor<Set<String>> findCaptor = ArgumentCaptor.forClass(Set.class);
        doReturn(Sets.newHashSet(user)).when(userService).getByUsersIds(findCaptor.capture());

        Set<User> usersOfWorkspace = underTest.getUsersOfWorkspace(user, createWorkspace());

        assertEquals(1, usersOfWorkspace.size());
        assertEquals(1, findCaptor.getValue().size());
        assertEquals(USER_ID, findCaptor.getValue().iterator().next());
    }

    private User createUser() {
        User user = TestUtil.user(1L, USER_ID);
        user.setUserCrn(USER_CRN);
        return user;
    }

    private Workspace createWorkspace() {
        Workspace workspace = TestUtil.workspace(1L, "workspace@ws.com");
        workspace.setResourceCrn(WORKSPACE_CRN);
        return workspace;
    }

    private UserManagementProto.ResourceAssignment createResourceRoleAssigment(String resourceRoleCrn, String resourceCrn) {
        return UserManagementProto.ResourceAssignment.newBuilder()
                .setResourceRoleCrn(resourceRoleCrn)
                .setResourceCrn(resourceCrn)
                .build();
    }

    private UserManagementProto.ResourceAssignee createResourceAssignee(String assigneeCrn) {
        return UserManagementProto.ResourceAssignee.newBuilder()
                .setAssigneeCrn(assigneeCrn)
                .build();
    }

}
