package com.sequenceiq.cloudbreak.service.stack.repair;

import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
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

@Component
public class StackRepairService {
    private static final Logger LOG = LoggerFactory.getLogger(StackRepairService.class);

    @Inject
    private ReactorFlowManager reactorFlowManager;

    @Inject
    private InstanceMetaDataRepository instanceMetaDataRepository;

    @Inject
    private HostMetadataRepository hostMetadataRepository;

    public void add(StackRepairNotificationRequest payload) {
        if (payload.getUnhealthyInstanceIds().isEmpty()) {
            LOG.warn("No instances are unhealthy, returning...");
            return;
        }
        UnhealthyInstances unhealthyInstances = groupInstancesByHostGroups(payload.getStack(), payload.getUnhealthyInstanceIds());
        reactorFlowManager.triggerStackRepairFlow(payload.getStackId(), unhealthyInstances);
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
}
