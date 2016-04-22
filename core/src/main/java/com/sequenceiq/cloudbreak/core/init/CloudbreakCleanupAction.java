package com.sequenceiq.cloudbreak.core.init;

import static com.sequenceiq.cloudbreak.api.model.Status.WAIT_FOR_SYNC;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.InstanceStatus;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.FlowLogRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.flowlog.FlowLogService;

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

    @PostConstruct
    public void resetInProgressStates() {
        resetStackStatus();
        resetCloudStatus();
        setDeleteFailedStatus();
        terminateRunningFlows();
    }

    private void resetCloudStatus() {
        List<Cluster> clustersInProgress = clusterRepository.findByStatuses(Arrays.asList(Status.UPDATE_REQUESTED, Status.UPDATE_IN_PROGRESS));
        for (Cluster cluster : clustersInProgress) {
            loggingStatusChange("Cluster", cluster.getId(), cluster.getStatus(), Status.WAIT_FOR_SYNC);
            cluster.setStatus(Status.WAIT_FOR_SYNC);
            clusterRepository.save(cluster);
            fireEvent(cluster.getStack());
        }
    }

    private void resetStackStatus() {
        List<Stack> stacksInProgress = stackRepository.findByStatuses(Arrays.asList(Status.UPDATE_REQUESTED, Status.UPDATE_IN_PROGRESS));
        for (Stack stack : stacksInProgress) {
            loggingStatusChange("Stack", stack.getId(), stack.getStatus(), Status.WAIT_FOR_SYNC);
            stack.setStatus(Status.WAIT_FOR_SYNC);
            stackRepository.save(stack);
            cleanInstanceMetaData(instanceMetaDataRepository.findAllInStack(stack.getId()));
            fireEvent(stack);
        }
    }

    private void cleanInstanceMetaData(Set<InstanceMetaData> metadataSet) {
        for (InstanceMetaData metadata : metadataSet) {
            if (InstanceStatus.REQUESTED.equals(metadata.getInstanceStatus()) && metadata.getInstanceId() == null) {
                LOGGER.info("InstanceMetaData [privateId: '{}'] is deleted at CB start.", metadata.getPrivateId());
                instanceMetaDataRepository.delete(metadata);
            }
        }
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

    private void fireEvent(Stack stack) {
        eventService.fireCloudbreakEvent(stack.getId(), WAIT_FOR_SYNC.name(),
                "Couldn't retrieve the cluster's status, push sync before continuing with another operation.");
    }
}
