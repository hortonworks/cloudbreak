package com.sequenceiq.freeipa.flow.stack.termination.action;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.termination.TerminationFailedException;
import com.sequenceiq.freeipa.service.stack.instance.InstanceGroupService;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

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

    public void finalizeTermination(Long stackId) {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        Date now = new Date();
        String terminatedName = stack.getName() + DELIMITER + now.getTime();
        try {
            transactionService.required(() -> {
                stack.setCredential(null);
                stack.setName(terminatedName);
                stack.setTerminated(clock.getCurrentTimeMillis());
                terminateInstanceGroups(stack);
                terminateMetaDataInstances(stack);
                stackService.save(stack);
                stackUpdater.updateStackStatus(stackId, DetailedStackStatus.DELETE_COMPLETED, "Stack was terminated successfully.");
                return null;
            });
        } catch (TransactionService.TransactionExecutionException ex) {
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