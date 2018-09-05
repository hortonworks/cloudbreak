package com.sequenceiq.cloudbreak.startup;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.organization.User;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.organization.OrganizationService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component
public class StackOrganizatationMigrator {

    @Inject
    private StackRepository stackRepository;

    @Inject
    private ClusterRepository clusterRepository;

    @Inject
    private StackService stackService;

    @Inject
    private OrganizationService organizationService;

    @Inject
    private TransactionService transactionService;

    public void migrateStackOrgAndCreator(UserMigrationResults userMigrationResults) throws TransactionExecutionException {
        transactionService.required(() -> {
            Map<Long, Stack> stacks = stackRepository.findAllAliveWithNoOrganizationOrUser().stream()
                    .collect(Collectors.toMap(Stack::getId, s -> s));
            stacks.values().forEach(stack -> setOrgAndCreatorForStack(userMigrationResults, stack));
            Set<Cluster> clusters = clusterRepository.findAllWithNoOrganization();
            clusters.forEach(cluster -> setOrgForCluster(stacks, cluster));
            return null;
        });
    }

    private void setOrgAndCreatorForStack(UserMigrationResults userMigrationResults, Stack stack) {
        String owner = stack.getOwner();
        User creator = userMigrationResults.getOwnerIdToUser().get(owner);
        if (creator == null) {
            putIntoOrphanedOrg(userMigrationResults, stack);
        } else {
            putIntoDefaultOrg(stack, creator);
        }
        stackRepository.save(stack);
    }

    private void putIntoOrphanedOrg(UserMigrationResults userMigrationResults, Stack stack) {
        Iterator<User> userIterator = userMigrationResults.getOwnerIdToUser().values().iterator();
        if (userIterator.hasNext()) {
            stack.setCreator(userIterator.next());
            stack.setOrganization(userMigrationResults.getOrgForOrphanedResources());
        }
    }

    private void putIntoDefaultOrg(Stack stack, User creator) {
        Organization organization = organizationService.getDefaultOrganizationForUser(creator);
        stack.setCreator(creator);
        stack.setOrganization(organization);
    }

    private void setOrgForCluster(Map<Long, Stack> stacks, Cluster cluster) {
        Long stackId = cluster.getStack().getId();
        Optional<Stack> stack = Optional.ofNullable(stacks.getOrDefault(stackId, stackRepository.findById(stackId).orElse(null)));
        stack.ifPresent(s -> {
            cluster.setOrganization(s.getOrganization());
            clusterRepository.save(cluster);
        });
    }
}
