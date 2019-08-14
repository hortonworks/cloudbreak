package com.sequenceiq.cloudbreak.workspace.authorization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.workspace.authorization.api.WorkspaceRole;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.model.WorkspaceStatus;

@RunWith(MockitoJUnitRunner.class)
public class UmsWorkspaceAuthorizationServiceTest {

    private static final String WORKSPACE_READER_ROLE_CRN = "crn:cdp:iam:us-west-1:1234:resourceRole:WorkspaceReader";

    private static final String WORKSAPCE_WRITER_ROLE_CRN = "crn:cdp:iam:us-west-1:1234:resourceRole:WorkspaceWriter";

    private static final String WORKSPACE_CRN = "crn:cdp:iam:us-west-1:1234:workspace:1234";

    private static final String USER_ID = "userId";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:" + USER_ID;

    @Mock
    private GrpcUmsClient umsClient;

    @InjectMocks
    private UmsWorkspaceAuthorizationService underTest;

    @Test
    public void testGetUserRoles() {
        User user = createUser();
        when(umsClient.listResourceRoleAssigments(eq(user.getUserCrn()), eq(user.getUserCrn()), any())).thenReturn(Lists.newArrayList(
                createResourceRoleAssigment(WORKSPACE_READER_ROLE_CRN, WORKSPACE_CRN),
                createResourceRoleAssigment(WORKSPACE_READER_ROLE_CRN, "crn:cdp:iam:us-west-1:1234:other:1234"),
                createResourceRoleAssigment(WORKSAPCE_WRITER_ROLE_CRN, WORKSPACE_CRN)
        ));

        Set<WorkspaceRole> userRolesInWorkspace = underTest.getUserRolesInWorkspace(user, createWorkspace().getResourceCrn());

        assertEquals(2L, userRolesInWorkspace.size());
        assertTrue(userRolesInWorkspace.contains(WorkspaceRole.WORKSPACEWRITER));
        assertTrue(userRolesInWorkspace.contains(WorkspaceRole.WORKSPACEREADER));
    }

    @Test
    public void testRemoveResourceRolesOfUser() {
        doNothing().when(umsClient).unassignResourceRole(anyString(), anyString(), anyString(), any());
        when(umsClient.listResourceRoleAssigments(anyString(), anyString(), any())).thenReturn(Lists.newArrayList(
                createResourceRoleAssigment(WORKSPACE_READER_ROLE_CRN, WORKSPACE_CRN),
                createResourceRoleAssigment(WORKSAPCE_WRITER_ROLE_CRN, WORKSPACE_CRN)
        ));

        underTest.removeResourceRolesOfUserInWorkspace(Sets.newHashSet(createUser()), createWorkspace().getResourceCrn());

        verify(umsClient, times(1)).unassignResourceRole(eq(USER_CRN), eq(WORKSPACE_CRN), eq(WORKSPACE_READER_ROLE_CRN), any());
        verify(umsClient, times(1)).unassignResourceRole(eq(USER_CRN), eq(WORKSPACE_CRN), eq(WORKSAPCE_WRITER_ROLE_CRN), any());
    }

    private User createUser() {
        User user = new User();
        user.setUserId(USER_ID);
        user.setId(1L);
        user.setUserCrn(USER_CRN);
        return user;
    }

    private Workspace createWorkspace() {
        Workspace workspace = new Workspace();
        workspace.setStatus(WorkspaceStatus.ACTIVE);
        workspace.setName("workspace@ws.com");
        workspace.setId(1L);
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
