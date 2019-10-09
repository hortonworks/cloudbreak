package com.sequenceiq.cloudbreak.service.workspace;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
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

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.workspace.authorization.UmsWorkspaceAuthorizationService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.authorization.resource.ResourceAction;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@RunWith(SpringJUnit4ClassRunner.class)
public class WorkspaceModificationVerifierServiceTest {

    private static final String WORKSPACE_NAME = "test-workspace";

    private static final String TENANT_NAME = "test-tenant";

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private StackService stackService;

    @Mock
    private UmsWorkspaceAuthorizationService umsWorkspaceAuthorizationService;

    @Mock
    private UserService userService;

    @InjectMocks
    private WorkspaceModificationVerifierService underTest;

    private final User initiator = TestUtil.user(1L, "initiator");

    private final Tenant testTenant = new Tenant();

    private final Workspace testWorkspace = TestUtil.workspace(1L, WORKSPACE_NAME);

    @Before
    public void setup() {
        initiator.setTenant(testTenant);
        initiator.setUserName("initiator");
        testTenant.setId(1L);
        testTenant.setName(TENANT_NAME);
        testWorkspace.setTenant(testTenant);
    }

    @Test
    public void testAuthorizeWorkspaceManipulationWithAccess() {
        when(umsWorkspaceAuthorizationService.getUserIdsOfWorkspace(any(), any())).thenReturn(Sets.newHashSet("1"));
        when(userService.getByUsersIds(anySet())).thenReturn(Sets.newHashSet(initiator));
        doNothing().when(umsWorkspaceAuthorizationService).checkRightOfUserForResource(any(), any(), any(), any());

        underTest.authorizeWorkspaceManipulation(initiator, testWorkspace, ResourceAction.MANAGE, "unauthorized");
    }

    @Test(expected = AccessDeniedException.class)
    public void testAuthorizeWorkspaceManipulationWithNoManagePermission() {
        when(umsWorkspaceAuthorizationService.getUserIdsOfWorkspace(any(), any())).thenReturn(Sets.newHashSet("1"));
        when(userService.getByUsersIds(anySet())).thenReturn(Sets.newHashSet(initiator));
        doThrow(AccessDeniedException.class).when(umsWorkspaceAuthorizationService).checkRightOfUserForResource(any(), any(), any(), anyString());

        underTest.authorizeWorkspaceManipulation(initiator, testWorkspace, ResourceAction.MANAGE, "unauthorized");
    }

    @Test(expected = AccessDeniedException.class)
    public void testAuthorizeWorkspaceManipulationWhenUserIsNotPartOfWs() {
        when(umsWorkspaceAuthorizationService.getUserIdsOfWorkspace(any(), any())).thenReturn(Sets.newHashSet());
        doNothing().when(umsWorkspaceAuthorizationService).checkRightOfUserForResource(any(), any(), any(), any());

        underTest.authorizeWorkspaceManipulation(initiator, testWorkspace, ResourceAction.MANAGE, "unauthorized");
    }

    @Test
    public void testValidateAllUsersAreAlreadyInTheWorkspace() {
        User user1 = TestUtil.user(1L, "user1");
        user1.setTenant(testTenant);
        User user2 = TestUtil.user(2L, "user2");
        user2.setTenant(testTenant);
        Set<User> users = Set.of(user1, user2);
        when(umsWorkspaceAuthorizationService.getUserIdsOfWorkspace(any(), any())).thenReturn(Sets.newHashSet("1", "2"));
        when(userService.getByUsersIds(anySet())).thenReturn(users);

        underTest.validateAllUsersAreAlreadyInTheWorkspace(initiator, testWorkspace, users);
    }

    @Test(expected = BadRequestException.class)
    public void testValidateAllUsersAreAlreadyInTheWorkspaceWithUsersNotInWs() {
        User user1 = TestUtil.user(1L, "user1");
        user1.setTenant(testTenant);
        User user2 = TestUtil.user(2L, "user2");
        user2.setTenant(testTenant);
        Set<User> users = Set.of(user1, user2);
        when(umsWorkspaceAuthorizationService.getUserIdsOfWorkspace(any(), any())).thenReturn(Sets.newHashSet("1"));
        when(userService.getByUsersIds(anySet())).thenReturn(Sets.newHashSet(user1));

        underTest.validateAllUsersAreAlreadyInTheWorkspace(initiator, testWorkspace, users);
    }

    @Test
    public void testValidateUsersAreNotInTheWorkspaceYet() {
        User user1 = TestUtil.user(1L, "user1");
        user1.setTenant(testTenant);
        User user2 = TestUtil.user(2L, "user2");
        user2.setTenant(testTenant);
        Set<User> users = Set.of(user1, user2);
        when(umsWorkspaceAuthorizationService.getUserIdsOfWorkspace(any(), any())).thenReturn(Sets.newHashSet());

        underTest.validateUsersAreNotInTheWorkspaceYet(initiator, testWorkspace, users);
    }

    @Test(expected = BadRequestException.class)
    public void testValidateUsersAreNotInTheWorkspaceYetWithUsersInWs() {
        User user1 = TestUtil.user(1L, "user1");
        user1.setTenant(testTenant);
        User user2 = TestUtil.user(2L, "user2");
        user2.setTenant(testTenant);
        Set<User> users = Set.of(user1, user2);
        when(umsWorkspaceAuthorizationService.getUserIdsOfWorkspace(any(), any())).thenReturn(Sets.newHashSet("1", "2"));
        when(userService.getByUsersIds(anySet())).thenReturn(users);

        underTest.validateUsersAreNotInTheWorkspaceYet(initiator, testWorkspace, users);
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
        user1.setUserName("user1");
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
        user1.setUserName("user1");
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