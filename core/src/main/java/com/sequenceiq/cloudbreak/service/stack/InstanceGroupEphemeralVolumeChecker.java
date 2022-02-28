package com.sequenceiq.cloudbreak.service.stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.VolumeUsageType;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.common.model.AwsDiskType;

@Component
public class InstanceGroupEphemeralVolumeChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceGroupEphemeralVolumeChecker.class);

    public boolean instanceGroupContainsOnlyDatabaseAndEphemeralVolumes(InstanceGroup ig) {
        if (instanceGroupContainsOnlyEphemeralVolumes(ig)) {
            LOGGER.debug("Instance group [{}] contains only ephemeral volumes.", ig.getGroupName());
            return true;
        } else if (instanceGroupContainsEphemeralVolumes(ig) && nonEphemeralVolumesAreDatabases(ig)) {
            LOGGER.debug("Instance group [{}] contains only ephemeral volumes besides the volumes used for embedded database.", ig.getGroupName());
            return true;
        } else {
            LOGGER.debug("Instance group [{}] contains non-ephemeral volumes that are not used for embedded database.", ig.getGroupName());
            return false;
        }
    }

    public boolean instanceGroupContainsEphemeralVolumes(InstanceGroup ig) {
        return ig.getTemplate().getVolumeTemplates().stream()
                .anyMatch(volumeTemplate -> AwsDiskType.Ephemeral.value().equalsIgnoreCase(volumeTemplate.getVolumeType()));
    }

    private boolean instanceGroupContainsOnlyEphemeralVolumes(InstanceGroup ig) {
        return ig.getTemplate().getVolumeTemplates().stream()
                .allMatch(volumeTemplate -> AwsDiskType.Ephemeral.value().equalsIgnoreCase(volumeTemplate.getVolumeType()));
    }

    private boolean nonEphemeralVolumesAreDatabases(InstanceGroup ig) {
        return ig.getTemplate().getVolumeTemplates().stream()
                .filter(volumeTemplate -> !AwsDiskType.Ephemeral.value().equalsIgnoreCase(volumeTemplate.getVolumeType()))
                .allMatch(volumeTemplate -> VolumeUsageType.DATABASE.equals(volumeTemplate.getUsageType()));
    }
}
