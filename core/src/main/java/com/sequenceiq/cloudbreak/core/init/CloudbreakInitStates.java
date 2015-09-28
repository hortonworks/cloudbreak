package com.sequenceiq.cloudbreak.core.init;

import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.common.type.InstanceStatus;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.common.type.Status;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;

@Component
public class CloudbreakInitStates {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakInitStates.class);

    @Inject
    private StackRepository stackRepository;

    @Inject
    private ClusterRepository clusterRepository;

    @Inject
    private InstanceMetaDataRepository instanceMetaDataRepository;

    @PostConstruct
    public void resetInProgressStates() {
        resetStackStatus();
        resetCloudStatus();
    }

    private void resetCloudStatus() {
        List<Cluster> clustersInProgress = clusterRepository.findByStatus(Status.UPDATE_IN_PROGRESS);
        for (Cluster cluster : clustersInProgress) {
            LOGGER.info("Cluster {} status is updated from {} to {} at CB start.", cluster.getId(), cluster.getStatus(), Status.WAIT_FOR_SYNC);
            cluster.setStatus(Status.WAIT_FOR_SYNC);
            clusterRepository.save(cluster);
        }
    }

    private void resetStackStatus() {
        List<Stack> stacksInProgress = stackRepository.findByStatus(Status.UPDATE_IN_PROGRESS);
        for (Stack stack : stacksInProgress) {
            LOGGER.info("Stack {} status is updated from {} to {} at CB start.", stack.getId(), stack.getStatus(), Status.WAIT_FOR_SYNC);
            stack.setStatus(Status.WAIT_FOR_SYNC);
            stackRepository.save(stack);
            cleanInstanceMetaData(instanceMetaDataRepository.findAllInStack(stack.getId()));
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

}
