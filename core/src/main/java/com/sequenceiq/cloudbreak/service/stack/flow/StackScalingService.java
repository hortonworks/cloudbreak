package com.sequenceiq.cloudbreak.service.stack.flow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;

@Service
public class StackScalingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackScalingService.class);

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private TransactionService transactionService;

    @Inject
    private Clock clock;

    public int updateRemovedResourcesState(Collection<String> instanceIds, InstanceGroup instanceGroup) throws TransactionService.TransactionExecutionException {
        return transactionService.required(() -> {
            int nodesRemoved = 0;
            List<InstanceMetaData> notTerminatedInstanceMetadataSet = instanceMetaDataService.findAliveInstancesInInstanceGroup(instanceGroup.getId());
            for (InstanceMetaData instanceMetaData : notTerminatedInstanceMetadataSet) {
                if (instanceIds.contains(instanceMetaData.getInstanceId())) {
                    instanceMetaData.setTerminationDate(clock.getCurrentTimeMillis());
                    instanceMetaData.setInstanceStatus(InstanceStatus.TERMINATED);
                    nodesRemoved++;
                }
            }
            instanceMetaDataService.saveAll(notTerminatedInstanceMetadataSet);
            int nodeCount = instanceGroup.getNodeCount() - nodesRemoved;
            LOGGER.debug("Successfully terminated metadata of instances '{}' in stack.", instanceIds);
            return nodeCount;
        });
    }

    public Map<String, String> getUnusedInstanceIds(String instanceGroupName, Integer scalingAdjustment, Stack stack) {
        if (scalingAdjustment > 0) {
            LOGGER.error("Scaling adjustment shouldn't be a positive number, we are trying to downscaling..");
            return new HashMap<>();
        }
        InstanceGroup instanceGroup = stack.getInstanceGroupByInstanceGroupName(instanceGroupName);
        List<InstanceMetaData> unattachedInstanceMetaDatas = new ArrayList<>(instanceGroup.getUnattachedInstanceMetaDataSet());

        return unattachedInstanceMetaDatas.stream()
                .filter(instanceMetaData -> instanceMetaData.getInstanceId() != null && instanceMetaData.getDiscoveryFQDN() != null)
                .sorted(Comparator.comparing(InstanceMetaData::getStartDate))
                .limit(Math.abs(scalingAdjustment))
                .collect(Collectors.toMap(InstanceMetaData::getInstanceId, InstanceMetaData::getDiscoveryFQDN));
    }

}
