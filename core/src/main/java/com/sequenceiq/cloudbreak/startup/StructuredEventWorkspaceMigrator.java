package com.sequenceiq.cloudbreak.startup;

import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.StructuredEventEntity;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.structuredevent.db.StructuredEventRepository;

@Component
public class StructuredEventWorkspaceMigrator {

    @Inject
    private TransactionService transactionService;

    @Inject
    private StructuredEventRepository structuredEventRepository;

    @Inject
    private WorkspaceService workspaceService;

    public void migrate(UserMigrationResults userMigrationResults) throws TransactionExecutionException {
        transactionService.required(() -> {
            List<StructuredEventEntity> structuredEvents = structuredEventRepository.findAllWithoutWorkspaceOrUser();
            structuredEvents.stream()
                    .filter(event -> event.getWorkspace() == null)
                    .forEach(event -> setWorkspaceAndUser(userMigrationResults, event));
            return null;
        });
    }

    private void setWorkspaceAndUser(UserMigrationResults userMigrationResults, StructuredEventEntity event) {
        String owner = event.getOwner();
        User creator = userMigrationResults.getOwnerIdToUser().get(owner);
        if (creator == null) {
            putIntoOrphanedWorkspace(userMigrationResults, event);
        } else {
            putIntoDefaultWorkspace(event, creator);
        }
        structuredEventRepository.save(event);
    }

    private void putIntoOrphanedWorkspace(UserMigrationResults userMigrationResults, StructuredEventEntity event) {
        Iterator<User> userIterator = userMigrationResults.getOwnerIdToUser().values().iterator();
        if (userIterator.hasNext()) {
            event.setUser(userIterator.next());
            event.setWorkspace(userMigrationResults.getWorkspaceForOrphanedResources());
        }
    }

    private void putIntoDefaultWorkspace(StructuredEventEntity event, User user) {
        Workspace workspace = workspaceService.getDefaultWorkspaceForUser(user);
        event.setUser(user);
        event.setWorkspace(workspace);
    }
}
