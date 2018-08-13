package com.sequenceiq.cloudbreak.service.organization;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.organization.User;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Service
public class OrganizationDeleteVerifierService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrganizationDeleteVerifierService.class);

    @Inject
    private StackService stackService;

    public void checkThatOrganizationIsDeletable(User userWhoRequestTheDeletion, Organization organizationForDelete,
            Organization defaultOrganizationOfUserWhoRequestTheDeletion) {
        if (defaultOrganizationOfUserWhoRequestTheDeletion.equals(organizationForDelete)) {
            LOGGER.info("The requested {} organization for delete is the same as the default organization of the user {}.",
                    organizationForDelete.getName(), userWhoRequestTheDeletion.getUserName());
            throw new BadRequestException(String.format("The following organization '%s' could not deleted because this is your default organization.",
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
