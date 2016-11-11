package com.sequenceiq.cloudbreak.service.stack.repair;

import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.controller.FlowsAlreadyRunningException;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg;
import com.sequenceiq.cloudbreak.core.flow2.stack.repair.StackRepairNotificationRequest;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Set;
import java.util.concurrent.ExecutorService;

@Component
public class StackRepairService {
    private static final Logger LOG = LoggerFactory.getLogger(StackRepairService.class);

    @Inject
    private FlowMessageService flowMessageService;

    @Inject
    private InstanceMetaDataRepository instanceMetaDataRepository;

    @Inject
    private HostMetadataRepository hostMetadataRepository;

    @Inject
    private ReactorFlowManager reactorFlowManager;

    @Inject
    private ExecutorService executorService;

    public void add(StackRepairNotificationRequest payload) {
        if (payload.getUnhealthyInstanceIds().isEmpty()) {
            LOG.warn("No instances are unhealthy, returning...");
            flowMessageService.fireEventAndLog(payload.getStackId(), Msg.STACK_REPAIR_COMPLETE_CLEAN, Status.AVAILABLE.name());
            return;
        }
        UnhealthyInstances unhealthyInstances = groupInstancesByHostGroups(payload.getStack(), payload.getUnhealthyInstanceIds());
        StackRepairFlowSubmitter stackRepairFlowSubmitter =
                new StackRepairFlowSubmitter(payload.getStackId(), unhealthyInstances);
        flowMessageService.fireEventAndLog(payload.getStackId(), Msg.STACK_REPAIR_ATTEMPTING, Status.UPDATE_IN_PROGRESS.name());
        executorService.submit(stackRepairFlowSubmitter);
    }

    private UnhealthyInstances groupInstancesByHostGroups(Stack stack, Set<String> unhealthyInstanceIds) {
        UnhealthyInstances unhealthyInstances = new UnhealthyInstances();
        for (String instanceId : unhealthyInstanceIds) {
            InstanceMetaData instanceMetaData = instanceMetaDataRepository.findByInstanceId(stack.getId(), instanceId);
            HostMetadata hostMetadata = hostMetadataRepository.findHostInClusterByName(stack.getCluster().getId(), instanceMetaData.getDiscoveryFQDN());
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
                } catch (FlowsAlreadyRunningException fare) {
                    trials++;
                    if (trials == RETRIES) {
                        LOG.error("Could not submit because other flows are running for stack " + stackId);
                        return;
                    }
                    LOG.info("Waiting for other flows of stack " + stackId + " to complete.");
                    try {
                        Thread.sleep(SLEEP_TIME_MS);
                    } catch (InterruptedException e) {
                        LOG.error("Interrupted while waiting for other flows to finish.", e);
                        return;
                    }
                }
            }
        }
    }

}
