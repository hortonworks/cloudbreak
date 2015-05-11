package com.sequenceiq.cloudbreak.service.stack.flow;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.BillingStatus;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.InstanceStatus;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.repository.HostGroupRepository;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;

@Service
public class TerminationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TerminationService.class);
    private static final String DELETE_COMPLETED_MSG = "Cluster and its infrastructure were successfully terminated.";
    private static final String BILLING_STOPPED_MSG = "Billing stopped because of the termination of the cluster and its infrastructure.";
    private static final String DELIMITER = "_";

    @javax.annotation.Resource
    private Map<CloudPlatform, CloudPlatformConnector> cloudPlatformConnectors;

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private RetryingStackUpdater retryingStackUpdater;

    @Autowired
    private HostGroupRepository hostGroupRepository;

    @Autowired
    private CloudbreakEventService cloudbreakEventService;

    public void handleTerminationFailure(Long stackId, String errorReason) {
        LOGGER.info("Stack delete failed on stack {} and set its status to {}.", stackId, Status.DELETE_FAILED);
        retryingStackUpdater.updateStackStatus(stackId, Status.DELETE_FAILED, errorReason);
    }

    public void terminateStack(Long stackId, CloudPlatform cloudPlatform) {
        retryingStackUpdater.updateStackStatus(stackId, Status.DELETE_IN_PROGRESS, "Termination of cluster infrastructure has started.");
        final Stack stack = stackRepository.findOneWithLists(stackId);
        try {
            cloudPlatformConnectors.get(cloudPlatform).deleteStack(stack, stack.getCredential());
            finalizeTermination(stackId);
        } catch (Exception ex) {
            LOGGER.error(String.format("Stack delete failed on '%s' stack: ", stack.getId()), ex);
            String statusReason = "Termination of cluster infrastructure failed: " + ex.getMessage();
            throw new TerminationFailedException(statusReason, ex);
        }
    }

    private void finalizeTermination(Long stackId) {
        Stack stack = stackRepository.findOneWithLists(stackId);
        cloudbreakEventService.fireCloudbreakEvent(stack.getId(), Status.DELETE_COMPLETED.name(), DELETE_COMPLETED_MSG);
        cloudbreakEventService.fireCloudbreakEvent(stack.getId(), BillingStatus.BILLING_STOPPED.name(), BILLING_STOPPED_MSG);
        retryingStackUpdater.updateStack(updateStackFields(stack));
    }

    private Stack updateStackFields(Stack stack) {
        Date now = new Date();
        String terminatedName = stack.getName() + DELIMITER + now.getTime();
        Cluster cluster = stack.getCluster();
        if (cluster != null) {
            cluster.setName(terminatedName);
            cluster.setBlueprint(null);
            cluster.setStatus(Status.DELETE_COMPLETED);
            cluster.setStatusReason(DELETE_COMPLETED_MSG);
            for (HostGroup hostGroup : hostGroupRepository.findHostGroupsInCluster(cluster.getId())) {
                hostGroup.getRecipes().clear();
                hostGroupRepository.save(hostGroup);
            }
        }
        stack.setCredential(null);
        stack.setNetwork(null);
        stack.setName(terminatedName);
        stack.setStatus(Status.DELETE_COMPLETED);
        stack.setStatusReason(DELETE_COMPLETED_MSG);
        terminateMetaDataInstances(stack);
        return stack;
    }

    private void terminateMetaDataInstances(Stack stack) {
        for (InstanceMetaData metaData : stack.getRunningInstanceMetaData()) {
            long timeInMillis = Calendar.getInstance().getTimeInMillis();
            metaData.setTerminationDate(timeInMillis);
            metaData.setInstanceStatus(InstanceStatus.TERMINATED);
        }
    }
}
