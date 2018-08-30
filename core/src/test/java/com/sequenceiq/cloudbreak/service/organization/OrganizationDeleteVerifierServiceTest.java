package com.sequenceiq.cloudbreak.service.organization;

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
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.organization.Tenant;
import com.sequenceiq.cloudbreak.domain.organization.User;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@RunWith(SpringJUnit4ClassRunner.class)
public class OrganizationDeleteVerifierServiceTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    private OrganizationModificationVerifierService underTest;

    @Mock
    private StackService stackService;

    private final User initiator = new User();

    public OrganizationDeleteVerifierServiceTest() {
        Tenant tenant = new Tenant();
        tenant.setName("1");
        tenant.setId(1L);
        initiator.setUserId("initiator");
        initiator.setTenant(tenant);
    }

    @Test
    public void testWhenTheDefultOrganizationIsSameAsOrganizationForDeleteThenShouldThrowBadRequestException() {
        Organization organizationForDelete = TestUtil.organization(1L, "testuser@mycompany.com");
        Organization defaultOrganizationOfUserWhoRequestTheDeletion = TestUtil.organization(1L, "testuser@mycompany.com");
        User userWhoRequestTheDeletion = TestUtil.user(1L, "testuser@mycompany.com");

        thrown.expectMessage("The following organization 'testuser@mycompany.com' could not be deleted because this is your default organization.");
        thrown.expect(BadRequestException.class);

        underTest.checkThatOrganizationIsDeletable(userWhoRequestTheDeletion, organizationForDelete, defaultOrganizationOfUserWhoRequestTheDeletion);
    }

    @Test
    public void testWhenTheOrganizationHasAlreadyRunningClustersThenShouldThrowBadRequestException() {
        Organization defaultOrganizationOfUserWhoRequestTheDeletion = TestUtil.organization(1L, "testuser1@mycompany.com");
        Organization organizationForDelete = TestUtil.organization(2L, "bigorg");
        User userWhoRequestTheDeletion = TestUtil.user(1L, "testuser@mycompany.com");
        when(stackService.findAllForOrganization(anyLong())).thenReturn(ImmutableSet.of(TestUtil.stack()));

        thrown.expectMessage("The requested 'bigorg' organization has already existing clusters. "
                + "Please delete them before you delete the organization.");
        thrown.expect(BadRequestException.class);

        underTest.checkThatOrganizationIsDeletable(userWhoRequestTheDeletion, organizationForDelete, defaultOrganizationOfUserWhoRequestTheDeletion);
    }

    @Test
    public void testWhenTheOrganizationHasAlreadyRunningClustersThenShouldReturnWithoutError() {
        Organization defaultOrganizationOfUserWhoRequestTheDeletion = TestUtil.organization(1L, "testuser1@mycompany.com");
        Organization organizationForDelete = TestUtil.organization(2L, "bigorg");
        User userWhoRequestTheDeletion = TestUtil.user(1L, "testuser@mycompany.com");
        when(stackService.findAllForOrganization(anyLong())).thenReturn(new HashSet<>());

        underTest.checkThatOrganizationIsDeletable(userWhoRequestTheDeletion, organizationForDelete, defaultOrganizationOfUserWhoRequestTheDeletion);
    }

    @Test(expected = BadRequestException.class)
    public void testVerifyRemovalFromDefaultOrgWithInitiatorsDefaultOrg() {
        Organization organization = new Organization();
        organization.setName("initiator");
        Set<User> usersToBeRemoved = Set.of(initiator);
        underTest.verifyRemovalFromDefaultOrg(initiator, organization, usersToBeRemoved);
    }

    @Test(expected = BadRequestException.class)
    public void testVerifyRemovalFromDefaultOrgWithDefaultOrgOfAUser() {
        Organization organization = new Organization();
        organization.setName("user1");
        User user1 = new User();
        user1.setUserId("user1");
        Set<User> usersToBeRemoved = Set.of(user1);
        underTest.verifyRemovalFromDefaultOrg(initiator, organization, usersToBeRemoved);
    }

    @Test
    public void testVerifyRemovalFromDefaultOrgWithEverythingIsFine() {
        Organization organization = new Organization();
        organization.setName("randomOrg");
        User user1 = new User();
        user1.setUserId("user1");
        User user2 = new User();
        user2.setUserId("user2");
        Set<User> usersToBeRemoved = Set.of(user1, user2, initiator);
        underTest.verifyRemovalFromDefaultOrg(initiator, organization, usersToBeRemoved);
    }

    @Test(expected = BadRequestException.class)
    public void testVerifyUserUpdatesWithInitiatorsDefaultOrg() {
        Organization organization = new Organization();
        organization.setName("initiator");
        Set<User> usersToBeRemoved = Set.of(initiator);
        underTest.verifyUserUpdates(initiator, organization, usersToBeRemoved);
    }

    @Test(expected = BadRequestException.class)
    public void testVerifyUserUpdatesWithDefaultOrgOfAUser() {
        Organization organization = new Organization();
        organization.setName("user1");
        User user1 = new User();
        user1.setUserId("user1");
        Set<User> usersToBeRemoved = Set.of(user1);
        underTest.verifyUserUpdates(initiator, organization, usersToBeRemoved);
    }

    @Test
    public void testVerifyUserUpdatesWithEverythingIsFine() {
        Organization organization = new Organization();
        organization.setName("randomOrg");
        User user1 = new User();
        user1.setUserId("user1");
        User user2 = new User();
        user2.setUserId("user2");
        Set<User> usersToBeRemoved = Set.of(user1, user2, initiator);
        underTest.verifyUserUpdates(initiator, organization, usersToBeRemoved);
    }
}