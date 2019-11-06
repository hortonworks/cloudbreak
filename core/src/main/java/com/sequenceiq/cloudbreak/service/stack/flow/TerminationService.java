package com.sequenceiq.cloudbreak.service.stack.flow;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorTypeResolver;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterTerminationService;
import com.sequenceiq.cloudbreak.service.datalake.DatalakeResourcesService;
import com.sequenceiq.cloudbreak.service.freeipa.FreeIpaCleanupService;
import com.sequenceiq.cloudbreak.service.freeipa.FreeIpaOperationFailedException;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;

@Service
public class TerminationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TerminationService.class);

    private static final String DELIMITER = "_";

    @Inject
    private StackService stackService;

    @Inject
    private InstanceGroupService instanceGroupService;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private ClusterTerminationService clusterTerminationService;

    @Inject
    private OrchestratorTypeResolver orchestratorTypeResolver;

    @Inject
    private TransactionService transactionService;

    @Inject
    private DatalakeResourcesService datalakeResourcesService;

    @Inject
    @Qualifier("cloudbreakListeningScheduledExecutorService")
    private ExecutorService executorService;

    @Inject
    private FreeIpaV1Endpoint freeIpaV1Endpoint;

    @Inject
    private ThreadBasedUserCrnProvider threadBasedUserCrnProvider;

    @Inject
    private Clock clock;

    @Inject
    private FreeIpaCleanupService freeIpaCleanupService;

    public void finalizeTermination(Long stackId, boolean force) {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        Date now = new Date();
        cleanupFreeIpa(force, stack);
        String terminatedName = stack.getName() + DELIMITER + now.getTime();
        if (stack.getType() == StackType.DATALAKE) {
            datalakeResourcesService.deleteWithDependenciesByStackId(stack.getId());
        }
        Cluster cluster = stack.getCluster();
        try {
            transactionService.required(() -> {
                if (cluster != null) {
                    try {
                        clusterTerminationService.finalizeClusterTermination(cluster.getId());
                    } catch (TransactionExecutionException e) {
                        throw e.getCause();
                    }
                }
                stack.setName(terminatedName);
                stack.setTerminated(clock.getCurrentTimeMillis());
                terminateInstanceGroups(stack);
                terminateMetaDataInstances(stack);
                stackService.save(stack);
                stackUpdater.updateStackStatus(stackId, DetailedStackStatus.DELETE_COMPLETED, "Stack was terminated successfully.");
                return null;
            });
        } catch (TransactionExecutionException ex) {
            LOGGER.info("Failed to terminate cluster infrastructure. Stack id {}", stack.getId());
            throw new TerminationFailedException(ex);
        }
    }

    private void cleanupFreeIpa(Boolean forcedTermination, Stack stack) {
        try {
            freeIpaCleanupService.cleanup(stack, false, null);
        } catch (FreeIpaOperationFailedException e) {
            LOGGER.error("Failed to cleanup", e);
            if (!forcedTermination) {
                throw e;
            }
        }
    }

    private void terminateInstanceGroups(Stack stack) {
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            instanceGroup.setSecurityGroup(null);
            instanceGroup.setTemplate(null);
            instanceGroupService.save(instanceGroup);
        }
    }

    private void terminateMetaDataInstances(Stack stack) {
        List<InstanceMetaData> instanceMetaDatas = new ArrayList<>();
        for (InstanceMetaData metaData : stack.getNotDeletedInstanceMetaDataSet()) {
            metaData.setTerminationDate(clock.getCurrentTimeMillis());
            metaData.setInstanceStatus(InstanceStatus.TERMINATED);
            instanceMetaDatas.add(metaData);
        }
        instanceMetaDataService.saveAll(instanceMetaDatas);
    }

}
