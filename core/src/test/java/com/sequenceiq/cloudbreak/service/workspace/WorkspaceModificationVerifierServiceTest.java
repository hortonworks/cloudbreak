package com.sequenceiq.cloudbreak.service.workspace;

import static com.sequenceiq.cloudbreak.authorization.WorkspacePermissions.ALL_READ;
import static com.sequenceiq.cloudbreak.authorization.WorkspacePermissions.ALL_WRITE;
import static com.sequenceiq.cloudbreak.authorization.WorkspacePermissions.WORKSPACE_MANAGE;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.authorization.WorkspacePermissions.Action;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.workspace.Tenant;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.UserWorkspacePermissions;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.user.UserWorkspacePermissionsService;

@RunWith(SpringJUnit4ClassRunner.class)
public class WorkspaceModificationVerifierServiceTest {

    private static final String WORKSPACE_NAME = "test-workspace";

    private static final String TENANT_NAME = "test-tenant";

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private StackService stackService;

    @Mock
    private UserWorkspacePermissionsService userWorkspacePermissionsService;

    @InjectMocks
    private WorkspaceModificationVerifierService underTest;

    private final User initiator = TestUtil.user(1L, "initiator");

    private final Tenant testTenant = new Tenant();

    private final Workspace testWorkspace = TestUtil.workspace(1L, WORKSPACE_NAME);

    private final UserWorkspacePermissions initiatorWsPermissions = new UserWorkspacePermissions();

    @Before
    public void setup() {
        initiator.setTenant(testTenant);
        testTenant.setId(1L);
        testTenant.setName(TENANT_NAME);
        testWorkspace.setTenant(testTenant);
        initiatorWsPermissions.setUser(initiator);
        initiatorWsPermissions.setWorkspace(testWorkspace);
    }

    @Test
    public void testAuthorizeWorkspaceManipulationWithAccess() {
        initiatorWsPermissions.setPermissionSet(Set.of(ALL_READ.value(), ALL_WRITE.value(), WORKSPACE_MANAGE.value()));
        when(userWorkspacePermissionsService.findForUserAndWorkspace(initiator, testWorkspace)).thenReturn(initiatorWsPermissions);

        underTest.authorizeWorkspaceManipulation(initiator, testWorkspace, Action.MANAGE, "unauthorized");
    }

    @Test(expected = AccessDeniedException.class)
    public void testAuthorizeWorkspaceManipulationWithNoManagePermission() {
        initiatorWsPermissions.setPermissionSet(Set.of(ALL_READ.value(), ALL_WRITE.value()));
        when(userWorkspacePermissionsService.findForUserAndWorkspace(initiator, testWorkspace)).thenReturn(initiatorWsPermissions);

        underTest.authorizeWorkspaceManipulation(initiator, testWorkspace, Action.MANAGE, "unauthorized");
    }

    @Test(expected = AccessDeniedException.class)
    public void testAuthorizeWorkspaceManipulationWhenUserIsNotPartOfWs() {
        when(userWorkspacePermissionsService.findForUserAndWorkspace(initiator, testWorkspace)).thenReturn(null);

        underTest.authorizeWorkspaceManipulation(initiator, testWorkspace, Action.MANAGE, "unauthorized");
    }

    @Test
    public void testValidateAllUsersAreAlreadyInTheWorkspace() {
        User user1 = TestUtil.user(1L, "user1");
        user1.setTenant(testTenant);
        User user2 = TestUtil.user(2L, "user2");
        user2.setTenant(testTenant);
        Set<User> users = Set.of(user1, user2);

        UserWorkspacePermissions userWorkspacePermissions1 = TestUtil.userWorkspacePermissions(user1, testWorkspace, ALL_READ.value());
        when(userWorkspacePermissionsService.findForUserAndWorkspace(user1, testWorkspace)).thenReturn(userWorkspacePermissions1);
        UserWorkspacePermissions userWorkspacePermissions2 = TestUtil.userWorkspacePermissions(user2, testWorkspace, ALL_READ.value());
        when(userWorkspacePermissionsService.findForUserAndWorkspace(user2, testWorkspace)).thenReturn(userWorkspacePermissions2);

        Set<UserWorkspacePermissions> permissionsSet = underTest.validateAllUsersAreAlreadyInTheWorkspace(testWorkspace, users);

        assertEquals(2L, permissionsSet.size());
    }

    @Test(expected = BadRequestException.class)
    public void testValidateAllUsersAreAlreadyInTheWorkspaceWithUsersNotInWs() {
        User user1 = TestUtil.user(1L, "user1");
        user1.setTenant(testTenant);
        User user2 = TestUtil.user(2L, "user2");
        user2.setTenant(testTenant);
        Set<User> users = Set.of(user1, user2);

        when(userWorkspacePermissionsService.findForUserAndWorkspace(user1, testWorkspace)).thenReturn(null);
        UserWorkspacePermissions userWorkspacePermissions2 = TestUtil.userWorkspacePermissions(user2, testWorkspace, ALL_READ.value());
        when(userWorkspacePermissionsService.findForUserAndWorkspace(user2, testWorkspace)).thenReturn(userWorkspacePermissions2);

        underTest.validateAllUsersAreAlreadyInTheWorkspace(testWorkspace, users);
    }

    @Test
    public void testValidateUsersAreNotInTheWorkspaceYet() {
        User user1 = TestUtil.user(1L, "user1");
        user1.setTenant(testTenant);
        User user2 = TestUtil.user(2L, "user2");
        user2.setTenant(testTenant);
        Set<User> users = Set.of(user1, user2);

        when(userWorkspacePermissionsService.findForUserAndWorkspace(user1, testWorkspace)).thenReturn(null);
        when(userWorkspacePermissionsService.findForUserAndWorkspace(user2, testWorkspace)).thenReturn(null);

        underTest.validateUsersAreNotInTheWorkspaceYet(testWorkspace, users);
    }

    @Test(expected = BadRequestException.class)
    public void testValidateUsersAreNotInTheWorkspaceYetWithUsersInWs() {
        User user1 = TestUtil.user(1L, "user1");
        user1.setTenant(testTenant);
        User user2 = TestUtil.user(2L, "user2");
        user2.setTenant(testTenant);
        Set<User> users = Set.of(user1, user2);

        UserWorkspacePermissions userWorkspacePermissions1 = TestUtil.userWorkspacePermissions(user1, testWorkspace, ALL_READ.value());
        when(userWorkspacePermissionsService.findForUserAndWorkspace(user1, testWorkspace)).thenReturn(userWorkspacePermissions1);
        when(userWorkspacePermissionsService.findForUserAndWorkspace(user2, testWorkspace)).thenReturn(null);

        underTest.validateUsersAreNotInTheWorkspaceYet(testWorkspace, users);
    }

    @Test
    public void testValidateAllUsersAreInTheTenant() {
        User user1 = TestUtil.user(1L, "user1");
        user1.setTenant(testTenant);
        User user2 = TestUtil.user(2L, "user2");
        user2.setTenant(testTenant);
        Set<User> users = Set.of(user1, user2);

        underTest.validateAllUsersAreInTheTenant(testWorkspace, users);
    }

    @Test(expected = NotFoundException.class)
    public void testValidateAllUsersAreInTheTenantWithUsersNotInTenant() {
        User user1 = TestUtil.user(1L, "user1");
        user1.setTenant(new Tenant());
        User user2 = TestUtil.user(2L, "user2");
        user2.setTenant(testTenant);
        Set<User> users = Set.of(user1, user2);

        underTest.validateAllUsersAreInTheTenant(testWorkspace, users);
    }

    @Test
    public void testWhenTheDefultWorkspaceIsSameAsWorkspaceForDeleteThenShouldThrowBadRequestException() {
        Workspace workspaceForDelete = TestUtil.workspace(1L, "testuser@mycompany.com");
        Workspace defaultWorkspaceOfUserWhoRequestTheDeletion = TestUtil.workspace(1L, "testuser@mycompany.com");
        User userWhoRequestTheDeletion = TestUtil.user(1L, "testuser@mycompany.com");

        thrown.expectMessage("The following workspace 'testuser@mycompany.com' could not deleted because this is your default workspace.");
        thrown.expect(BadRequestException.class);

        underTest.checkThatWorkspaceIsDeletable(userWhoRequestTheDeletion, workspaceForDelete, defaultWorkspaceOfUserWhoRequestTheDeletion);
    }

    @Test
    public void testWhenTheWorkspaceHasAlreadyRunningClustersThenShouldThrowBadRequestException() {
        Workspace defaultWorkspaceOfUserWhoRequestTheDeletion = TestUtil.workspace(1L, "testuser1@mycompany.com");
        Workspace workspaceForDelete = TestUtil.workspace(2L, "bigorg");
        User userWhoRequestTheDeletion = TestUtil.user(1L, "testuser@mycompany.com");
        when(stackService.anyStackInWorkspace(anyLong())).thenReturn(Boolean.TRUE);

        thrown.expectMessage("The requested 'bigorg' workspace has already existing clusters. "
                + "Please delete them before you delete the workspace.");
        thrown.expect(BadRequestException.class);

        underTest.checkThatWorkspaceIsDeletable(userWhoRequestTheDeletion, workspaceForDelete, defaultWorkspaceOfUserWhoRequestTheDeletion);
    }

    @Test
    public void testWhenTheWorkspaceHasAlreadyRunningClustersThenShouldReturnWithoutError() {
        Workspace defaultWorkspaceOfUserWhoRequestTheDeletion = TestUtil.workspace(1L, "testuser1@mycompany.com");
        Workspace workspaceForDelete = TestUtil.workspace(2L, "bigorg");
        User userWhoRequestTheDeletion = TestUtil.user(1L, "testuser@mycompany.com");
        when(stackService.anyStackInWorkspace(anyLong())).thenReturn(Boolean.FALSE);

        underTest.checkThatWorkspaceIsDeletable(userWhoRequestTheDeletion, workspaceForDelete, defaultWorkspaceOfUserWhoRequestTheDeletion);
    }

    @Test(expected = BadRequestException.class)
    public void testVerifyRemovalFromDefaultWorkspaceWithInitiatorsDefaultWorkspace() {
        Workspace workspace = new Workspace();
        workspace.setName("initiator");
        Set<User> usersToBeRemoved = Set.of(initiator);
        underTest.verifyDefaultWorkspaceUserRemovals(initiator, workspace, usersToBeRemoved);
    }

    @Test(expected = BadRequestException.class)
    public void testVerifyRemovalFromDefaultWorkspaceWithDefaultWorkspaceOfAUser() {
        Workspace workspace = new Workspace();
        workspace.setName("user1");
        User user1 = new User();
        user1.setUserId("user1");
        Set<User> usersToBeRemoved = Set.of(user1);
        underTest.verifyDefaultWorkspaceUserRemovals(initiator, workspace, usersToBeRemoved);
    }

    @Test
    public void testVerifyRemovalFromDefaultWorkspaceWithEverythingIsFine() {
        Workspace workspace = new Workspace();
        workspace.setName("randomWorkspace");
        User user1 = new User();
        user1.setUserId("user1");
        User user2 = new User();
        user2.setUserId("user2");
        Set<User> usersToBeRemoved = Set.of(user1, user2, initiator);
        underTest.verifyDefaultWorkspaceUserRemovals(initiator, workspace, usersToBeRemoved);
    }

    @Test(expected = BadRequestException.class)
    public void testVerifyUserUpdatesWithInitiatorsDefaultWorkspace() {
        Workspace workspace = new Workspace();
        workspace.setName("initiator");
        Set<User> usersToBeRemoved = Set.of(initiator);
        underTest.verifyDefaultWorkspaceUserUpdates(initiator, workspace, usersToBeRemoved);
    }

    @Test(expected = BadRequestException.class)
    public void testVerifyUserUpdatesWithDefaultWorkspaceOfAUser() {
        Workspace workspace = new Workspace();
        workspace.setName("user1");
        User user1 = new User();
        user1.setUserId("user1");
        Set<User> usersToBeRemoved = Set.of(user1);
        underTest.verifyDefaultWorkspaceUserUpdates(initiator, workspace, usersToBeRemoved);
    }

    @Test
    public void testVerifyUserUpdatesWithEverythingIsFine() {
        Workspace workspace = new Workspace();
        workspace.setName("randomWorkspace");
        User user1 = new User();
        user1.setUserId("user1");
        User user2 = new User();
        user2.setUserId("user2");
        Set<User> usersToBeRemoved = Set.of(user1, user2, initiator);
        underTest.verifyDefaultWorkspaceUserUpdates(initiator, workspace, usersToBeRemoved);
    }
}