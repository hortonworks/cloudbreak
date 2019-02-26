package com.sequenceiq.cloudbreak.startup;

import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.domain.workspace.WorkspaceAwareResource;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

@Component
public class WorkspaceAwareResourceMigrator {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkspaceAwareResourceMigrator.class);

    @Inject
    private TransactionService transactionService;

    @Inject
    private WorkspaceService workspaceService;

    public void migrateResourceWorkspace(UserMigrationResults userMigrationResults, Supplier<Iterable<WorkspaceAwareResource>> findAll,
            Consumer<WorkspaceAwareResource> save) {
        try {
            transactionService.required(() -> {
                Iterable<? extends WorkspaceAwareResource> resources = findAll.get();
                resources.forEach(resource -> setWorkspace(userMigrationResults, save, resource));
                return null;
            });
        } catch (TransactionExecutionException e) {
            LOGGER.error("Error during migration.", e);
        }
    }

    private void setWorkspace(UserMigrationResults userMigrationResults, Consumer<WorkspaceAwareResource> save, WorkspaceAwareResource resource) {
        if (resource.getWorkspace() == null) {
            String owner = resource.getOwner();
            User creator = userMigrationResults.getOwnerIdToUser().get(owner);
            if (creator != null) {
                putIntoDefaultWorkspace(resource, creator);
                save.accept(resource);
            }
        }
    }

    private void putIntoDefaultWorkspace(WorkspaceAwareResource resource, User creator) {
        Workspace workspace = workspaceService.getDefaultWorkspaceForUser(creator);
        resource.setWorkspace(workspace);
    }
}
