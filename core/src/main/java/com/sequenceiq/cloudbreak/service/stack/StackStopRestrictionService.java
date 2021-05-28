package com.sequenceiq.cloudbreak.service.stack;

import java.util.Set;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.StopRestrictionReason;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;

@Service
public class StackStopRestrictionService {

    private static final String SUPPORTED_CLOUD_PLATFORM = "AWS";

    private static final String EPHEMERAL_VOLUME = "ephemeral";

    public StopRestrictionReason isInfrastructureStoppable(String cloudPlatform, Set<InstanceGroup> instanceGroups) {
        StopRestrictionReason reason = StopRestrictionReason.NONE;
        if (SUPPORTED_CLOUD_PLATFORM.equals(cloudPlatform)) {
            for (InstanceGroup instanceGroup : instanceGroups) {
                if (instanceGroup.getTemplate().getVolumeTemplates().stream()
                        .anyMatch(volume -> EPHEMERAL_VOLUME.equals(volume.getVolumeType()))) {
                    reason = StopRestrictionReason.EPHEMERAL_VOLUMES;
                    break;
                }
            }
        }
        return reason;
    }
}
