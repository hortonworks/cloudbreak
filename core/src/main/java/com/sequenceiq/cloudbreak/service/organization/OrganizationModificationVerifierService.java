package com.sequenceiq.cloudbreak.service.organization;

import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.organization.User;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Service
public class OrganizationModificationVerifierService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrganizationModificationVerifierService.class);

    @Inject
    private StackService stackService;

    public void verifyUserUpdates(User initiator, Organization organization, Set<User> usersToBeUpdated) {
        verifyOperationRegardingDefaultOrgs(initiator, organization, usersToBeUpdated,
                "You cannot change your permissions in your default organization.",
                "You cannot modify the permission of %s in their default organization.");
    }

    public void verifyRemovalFromDefaultOrg(User initiator, Organization organization, Set<User> usersToBeRemoved) {
        verifyOperationRegardingDefaultOrgs(initiator, organization, usersToBeRemoved,
                "You cannot remove yourself from your default organization.",
                "You cannot remove %s from their default organization.");
    }

    private void verifyOperationRegardingDefaultOrgs(User initiator, Organization organization, Set<User> usersToBeModified,
            String initiatorErrorMessage, String otherUserErrorMessage) {

        if (usersToBeModified.contains(initiator) && isDefaultOrgForUser(organization, initiator)) {
            throw new BadRequestException(initiatorErrorMessage);
        }

        Optional<User> usersDefaultOrg = usersToBeModified.stream()
                .filter(user -> isDefaultOrgForUser(organization, user))
                .findFirst();

        if (usersDefaultOrg.isPresent()) {
            throw new BadRequestException(String.format(otherUserErrorMessage, usersDefaultOrg.get().getUserId()));
        }
    }

    public boolean isDefaultOrgForUser(Organization organization, User user) {
        return organization.getName().equals(user.getUserId());
    }

    public void checkThatOrganizationIsDeletable(User userWhoRequestTheDeletion, Organization organizationForDelete,
            Organization defaultOrganizationOfUserWhoRequestTheDeletion) {

        if (defaultOrganizationOfUserWhoRequestTheDeletion.equals(organizationForDelete)) {
            LOGGER.info("The requested {} organization for delete is the same as the default organization of the user {}.",
                    organizationForDelete.getName(), userWhoRequestTheDeletion.getUserName());
            throw new BadRequestException(String.format("The following organization '%s' could not be deleted because this is your default organization.",
                    organizationForDelete.getName()));
        }
        if (!stackService.findAllForOrganization(organizationForDelete.getId()).isEmpty()) {
            LOGGER.info("The requested {} organization has already existing clusters. We can not delete them until those will be deleted",
                    organizationForDelete.getName());
            throw new BadRequestException(String.format("The requested '%s' organization has already existing clusters. "
                    + "Please delete them before you delete the organization.", organizationForDelete.getName()));
        }
    }
}
