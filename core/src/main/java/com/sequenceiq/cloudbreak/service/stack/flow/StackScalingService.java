package com.sequenceiq.cloudbreak.service.stack.flow;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@Service
public class StackScalingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackScalingService.class);

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private TransactionService transactionService;

    @Inject
    private Clock clock;

    public void updateInstancesToTerminated(Collection<Long> privateIds, long stackId) throws TransactionService.TransactionExecutionException {
        transactionService.required(() -> {
            Set<InstanceMetaData> notTerminatedInstanceMetadataSet = instanceMetaDataService.getAllInstanceMetadataWithoutInstanceGroupByStackId(stackId);
            for (InstanceMetaData instanceMetaData : notTerminatedInstanceMetadataSet) {
                if (privateIds.contains(instanceMetaData.getPrivateId())) {
                    instanceMetaData.setTerminationDate(clock.getCurrentTimeMillis());
                    instanceMetaData.setInstanceStatus(InstanceStatus.TERMINATED);
                }
            }
            instanceMetaDataService.saveAll(notTerminatedInstanceMetadataSet);
            LOGGER.debug("Successfully terminated metadata of instances '{}' in stack.", privateIds);
        });
    }

    public Set<Long> getUnusedPrivateIds(InstanceGroupDto instanceGroupDto, Integer scalingAdjustment) {
        if (scalingAdjustment > 0) {
            LOGGER.error("Scaling adjustment shouldn't be a positive number, we are trying to downscaling..");
            return Collections.emptySet();
        }
        List<InstanceMetadataView> unattachedInstanceMetaDatas = instanceGroupDto.getUnattachedInstanceMetaData();

        return unattachedInstanceMetaDatas.stream()
                .filter(instanceMetaData -> instanceMetaData.getInstanceId() != null && instanceMetaData.getDiscoveryFQDN() != null)
                .sorted(Comparator.comparing(InstanceMetadataView::getStartDate))
                .limit(Math.abs(scalingAdjustment))
                .map(InstanceMetadataView::getPrivateId)
                .collect(Collectors.toSet());
    }

}
