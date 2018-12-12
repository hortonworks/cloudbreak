package com.sequenceiq.cloudbreak.service.workspace;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.ImmutableSet;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.domain.workspace.Tenant;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@RunWith(SpringJUnit4ClassRunner.class)
public class WorkspaceDeleteVerifierServiceTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    private com.sequenceiq.cloudbreak.service.workspace.WorkspaceModificationVerifierService underTest;

    @Mock
    private StackService stackService;

    private final User initiator = new User();

    public WorkspaceDeleteVerifierServiceTest() {
        Tenant tenant = new Tenant();
        tenant.setName("1");
        tenant.setId(1L);
        initiator.setUserId("initiator");
        initiator.setTenant(tenant);
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
        when(stackService.findAllForWorkspace(anyLong())).thenReturn(ImmutableSet.of(TestUtil.stack()));

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
        when(stackService.findAllForWorkspace(anyLong())).thenReturn(new HashSet<>());

        underTest.checkThatWorkspaceIsDeletable(userWhoRequestTheDeletion, workspaceForDelete, defaultWorkspaceOfUserWhoRequestTheDeletion);
    }

    @Test(expected = BadRequestException.class)
    public void testVerifyRemovalFromDefaultWorkspaceWithInitiatorsDefaultWorkspace() {
        Workspace workspace = new Workspace();
        workspace.setName("initiator");
        Set<User> usersToBeRemoved = Set.of(initiator);
        underTest.verifyRemovalFromDefaultWorkspace(initiator, workspace, usersToBeRemoved);
    }

    @Test(expected = BadRequestException.class)
    public void testVerifyRemovalFromDefaultWorkspaceWithDefaultWorkspaceOfAUser() {
        Workspace workspace = new Workspace();
        workspace.setName("user1");
        User user1 = new User();
        user1.setUserId("user1");
        Set<User> usersToBeRemoved = Set.of(user1);
        underTest.verifyRemovalFromDefaultWorkspace(initiator, workspace, usersToBeRemoved);
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
        underTest.verifyRemovalFromDefaultWorkspace(initiator, workspace, usersToBeRemoved);
    }

    @Test(expected = BadRequestException.class)
    public void testVerifyUserUpdatesWithInitiatorsDefaultWorkspace() {
        Workspace workspace = new Workspace();
        workspace.setName("initiator");
        Set<User> usersToBeRemoved = Set.of(initiator);
        underTest.verifyUserUpdates(initiator, workspace, usersToBeRemoved);
    }

    @Test(expected = BadRequestException.class)
    public void testVerifyUserUpdatesWithDefaultWorkspaceOfAUser() {
        Workspace workspace = new Workspace();
        workspace.setName("user1");
        User user1 = new User();
        user1.setUserId("user1");
        Set<User> usersToBeRemoved = Set.of(user1);
        underTest.verifyUserUpdates(initiator, workspace, usersToBeRemoved);
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
        underTest.verifyUserUpdates(initiator, workspace, usersToBeRemoved);
    }
}