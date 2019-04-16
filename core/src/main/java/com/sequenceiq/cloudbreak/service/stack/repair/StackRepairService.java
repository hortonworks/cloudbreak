package com.sequenceiq.cloudbreak.service.stack.repair;

import java.util.Collection;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.controller.exception.FlowsAlreadyRunningException;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.message.Msg;
import com.sequenceiq.cloudbreak.service.hostmetadata.HostMetadataService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;

@Component
public class StackRepairService {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackRepairService.class);

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private HostMetadataService hostMetadataService;

    @Inject
    private ReactorFlowManager reactorFlowManager;

    @Inject
    private ExecutorService executorService;

    public void add(Stack stack, Collection<String> unhealthyInstanceIds) {
        if (unhealthyInstanceIds.isEmpty()) {
            LOGGER.debug("No instances are unhealthy, returning...");
            flowMessageService.fireEventAndLog(stack.getId(), Msg.STACK_REPAIR_COMPLETE_CLEAN, Status.AVAILABLE.name());
            return;
        }
        UnhealthyInstances unhealthyInstances = groupInstancesByHostGroups(stack, unhealthyInstanceIds);
        Runnable stackRepairFlowSubmitter = new StackRepairFlowSubmitter(stack.getId(), unhealthyInstances);
        flowMessageService.fireEventAndLog(stack.getId(), Msg.STACK_REPAIR_ATTEMPTING, Status.UPDATE_IN_PROGRESS.name());
        executorService.submit(stackRepairFlowSubmitter);
    }

    private UnhealthyInstances groupInstancesByHostGroups(Stack stack, Iterable<String> unhealthyInstanceIds) {
        UnhealthyInstances unhealthyInstances = new UnhealthyInstances();
        for (String instanceId : unhealthyInstanceIds) {
            InstanceMetaData instanceMetaData = instanceMetaDataService.findByStackIdAndInstanceId(stack.getId(), instanceId)
                    .orElseThrow(NotFoundException.notFound("instanceMetaData", instanceId));
            HostMetadata hostMetadata = hostMetadataService.findHostInClusterByName(stack.getCluster().getId(), instanceMetaData.getDiscoveryFQDN())
                    .orElseThrow(NotFoundException.notFound("hostMetadata", instanceMetaData.getDiscoveryFQDN()));
            String hostGroupName = hostMetadata.getHostGroup().getName();
            unhealthyInstances.addInstance(instanceId, hostGroupName);
        }
        return unhealthyInstances;
    }

    class StackRepairFlowSubmitter implements Runnable {

        private static final int RETRIES = 10;

        private static final int SLEEP_TIME_MS = 1000;

        private final Long stackId;

        private final UnhealthyInstances unhealthyInstances;

        StackRepairFlowSubmitter(Long stackId, UnhealthyInstances unhealthyInstances) {
            this.stackId = stackId;
            this.unhealthyInstances = unhealthyInstances;
        }

        public Long getStackId() {
            return stackId;
        }

        public UnhealthyInstances getUnhealthyInstances() {
            return unhealthyInstances;
        }

        @Override
        public void run() {
            boolean submitted = false;
            int trials = 0;
            while (!submitted) {
                try {
                    reactorFlowManager.triggerStackRepairFlow(stackId, unhealthyInstances);
                    flowMessageService.fireEventAndLog(stackId, Msg.STACK_REPAIR_TRIGGERED, Status.UPDATE_IN_PROGRESS.name());
                    submitted = true;
                } catch (FlowsAlreadyRunningException ignored) {
                    trials++;
                    if (trials == RETRIES) {
                        LOGGER.info("Could not submit because other flows are running for stack " + stackId);
                        return;
                    }
                    LOGGER.debug("Waiting for other flows of stack " + stackId + " to complete.");
                    try {
                        Thread.sleep(SLEEP_TIME_MS);
                    } catch (InterruptedException e) {
                        LOGGER.error("Interrupted while waiting for other flows to finish.", e);
                        return;
                    }
                }
            }
        }
    }

}
