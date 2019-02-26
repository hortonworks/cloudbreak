package com.sequenceiq.cloudbreak.startup;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.domain.workspace.WorkspaceAwareResource;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceAwareResourceService;

@Component
public class WorkspaceMigrationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkspaceMigrationRunner.class);

    @Inject
    private TransactionService transactionService;

    @Inject
    private StackRepository stackRepository;

    @Inject
    private UserAndWorkspaceMigrator userAndWorkspaceMigrator;

    @Inject
    private StackWorkspaceMigrator stackWorkspaceMigrator;

    @Inject
    private WorkspaceAwareResourceMigrator workspaceAwareResourceMigrator;

    @Inject
    private StructuredEventWorkspaceMigrator structuredEventWorkspaceMigrator;

    @Inject
    private List<WorkspaceAwareResourceService<? extends WorkspaceAwareResource>> services;

    private final AtomicBoolean finished = new AtomicBoolean(false);

    public void run() {
        try {
            Long stacksWithMissingWorkspace = transactionService.required(() -> stackRepository.countStacksWithNoWorkspaceOrCreator());
            if (stacksWithMissingWorkspace > 0) {
                UserMigrationResults userMigrationResults = userAndWorkspaceMigrator.migrateUsersAndWorkspaces();
                stackWorkspaceMigrator.migrateStackWorkspaceAndCreator(userMigrationResults);
                services.stream()
                        .filter(service -> !service.resource().equals(WorkspaceResource.STRUCTURED_EVENT))
                        .map(service -> (WorkspaceAwareResourceService<WorkspaceAwareResource>) service)
                        .forEach(service -> workspaceAwareResourceMigrator.migrateResourceWorkspace(userMigrationResults, service::findAll,
                                service::pureSave));
                structuredEventWorkspaceMigrator.migrate(userMigrationResults);
            }
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
