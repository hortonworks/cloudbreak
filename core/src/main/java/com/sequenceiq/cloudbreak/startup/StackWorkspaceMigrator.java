package com.sequenceiq.cloudbreak.startup;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.projection.StackWorkspaceView;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

@Component
public class StackWorkspaceMigrator {

    @Inject
    private StackRepository stackRepository;

    @Inject
    private ClusterRepository clusterRepository;

    @Inject
    private StackService stackService;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private TransactionService transactionService;

    public void migrateStackWorkspaceAndCreator(UserMigrationResults userMigrationResults) throws TransactionExecutionException {
        transactionService.required(() -> {
            Map<Long, StackWorkspaceView> stacks = stackRepository.findAllAliveWithNoWorkspaceOrUser().stream()
                    .collect(Collectors.toMap(StackWorkspaceView::getId, s -> s));
            stacks.values().forEach(stack -> setWorkspaceAndCreatorForStack(userMigrationResults, stack));
            Set<Cluster> clusters = clusterRepository.findAllWithNoWorkspace();
            clusters.forEach(cluster -> setWorkspaceForCluster(stacks, cluster));
            return null;
        });
    }

    private void setWorkspaceAndCreatorForStack(UserMigrationResults userMigrationResults, StackWorkspaceView stack) {
        String owner = stack.getOwner();
        User creator = userMigrationResults.getOwnerIdToUser().get(owner);
        if (creator != null) {
            Workspace workspace = workspaceService.getDefaultWorkspaceForUser(creator);
            stackRepository.updateCreatorAndWorkspaceById(stack.getId(), creator, workspace);
        }
    }

    private void setWorkspaceForCluster(Map<Long, StackWorkspaceView> stacks, Cluster cluster) {
        Stack clusterStack = cluster.getStack();
        if (clusterStack != null) {
            Long stackId = clusterStack.getId();
            Optional<StackWorkspaceView> stack = Optional.ofNullable(stacks.getOrDefault(stackId, stackRepository.findWorkspaceViewById(stackId)));
            stack.ifPresent(s -> {
                cluster.setWorkspace(s.getWorkspace());
                clusterRepository.save(cluster);
            });
        }
    }
}
