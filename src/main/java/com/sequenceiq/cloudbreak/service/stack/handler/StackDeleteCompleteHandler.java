package com.sequenceiq.cloudbreak.service.stack.handler;

import java.util.Calendar;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.BillingStatus;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.stack.event.StackDeleteComplete;

import reactor.event.Event;
import reactor.function.Consumer;

@Service
public class StackDeleteCompleteHandler implements Consumer<Event<StackDeleteComplete>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackDeleteCompleteHandler.class);
    private static final String DELETE_COMPLETED_MSG = "Cluster and its infrastructure were successfully terminated.";
    private static final String BILLING_STOPPED_MSG = "Billing stopped because of the termination of the cluster and its infrastructure.";
    private static final String DELIMITER = "_";

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private RetryingStackUpdater retryingStackUpdater;

    @Autowired
    private CloudbreakEventService cloudbreakEventService;

    @Override
    public void accept(Event<StackDeleteComplete> stackDeleteComplete) {
        StackDeleteComplete data = stackDeleteComplete.getData();
        Stack stack = stackRepository.findOneWithLists(data.getStackId());
        MDCBuilder.buildMdcContext(stack);
        LOGGER.info("Accepted {} event.", ReactorConfig.DELETE_COMPLETE_EVENT);
        cloudbreakEventService.fireCloudbreakEvent(data.getStackId(), Status.DELETE_COMPLETED.name(), DELETE_COMPLETED_MSG);
        cloudbreakEventService.fireCloudbreakEvent(data.getStackId(), BillingStatus.BILLING_STOPPED.name(), BILLING_STOPPED_MSG);
        updateStackFields(stack);
        retryingStackUpdater.updateStack(stack);
    }

    private void updateStackFields(Stack stack) {
        Date now = new Date();
        String terminatedName = stack.getName() + DELIMITER + now.getTime();
        Cluster cluster = stack.getCluster();
        if (cluster != null) {
            cluster.setName(terminatedName);
            cluster.setBlueprint(null);
            cluster.setRecipe(null);
        }
        stack.setCredential(null);
        stack.setName(terminatedName);
        stack.setStatus(Status.DELETE_COMPLETED);
        stack.setStatusReason(DELETE_COMPLETED_MSG);
        terminateMetaDataInstances(stack);
    }

    private void terminateMetaDataInstances(Stack stack) {
        for (InstanceMetaData metaData : stack.getRunningInstanceMetaData()) {
            long timeInMillis = Calendar.getInstance().getTimeInMillis();
            metaData.setTerminationDate(timeInMillis);
            metaData.setTerminated(true);
        }
    }
}
