package com.sequenceiq.cloudbreak.startup;

import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.organization.OrganizationAwareResource;
import com.sequenceiq.cloudbreak.domain.organization.User;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.organization.OrganizationService;

@Component
public class OrganizationAwareResourceMigrator {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrganizationAwareResourceMigrator.class);

    @Inject
    private TransactionService transactionService;

    @Inject
    private OrganizationService organizationService;

    public void migrateResourceOrg(UserMigrationResults userMigrationResults, Supplier<Iterable<OrganizationAwareResource>> findAll,
            Consumer<OrganizationAwareResource> save) {
        try {
            transactionService.required(() -> {
                Iterable<? extends OrganizationAwareResource> resources = findAll.get();
                resources.forEach(resource -> {
                    setOrganization(userMigrationResults, save, resource);
                });
                return null;
            });
        } catch (TransactionExecutionException e) {
            LOGGER.error("Error during migration.", e);
        }
    }

    private void setOrganization(UserMigrationResults userMigrationResults, Consumer<OrganizationAwareResource> save, OrganizationAwareResource resource) {
        if (resource.getOrganization() == null) {
            String owner = resource.getOwner();
            User creator = userMigrationResults.getOwnerIdToUser().get(owner);
            if (creator == null) {
                putIntoOrphanedOrg(userMigrationResults, resource);
            } else {
                putIntoDefaultOrg(resource, creator);
            }
            save.accept(resource);
        }
    }

    private void putIntoOrphanedOrg(UserMigrationResults userMigrationResults, OrganizationAwareResource resource) {
        Iterator<User> userIterator = userMigrationResults.getOwnerIdToUser().values().iterator();
        if (userIterator.hasNext()) {
            resource.setOrganization(userMigrationResults.getOrgForOrphanedResources());
        }
    }

    private void putIntoDefaultOrg(OrganizationAwareResource resource, User creator) {
        Organization organization = organizationService.getDefaultOrganizationForUser(creator);
        resource.setOrganization(organization);
    }
}
