package com.sequenceiq.cloudbreak.service.stack.handler;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ecwid.consul.v1.ConsulClient;
import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.controller.json.HostGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.domain.BillingStatus;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.InstanceStatus;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.cluster.AmbariClusterService;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.stack.event.StackUpdateSuccess;
import com.sequenceiq.cloudbreak.service.stack.flow.ConsulAgentLeaveCheckerTask;
import com.sequenceiq.cloudbreak.service.stack.flow.ConsulContext;
import com.sequenceiq.cloudbreak.service.stack.flow.ConsulUtils;

import reactor.event.Event;
import reactor.function.Consumer;

@Component
public class StackUpdateSuccessHandler implements Consumer<Event<StackUpdateSuccess>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackUpdateSuccessHandler.class);

    private static final int POLLING_INTERVAL = 5000;
    private static final int MAX_POLLING_ATTEMPTS = 100;

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private RetryingStackUpdater stackUpdater;

    @Autowired
    private CloudbreakEventService eventService;

    @Autowired
    private PollingService<ConsulContext> consulPollingService;

    @Autowired
    private ConsulAgentLeaveCheckerTask consulAgentLeaveCheckerTask;

    @Autowired
    private AmbariClusterService ambariClusterService;

    @Override
    public void accept(Event<StackUpdateSuccess> t) {
        StackUpdateSuccess updateSuccess = t.getData();
        Long stackId = updateSuccess.getStackId();
        Stack stack = stackRepository.findOneWithLists(stackId);
        MDCBuilder.buildMdcContext(stack);
        LOGGER.info("Accepted {} event.", ReactorConfig.STACK_UPDATE_SUCCESS_EVENT);
        Set<String> instanceIds = updateSuccess.getInstanceIds();
        InstanceGroup instanceGroup = stack.getInstanceGroupByInstanceGroupName(updateSuccess.getInstanceGroup());
        if (updateSuccess.isRemoveInstances()) {
            terminate(stack, stackId, instanceIds, instanceGroup);
        } else {
            int nodeCount = instanceGroup.getNodeCount() + instanceIds.size();
            stackUpdater.updateNodeCount(stackId, nodeCount, updateSuccess.getInstanceGroup());
            eventService.fireCloudbreakEvent(stackId, BillingStatus.BILLING_CHANGED.name(), "Billing changed due to upscaling of cluster infrastructure.");
        }
        stackUpdater.updateMetadataReady(stackId, true);
        String statusCause = String.format("%sscaling of cluster infrastructure was successful.", updateSuccess.isRemoveInstances() ? "Down" : "Up");
        stackUpdater.updateStackStatus(stackId, Status.AVAILABLE, statusCause);
        if (updateSuccess.isWithClusterEvent() && !updateSuccess.isRemoveInstances()) {
            HostGroupAdjustmentJson hostGroupAdjustmentJson = new HostGroupAdjustmentJson();
            hostGroupAdjustmentJson.setHostGroup(updateSuccess.getInstanceGroup());
            hostGroupAdjustmentJson.setScalingAdjustment(updateSuccess.getInstanceIds().size());
            hostGroupAdjustmentJson.setWithStackUpdate(false);
            ambariClusterService.updateHosts(updateSuccess.getStackId(), hostGroupAdjustmentJson, updateSuccess.isWithClusterEvent());
        }

    }

    private void terminate(Stack stack, long stackId,
            Set<String> instanceIds, InstanceGroup instanceGroup) {
        MDCBuilder.buildMdcContext(stack);
        int nodeCount = instanceGroup.getNodeCount() - instanceIds.size();
        stackUpdater.updateNodeCount(stackId, nodeCount, instanceGroup.getGroupName());

        List<ConsulClient> clients = createConsulClients(stack, instanceGroup.getGroupName());
        for (InstanceMetaData instanceMetaData : instanceGroup.getInstanceMetaData()) {
            if (instanceIds.contains(instanceMetaData.getInstanceId())) {
                long timeInMillis = Calendar.getInstance().getTimeInMillis();
                instanceMetaData.setTerminationDate(timeInMillis);
                instanceMetaData.setInstanceStatus(InstanceStatus.TERMINATED);
                removeAgentFromConsul(stack, clients, instanceMetaData);
            }
        }

        stackUpdater.updateStackMetaData(stackId, instanceGroup.getAllInstanceMetaData(), instanceGroup.getGroupName());
        LOGGER.info("Successfully terminated metadata of instances '{}' in stack.", instanceIds);
        eventService.fireCloudbreakEvent(stackId, BillingStatus.BILLING_CHANGED.name(),
                "Billing changed due to downscaling of cluster infrastructure.");
    }

    private void removeAgentFromConsul(Stack stack, List<ConsulClient> clients, InstanceMetaData metaData) {
        String nodeName = metaData.getLongName().replace(ConsulUtils.CONSUL_DOMAIN, "");
        consulPollingService.pollWithTimeout(
                consulAgentLeaveCheckerTask,
                new ConsulContext(stack, clients, Collections.singletonList(nodeName)),
                POLLING_INTERVAL,
                MAX_POLLING_ATTEMPTS);
    }

    private List<ConsulClient> createConsulClients(Stack stack, String instanceGroupName) {
        List<InstanceGroup> instanceGroups = stack.getInstanceGroupsAsList();
        List<ConsulClient> clients = Collections.emptyList();
        for (InstanceGroup instanceGroup : instanceGroups) {
            if (!instanceGroup.getGroupName().equalsIgnoreCase(instanceGroupName)) {
                clients = ConsulUtils.createClients(instanceGroup.getInstanceMetaData());
            }
        }
        return clients;
    }

}
