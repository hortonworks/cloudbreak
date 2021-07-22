package com.sequenceiq.cloudbreak.service.stack;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.cloudbreak.domain.StopRestrictionReason;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.common.model.AwsDiskType;

@Service
public class StackStopRestrictionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackStopRestrictionService.class);

    private static final String SUPPORTED_CLOUD_PLATFORM = "AWS";

    public StopRestrictionReason isInfrastructureStoppable(String cloudPlatform, Set<InstanceGroup> instanceGroups) {
        StopRestrictionReason reason = StopRestrictionReason.NONE;
        if (SUPPORTED_CLOUD_PLATFORM.equals(cloudPlatform)) {
            for (InstanceGroup instanceGroup : instanceGroups) {
                TemporaryStorage temporaryStorage = instanceGroup.getTemplate().getTemporaryStorage();
                if (TemporaryStorage.EPHEMERAL_VOLUMES.equals(temporaryStorage)) {
                    reason = StopRestrictionReason.NONE;
                    LOGGER.info("Group [{}] has ephemeral volume caching enabled. Allowing infrastructure to stop.", instanceGroup.getGroupName());
                    return reason;
                }
                if (onlyAwsEphemeralStorage(instanceGroup)) {
                    reason = StopRestrictionReason.EPHEMERAL_VOLUMES;
                    LOGGER.info("Infrastructure cannot be stopped. Instances in group [{}] have ephemeral storage only.", instanceGroup.getGroupName());
                    return reason;
                }
            }
        }
        return reason;
    }

    private boolean onlyAwsEphemeralStorage(InstanceGroup instanceGroup) {
        return instanceGroup.getTemplate().getVolumeTemplates().stream().allMatch(volume -> AwsDiskType.Ephemeral.value().equals(volume.getVolumeType()));
    }
}
