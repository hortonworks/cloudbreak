package com.sequenceiq.freeipa.flow.stack.termination.action;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.entity.InstanceGroup;
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
                terminateInstanceGroups(stack);
                terminateMetaDataInstances(stack);
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

    private void cleanupVault(Stack stack) throws DeleteException {
        kerberosMgmtV1Service.cleanupByEnvironment(stack.getEnvironmentCrn(), stack.getAccountId());
    }

}