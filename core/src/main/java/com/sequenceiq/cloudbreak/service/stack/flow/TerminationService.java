package com.sequenceiq.cloudbreak.service.stack.flow;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.authorization.service.OwnerAssignmentService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterTerminationService;
import com.sequenceiq.cloudbreak.service.freeipa.FreeIpaCleanupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackEncryptionService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stackstatus.StackStatusService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@Service
public class TerminationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TerminationService.class);

    private static final String DELIMITER = "_";

    @Inject
    private StackService stackService;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private InstanceGroupService instanceGroupService;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private ClusterTerminationService clusterTerminationService;

    @Inject
    private TransactionService transactionService;

    @Inject
    private FreeIpaCleanupService freeIpaCleanupService;

    @Inject
    private Clock clock;

    @Inject
    private OwnerAssignmentService ownerAssignmentService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private FinalizationCleanUpService cleanUpService;

    @Inject
    private StackRotationService stackRotationService;

    @Inject
    private StackEncryptionService stackEncryptionService;

    @Inject
    private StackStatusService stackStatusService;

    public void finalizeTermination(Long stackId, boolean force) {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        Date now = new Date();
        cleanupFreeIpa(stack);
        String terminatedName = stack.getName() + DELIMITER + now.getTime();
        Cluster cluster = stack.getCluster();
        try {
            LOGGER.info("Starting to update the stack and cluster to delete completed");
            transactionService.required(() -> {
                if (cluster != null) {
                    try {
                        clusterService.cleanupCluster(stack);
                        clusterTerminationService.finalizeClusterTermination(cluster.getId(), force);
                    } catch (TransactionExecutionException e) {
                        throw e.getCause();
                    }
                }
                terminateInstanceGroups(stack);
                terminateMetaDataInstances(stack);
                deleteStackEncryption(stack);
                updateToDeleteCompleted(stack.getId(), terminatedName, "Stack was terminated successfully.");
                return null;
            });
        } catch (TransactionExecutionException ex) {
            LOGGER.info("Failed to terminate cluster infrastructure. Stack id {}", stack.getId());
            deleteOnlyIfForced(stack, force, terminatedName);
            throw new TerminationFailedException(ex);
        }
        cleanUpUnnecessaryDatabaseEntries(stackId, stack.getResourceCrn());
    }

    private void cleanUpUnnecessaryDatabaseEntries(Long stackId, String resourceCrn) {
        LOGGER.debug("About to clean up leftover DB entries.");
        try {
            cleanUpService.cleanUpStructuredEventsForStack(stackId);
            cleanUpService.detachClusterComponentRelatedAuditEntries(stackId);
        } catch (Exception e) {
            LOGGER.warn("Unable to clean up resources due to: " + e.getMessage(), e);
        }
    }

    public void finalizeRecoveryTeardown(Long stackId) {
        StackDto stackDto = stackDtoService.getById(stackId);
        cleanupFreeIpa(stackDto);
        deleteMetaDataInstances(stackDto);
    }

    private void cleanupFreeIpa(StackDtoDelegate stackDto) {
        try {
            LOGGER.info("Cleaning up the related FreeIpa");
            freeIpaCleanupService.cleanupButIp(stackDto);
        } catch (Exception e) {
            LOGGER.warn("FreeIPA cleanup has failed during termination finalization, ignoring error", e);
        }
    }

    private void deleteOnlyIfForced(Stack stack, boolean force, String terminatedName) {
        if (force) {
            updateToDeleteCompleted(stack.getId(), terminatedName, "Finalization of stack termination failed, stack marked as deleted based on force flag.");
        } else {
            stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.DELETE_FAILED, "Finalization of stack termination failed.");
        }
    }

    private void updateToDeleteCompleted(Long stackId, String terminatedName, String statusReason) {
        try {
            LOGGER.info("Updating the stack to delete completed status");
            Stack stack = transactionService.required(() -> {
                Stack updatedStack = stackService.get(stackId);
                updatedStack.setName(terminatedName);
                updatedStack.setTerminated(clock.getCurrentTimeMillis());
                updatedStack = stackService.save(updatedStack);
                stackUpdater.updateStackStatus(updatedStack.getId(), DetailedStackStatus.DELETE_COMPLETED, statusReason);
                stackStatusService.cleanupByPreservedStatus(updatedStack.getId(), Status.DELETE_COMPLETED);
                return updatedStack;
            });
            if (stack.getType().equals(StackType.WORKLOAD)) {
                ownerAssignmentService.notifyResourceDeleted(stack.getResourceCrn());
                stackRotationService.cleanupSecretRotationEntries(stack.getResourceCrn());
            }
        } catch (TransactionService.TransactionExecutionException e) {
            throw e.getCause();
        }
    }

    private void terminateInstanceGroups(Stack stack) {
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            instanceGroup.setSecurityGroup(null);
            instanceGroup.setTemplate(null);
            instanceGroup.setInstanceGroupNetwork(null);
            instanceGroupService.save(instanceGroup);
        }
    }

    private void terminateMetaDataInstances(Stack stack) {
        List<InstanceMetaData> instanceMetaDatas = new ArrayList<>();
        Set<InstanceMetaData> notDeletedInstanceMetadataSet = instanceMetaDataService.getNotDeletedInstanceMetadataByStackId(stack.getId());
        for (InstanceMetaData metaData : notDeletedInstanceMetadataSet) {
            metaData.setTerminationDate(clock.getCurrentTimeMillis());
            metaData.setInstanceStatus(InstanceStatus.TERMINATED);
            instanceMetaDatas.add(metaData);
        }
        instanceMetaDataService.saveAll(instanceMetaDatas);
    }

    private void deleteMetaDataInstances(StackDto stackDto) {
        List<Long> metaData = stackDto.getNotTerminatedInstanceMetaData().stream().map(InstanceMetadataView::getId).collect(Collectors.toList());
        LOGGER.debug("Deleting instance metadata entry {}", metaData);
        instanceMetaDataService.deleteAllByInstanceIds(metaData);
    }

    private void deleteStackEncryption(Stack stack) {
        LOGGER.info("Deleting Stack Encryption for  Stack {}", stack.getId());
        stackEncryptionService.deleteStackEncryption(stack.getId());
    }

}
