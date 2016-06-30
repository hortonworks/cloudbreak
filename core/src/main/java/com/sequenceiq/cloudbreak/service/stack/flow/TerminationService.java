package com.sequenceiq.cloudbreak.service.stack.flow;

import static com.sequenceiq.cloudbreak.api.model.Status.DELETE_COMPLETED;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.FileSystemType;
import com.sequenceiq.cloudbreak.api.model.InstanceStatus;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.InstanceGroupRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterTerminationService;
import com.sequenceiq.cloudbreak.service.cluster.flow.filesystem.FileSystemConfigurator;
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderConnectorAdapter;

@Service
@Transactional
public class TerminationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TerminationService.class);
    private static final String DELIMITER = "_";

    @Inject
    private ServiceProviderConnectorAdapter connector;

    @Inject
    private StackRepository stackRepository;

    @Inject
    private InstanceGroupRepository instanceGroupRepository;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private InstanceMetaDataRepository instanceMetaDataRepository;

    @Inject
    private ClusterTerminationService clusterTerminationService;

    @Resource
    private Map<FileSystemType, FileSystemConfigurator> fileSystemConfigurators;

    public void finalizeTermination(Long stackId, boolean force) {
        Stack stack = stackRepository.findOneWithLists(stackId);
        try {
            Date now = new Date();
            String terminatedName = stack.getName() + DELIMITER + now.getTime();
            Cluster cluster = stack.getCluster();
            if (!force && cluster != null) {
                throw new TerminationFailedException(String.format("There is a cluster installed on stack '%s', terminate it first!.", stackId));
            } else if (cluster != null) {
                clusterTerminationService.finalizeClusterTermination(cluster.getId());
            }
            stack.setCredential(null);
            stack.setNetwork(null);
            stack.setName(terminatedName);
            terminateInstanceGroups(stack);
            terminateMetaDataInstances(stack);
            stackRepository.save(stack);
            stackUpdater.updateStackStatus(stackId, DELETE_COMPLETED, "Stack was terminated successfully.");
        } catch (Exception ex) {
            LOGGER.error("Failed to terminate cluster infrastructure. Stack id {}", stack.getId());
            throw new TerminationFailedException(ex);
        }
    }

    private void terminateInstanceGroups(Stack stack) {
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            instanceGroup.setSecurityGroup(null);
            instanceGroupRepository.save(instanceGroup);
        }

    }

    private void terminateMetaDataInstances(Stack stack) {
        List<InstanceMetaData> instanceMetaDatas = new ArrayList<>();
        for (InstanceMetaData metaData : stack.getRunningInstanceMetaData()) {
            long timeInMillis = Calendar.getInstance().getTimeInMillis();
            metaData.setTerminationDate(timeInMillis);
            metaData.setInstanceStatus(InstanceStatus.TERMINATED);
            instanceMetaDatas.add(metaData);
        }
        instanceMetaDataRepository.save(instanceMetaDatas);
    }
}