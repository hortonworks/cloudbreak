package com.sequenceiq.cloudbreak.service.stack.flow;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorTypeResolver;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.Clock;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterTerminationService;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

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
    private Clock clock;

    public void finalizeTermination(Long stackId, boolean force) {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        Date now = new Date();
        String terminatedName = stack.getName() + DELIMITER + now.getTime();
        Cluster cluster = stack.getCluster();
        try {
            transactionService.required(() -> {
                if (cluster != null) {
                    try {
                        boolean containerOrchestrator = orchestratorTypeResolver.resolveType(stack.getOrchestrator()).containerOrchestrator();
                        if (containerOrchestrator) {
                            clusterTerminationService.deleteClusterComponents(cluster.getId());
                        } else if (!force) {
                            throw new TerminationFailedException(String.format("There is a cluster installed on stack '%s', terminate it first!.", stackId));
                        }
                        clusterTerminationService.finalizeClusterTermination(cluster.getId());
                    } catch (CloudbreakException e) {
                        throw new RuntimeException(e.getMessage(), e);
                    } catch (TransactionExecutionException e) {
                        throw e.getCause();
                    }
                }
                stack.setCredential(null);
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