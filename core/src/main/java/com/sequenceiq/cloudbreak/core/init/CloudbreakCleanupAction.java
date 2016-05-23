package com.sequenceiq.cloudbreak.core.init;

import static com.sequenceiq.cloudbreak.api.model.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.model.Status.UPDATE_REQUESTED;
import static com.sequenceiq.cloudbreak.api.model.Status.WAIT_FOR_SYNC;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.InstanceStatus;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.api.model.StatusRequest;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.core.flow.FlowManager;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.FlowLogRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.cluster.event.ClusterStatusUpdateRequest;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.flowlog.FlowLogService;
import com.sequenceiq.cloudbreak.service.stack.event.StackStatusUpdateRequest;

@Component
public class CloudbreakCleanupAction {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakCleanupAction.class);

    @Inject
    private StackRepository stackRepository;

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
    private FlowManager flowManager;

    public void resetStates() {
        List<Stack> stacksToSync = resetStackStatus();
        List<Cluster> clustersToSync = resetClusterStatus(stacksToSync);
        setDeleteFailedStatus();
        terminateRunningFlows();
        triggerSyncs(stacksToSync, clustersToSync);
    }

    private List<Stack> resetStackStatus() {
        List<Stack> stacksInProgress = stackRepository.findByStatuses(Arrays.asList(UPDATE_REQUESTED, UPDATE_IN_PROGRESS, WAIT_FOR_SYNC));
        for (Stack stack : stacksInProgress) {
            if (!WAIT_FOR_SYNC.equals(stack.getStatus())) {
                loggingStatusChange("Stack", stack.getId(), stack.getStatus(), WAIT_FOR_SYNC);
                stack.setStatus(WAIT_FOR_SYNC);
                stackRepository.save(stack);
            }
            cleanInstanceMetaData(instanceMetaDataRepository.findAllInStack(stack.getId()));
        }
        return stacksInProgress;
    }

    private void cleanInstanceMetaData(Set<InstanceMetaData> metadataSet) {
        for (InstanceMetaData metadata : metadataSet) {
            if (InstanceStatus.REQUESTED.equals(metadata.getInstanceStatus()) && metadata.getInstanceId() == null) {
                LOGGER.info("InstanceMetaData [privateId: '{}'] is deleted at CB start.", metadata.getPrivateId());
                instanceMetaDataRepository.delete(metadata);
            }
        }
    }

    private List<Cluster> resetClusterStatus(List<Stack> stacksToSync) {
        List<Cluster> clustersInProgress = clusterRepository.findByStatuses(Arrays.asList(UPDATE_REQUESTED, UPDATE_IN_PROGRESS, WAIT_FOR_SYNC));
        List<Cluster> clustersToSync = new ArrayList<>();
        for (Cluster cluster : clustersInProgress) {
            loggingStatusChange("Cluster", cluster.getId(), cluster.getStatus(), WAIT_FOR_SYNC);
            cluster.setStatus(WAIT_FOR_SYNC);
            clusterRepository.save(cluster);
            if (!stackToSyncContainsCluster(stacksToSync, cluster)) {
                clustersToSync.add(cluster);
            }
        }
        return clustersToSync;
    }

    private boolean stackToSyncContainsCluster(List<Stack> stacksToSync, Cluster cluster) {
        Set<Long> stackIds = stacksToSync.stream().map(Stack::getId).collect(Collectors.toSet());
        return stackIds.contains(cluster.getStack().getId());
    }

    private void setDeleteFailedStatus() {
        List<Stack> stacksDeleteInProgress = stackRepository.findByStatuses(Collections.singletonList(Status.DELETE_IN_PROGRESS));
        for (Stack stack : stacksDeleteInProgress) {
            loggingStatusChange("Stack", stack.getId(), stack.getStatus(), Status.DELETE_FAILED);
            stack.setStatus(Status.DELETE_FAILED);
            stackRepository.save(stack);
        }
    }

    private void terminateRunningFlows() {
        List<Object[]> runningFlows = flowLogRepository.findAllNonFinalized();
        String logMessage = "Terminating flow {}";
        for (Object[] flow : runningFlows) {
            LOGGER.info(logMessage, flow[0]);
            flowLogService.terminate((Long) flow[1], (String) flow[0]);
        }
    }

    private void loggingStatusChange(String type, Long id, Status status, Status deleteFailed) {
        LOGGER.info("{} {} status is updated from {} to {} at CB start.", type, id, status, deleteFailed);
    }

    private void triggerSyncs(List<Stack> stacksToSync, List<Cluster> clustersToSync) {
        for (Stack stack : stacksToSync) {
            Platform platform = Platform.platform(stack.cloudPlatform());
            StackStatusUpdateRequest request = new StackStatusUpdateRequest(platform, stack.getId(), StatusRequest.FULL_SYNC);
            LOGGER.info("Triggering full sync on stack [name: {}, id: {}].", stack.getName(), stack.getId());
            fireEvent(stack);
            flowManager.triggerFullSync(request);
        }

        for (Cluster cluster : clustersToSync) {
            Stack stack = cluster.getStack();
            Platform platform = Platform.platform(stack.cloudPlatform());
            ClusterStatusUpdateRequest request = new ClusterStatusUpdateRequest(stack.getId(), StatusRequest.SYNC, platform);
            LOGGER.info("Triggering sync on cluster [name: {}, id: {}].", cluster.getName(), cluster.getId());
            fireEvent(stack);
            flowManager.triggerClusterSync(request);
        }
    }

    private void fireEvent(Stack stack) {
        eventService.fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(),
                "Couldn't retrieve the cluster's status, starting to sync.");
    }
}
