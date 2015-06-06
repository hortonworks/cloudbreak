package com.sequenceiq.cloudbreak.service.stack.flow;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.InstanceStatus;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.HostGroupRepository;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;

@Service
public class TerminationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TerminationService.class);
    private static final String DELIMITER = "_";

    @javax.annotation.Resource
    private Map<CloudPlatform, CloudPlatformConnector> cloudPlatformConnectors;

    @Inject
    private StackRepository stackRepository;

    @Inject
    private ClusterRepository clusterRepository;

    @Inject
    private RetryingStackUpdater retryingStackUpdater;

    @Inject
    private HostGroupRepository hostGroupRepository;

    public void terminateStack(Long stackId, CloudPlatform cloudPlatform) {
        final Stack stack = stackRepository.findOneWithLists(stackId);
        try {
            cloudPlatformConnectors.get(cloudPlatform).deleteStack(stack, stack.getCredential());
        } catch (Exception ex) {
            LOGGER.error("Failed to terminate cluster infrastructure. Stack id {}", stack.getId());
            throw new TerminationFailedException(ex);
        }
    }

    public void finalizeTermination(Long stackId) {
        Stack stack = stackRepository.findOneWithLists(stackId);
        try {
            Date now = new Date();
            String terminatedName = stack.getName() + DELIMITER + now.getTime();
            Cluster cluster = stack.getCluster();
            if (cluster != null) {
                cluster.setName(terminatedName);
                cluster.setBlueprint(null);
                clusterRepository.save(cluster);
                for (HostGroup hostGroup : hostGroupRepository.findHostGroupsInCluster(cluster.getId())) {
                    hostGroup.getRecipes().clear();
                    hostGroupRepository.save(hostGroup);
                }
            }
            stack.setCredential(null);
            stack.setNetwork(null);
            stack.setName(terminatedName);
            terminateMetaDataInstances(stack);
            retryingStackUpdater.updateStack(stack);
        } catch (Exception ex) {
            LOGGER.error("Failed to terminate cluster infrastructure. Stack id {}", stack.getId());
            throw new TerminationFailedException(ex);
        }
    }

    private void terminateMetaDataInstances(Stack stack) {
        for (InstanceMetaData metaData : stack.getRunningInstanceMetaData()) {
            long timeInMillis = Calendar.getInstance().getTimeInMillis();
            metaData.setTerminationDate(timeInMillis);
            metaData.setInstanceStatus(InstanceStatus.TERMINATED);
        }
    }
}
