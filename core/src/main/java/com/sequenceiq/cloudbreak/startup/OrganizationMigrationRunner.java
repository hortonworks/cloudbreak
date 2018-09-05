package com.sequenceiq.cloudbreak.startup;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.authorization.OrganizationResource;
import com.sequenceiq.cloudbreak.domain.organization.OrganizationAwareResource;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.service.organization.OrganizationAwareResourceService;

@Component
public class OrganizationMigrationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrganizationMigrationRunner.class);

    @Inject
    private TransactionService transactionService;

    @Inject
    private StackRepository stackRepository;

    @Inject
    private UserAndOrganizationMigrator userAndOrganizationMigrator;

    @Inject
    private StackOrganizatationMigrator stackOrganizatationMigrator;

    @Inject
    private OrganizationAwareResourceMigrator organizationAwareResourceMigrator;

    @Inject
    private StructuredEventOrganizationMigrator structuredEventOrganizationMigrator;

    @Inject
    private List<OrganizationAwareResourceService<? extends OrganizationAwareResource>> services;

    private final AtomicBoolean finished = new AtomicBoolean(false);

    public void run() {
        try {
            Long stacksWithMissingOrg = transactionService.required(() -> stackRepository.countStacksWithNoOrganizationOrCreator());
            if (stacksWithMissingOrg == 0) {
                return;
            }
            UserMigrationResults userMigrationResults = userAndOrganizationMigrator.migrateUsersAndOrgs();
            stackOrganizatationMigrator.migrateStackOrgAndCreator(userMigrationResults);
            services.stream()
                    .filter(service -> !service.resource().equals(OrganizationResource.STRUCTURED_EVENT))
                    .map(service -> (OrganizationAwareResourceService<OrganizationAwareResource>) service)
                    .forEach(service -> {
                        organizationAwareResourceMigrator.migrateResourceOrg(userMigrationResults, service::findAll,
                                service::pureSave);
                    });
            structuredEventOrganizationMigrator.migrate(userMigrationResults);

            finished.set(true);
        } catch (TransactionExecutionException e) {
            LOGGER.error("Error during db migration", e);
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public boolean isFinished() {
        return finished.get();
    }
}
