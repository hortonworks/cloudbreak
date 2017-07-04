package com.sequenceiq.cloudbreak.init;

import static com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.model.Status.START_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.model.Status.STOP_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.model.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.model.Status.UPDATE_REQUESTED;
import static com.sequenceiq.cloudbreak.api.model.Status.WAIT_FOR_SYNC;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.model.InstanceStatus;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.core.flow2.Flow2Handler;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.FlowLog;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.FlowLogRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.flowlog.FlowLogService;
import com.sequenceiq.cloudbreak.service.usages.UsageService;

@Component
public class CloudbreakCleanupService implements ApplicationListener<ContextRefreshedEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakCleanupService.class);

    @Value("${cb.instance.uuid:}")
    private String uuid;

    @Inject
    private StackRepository stackRepository;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private ClusterRepository clusterRepository;

    @Inject
    private InstanceMetaDataRepository instanceMetaDataRepository;

    @Inject
    private CloudbreakEventService eventService;

    @Inject
    private FlowLogRepository flowLogRepository;

    @Inject
    private FlowLogService flowLogService;

    @Inject
    private ReactorFlowManager flowManager;

    @Inject
    private UsageService usageService;

    @Inject
    private Flow2Handler flow2Handler;

    public void onApplicationEvent(ContextRefreshedEvent event) {
        List<Long> stackIdsUnderOperation = restartDistruptedFlows();
        usageService.fixUsages();
//        List<Stack> stacksToSync = resetStackStatus(stackIdsUnderOperation);
//        List<Cluster> clustersToSync = resetClusterStatus(stacksToSync, stackIdsUnderOperation);
//        triggerSyncs(stacksToSync, clustersToSync);
    }

    private List<Stack> resetStackStatus(List<Long> excludeStackIds) {
        return stackRepository.findByStatuses(Arrays.asList(UPDATE_REQUESTED, UPDATE_IN_PROGRESS, WAIT_FOR_SYNC, START_IN_PROGRESS,
                STOP_IN_PROGRESS, AVAILABLE))
                .stream().filter(s -> !excludeStackIds.contains(s.getId()) || WAIT_FOR_SYNC.equals(s.getStatus()))
                .map(s -> {
                    if (!WAIT_FOR_SYNC.equals(s.getStatus())) {
                        loggingStatusChange("Stack", s.getId(), s.getStatus(), WAIT_FOR_SYNC);
                        stackUpdater.updateStackStatus(s.getId(), DetailedStackStatus.WAIT_FOR_SYNC, s.getStatusReason());
                    }
                    cleanInstanceMetaData(instanceMetaDataRepository.findAllInStack(s.getId()));
                    return s;
                }).collect(Collectors.toList());
    }

    private void cleanInstanceMetaData(Set<InstanceMetaData> metadataSet) {
        for (InstanceMetaData metadata : metadataSet) {
            if (InstanceStatus.REQUESTED.equals(metadata.getInstanceStatus()) && metadata.getInstanceId() == null) {
                LOGGER.info("InstanceMetaData [privateId: '{}'] is deleted at CB start.", metadata.getPrivateId());
                instanceMetaDataRepository.delete(metadata);
            }
        }
    }

    private List<Cluster> resetClusterStatus(List<Stack> stacksToSync, List<Long> excludeStackIds) {
        return clusterRepository.findByStatuses(Arrays.asList(UPDATE_REQUESTED, UPDATE_IN_PROGRESS, WAIT_FOR_SYNC, START_IN_PROGRESS, STOP_IN_PROGRESS))
                .stream().filter(c -> !excludeStackIds.contains(c.getStack().getId()))
                .map(c -> {
                    loggingStatusChange("Cluster", c.getId(), c.getStatus(), WAIT_FOR_SYNC);
                    c.setStatus(WAIT_FOR_SYNC);
                    clusterRepository.save(c);
                    return c;
                }).filter(c -> !stackToSyncContainsCluster(stacksToSync, c)).collect(Collectors.toList());
    }

    private boolean stackToSyncContainsCluster(List<Stack> stacksToSync, Cluster cluster) {
        Set<Long> stackIds = stacksToSync.stream().map(Stack::getId).collect(Collectors.toSet());
        return stackIds.contains(cluster.getStack().getId());
    }

    private List<Long> restartDistruptedFlows() {
        List<Long> stackIds = new ArrayList<>();
        List<Object[]> runningFlows = flowLogRepository.findAllNonFinalized();
        String logMessage = "Restarting flow {}";
        for (Object[] flow : runningFlows) {
            LOGGER.info(logMessage, flow[0]);
            try {
                String flowId = (String) flow[0];
                FlowLog flowLog = flowLogRepository.findFirstByFlowIdOrderByCreatedDesc(flowId);
                if (flowLog.getCloudbreakNodeId().equalsIgnoreCase(uuid)) {
                    flow2Handler.restartFlow(flowId);
                    stackIds.add((Long) flow[1]);
                }
            } catch (Exception e) {
                LOGGER.error(String.format("Failed to restart flow %s on stack %s", flow[0].toString(), flow[1].toString()), e);
                flowLogService.terminate((Long) flow[1], (String) flow[0]);
            }
        }
        return stackIds;
    }

    private void loggingStatusChange(String type, Long id, Status status, Status deleteFailed) {
        LOGGER.info("{} {} status is updated from {} to {} at CB start.", type, id, status, deleteFailed);
    }

    private void triggerSyncs(List<Stack> stacksToSync, List<Cluster> clustersToSync) {
        for (Stack stack : stacksToSync) {
            LOGGER.info("Triggering full sync on stack [name: {}, id: {}].", stack.getName(), stack.getId());
            fireEvent(stack);
            flowManager.triggerFullSync(stack.getId());
        }

        for (Cluster cluster : clustersToSync) {
            Stack stack = cluster.getStack();
            LOGGER.info("Triggering sync on cluster [name: {}, id: {}].", cluster.getName(), cluster.getId());
            fireEvent(stack);
            flowManager.triggerClusterSync(stack.getId());
        }
    }

    private void fireEvent(Stack stack) {
        eventService.fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(),
                "Couldn't retrieve the cluster's status, starting to sync.");
    }
}

