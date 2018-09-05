package com.sequenceiq.cloudbreak.startup;

import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.StructuredEventEntity;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.organization.User;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.organization.OrganizationService;
import com.sequenceiq.cloudbreak.structuredevent.db.StructuredEventRepository;

@Component
public class StructuredEventOrganizationMigrator {

    @Inject
    private TransactionService transactionService;

    @Inject
    private StructuredEventRepository structuredEventRepository;

    @Inject
    private OrganizationService organizationService;

    public void migrate(UserMigrationResults userMigrationResults) throws TransactionExecutionException {
        transactionService.required(() -> {
            List<StructuredEventEntity> structuredEvents = structuredEventRepository.findAllWithoutOrganizationOrUser();
            structuredEvents.stream()
                    .filter(event -> event.getOrganization() == null)
                    .forEach(event -> setOrgAndUser(userMigrationResults, event));
            return null;
        });
    }

    private void setOrgAndUser(UserMigrationResults userMigrationResults, StructuredEventEntity event) {
        String owner = event.getOwner();
        User creator = userMigrationResults.getOwnerIdToUser().get(owner);
        if (creator == null) {
            putIntoOrphanedOrg(userMigrationResults, event);
        } else {
            putIntoDefaultOrg(event, creator);
        }
        structuredEventRepository.save(event);
    }

    private void putIntoOrphanedOrg(UserMigrationResults userMigrationResults, StructuredEventEntity event) {
        Iterator<User> userIterator = userMigrationResults.getOwnerIdToUser().values().iterator();
        if (userIterator.hasNext()) {
            event.setUser(userIterator.next());
            event.setOrganization(userMigrationResults.getOrgForOrphanedResources());
        }
    }

    private void putIntoDefaultOrg(StructuredEventEntity event, User user) {
        Organization organization = organizationService.getDefaultOrganizationForUser(user);
        event.setUser(user);
        event.setOrganization(organization);
    }
}
