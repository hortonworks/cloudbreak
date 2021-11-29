package com.sequenceiq.freeipa.flow.stack.termination.action;

import static java.util.function.Predicate.not;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.termination.TerminationFailedException;
import com.sequenceiq.freeipa.kerberosmgmt.exception.DeleteException;
import com.sequenceiq.freeipa.kerberosmgmt.v1.KerberosMgmtV1Service;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;
import com.sequenceiq.freeipa.service.stack.instance.InstanceGroupService;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;

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
    private TransactionService transactionService;

    @Inject
    private Clock clock;

    @Inject
    private KerberosMgmtV1Service kerberosMgmtV1Service;

    public void finalizeTermination(Long stackId) {
        long currentTimeMillis = clock.getCurrentTimeMillis();
        try {
            transactionService.required(() -> {
                Stack stack = stackService.getByIdWithListsInTransaction(stackId);
                String terminatedName = stack.getName() + DELIMITER + currentTimeMillis;
                stack.setName(terminatedName);
                stack.setTerminated(currentTimeMillis);

                // Do not remove the instance security groups or tempalates because they are needed for repairs which rebuild the instances

                terminateMetaDataInstances(stack, null);
                cleanupVault(stack);
                stackUpdater.updateStackStatus(stack, DetailedStackStatus.DELETE_COMPLETED, "Stack was terminated successfully.");
                stackService.save(stack);
                return null;
            });
        } catch (TransactionExecutionException ex) {
            LOGGER.info("Failed to terminate cluster infrastructure.");
            throw new TerminationFailedException(ex);
        }
    }

    public void finalizeTermination(Long stackId, List<String> instanceIds) {
        try {
            transactionService.required(() -> {
                Stack stack = stackService.getByIdWithListsInTransaction(stackId);
                terminateMetaDataInstances(stack, instanceIds);
            });
        } catch (TransactionExecutionException ex) {
            LOGGER.info("Failed to terminate cluster infrastructure.");
            throw new TerminationFailedException(ex);

        }
    }

    public void finalizeTerminationForInstancesWithoutInstanceIds(Long stackId) {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        terminateInstancesWithoutInstanceIds(stack);
    }

    public void requestDeletion(Long stackId, List<String> instanceIds) {
        try {
            transactionService.required(() -> {
                Stack stack = stackService.getByIdWithListsInTransaction(stackId);
                requestDeletionForInstances(stack, instanceIds);
            });
        } catch (TransactionExecutionException ex) {
            LOGGER.info("Failed to request deletion for cluster infrastructure.");
            throw new TerminationFailedException(ex);
        }
    }

    void requestDeletionForInstances(Stack stack, List<String> instanceIds) {
        List<InstanceMetaData> instanceMetaDatas = new ArrayList<>();
        stack.getAllInstanceMetaDataList().stream()
                .filter(not(InstanceMetaData::isTerminated))
                .filter(metaData -> Objects.isNull(instanceIds) || instanceIds.contains(metaData.getInstanceId()))
                .forEach(metaData -> {
                    metaData.setInstanceStatus(InstanceStatus.DELETE_REQUESTED);
                    instanceMetaDatas.add(metaData);
                });
        instanceMetaDataService.saveAll(instanceMetaDatas);
    }

    private void terminateMetaDataInstances(Stack stack, List<String> instanceIds) {
        List<InstanceMetaData> instanceMetaDatas = new ArrayList<>();
        stack.getAllInstanceMetaDataList().stream()
                .filter(not(InstanceMetaData::isTerminated))
                .filter(metaData -> Objects.isNull(instanceIds) || instanceIds.contains(metaData.getInstanceId()))
                .forEach(metaData -> {
                    metaData.setTerminationDate(clock.getCurrentTimeMillis());
                    metaData.setInstanceStatus(InstanceStatus.TERMINATED);
                    instanceMetaDatas.add(metaData);
                });
        instanceMetaDataService.saveAll(instanceMetaDatas);
    }

    private void terminateInstancesWithoutInstanceIds(Stack stack) {
        List<InstanceMetaData> instanceMetaDatas = new ArrayList<>();
        stack.getAllInstanceMetaDataList().stream()
                .filter(not(InstanceMetaData::isTerminated))
                .filter(metaData -> Objects.isNull(metaData.getInstanceId()))
                .forEach(metaData -> {
                    metaData.setTerminationDate(clock.getCurrentTimeMillis());
                    metaData.setInstanceStatus(InstanceStatus.TERMINATED);
                    instanceMetaDatas.add(metaData);
                });
        instanceMetaDataService.saveAll(instanceMetaDatas);
    }

    private void cleanupVault(Stack stack) throws DeleteException {
        kerberosMgmtV1Service.cleanupByEnvironment(stack.getEnvironmentCrn(), stack.getAccountId());
    }

}