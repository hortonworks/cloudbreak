package com.sequenceiq.cloudbreak.service.stack;

import static com.sequenceiq.cloudbreak.util.EphemeralVolumeUtil.volumeIsEphemeral;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.VolumeUsageType;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;

@Component
public class InstanceGroupEphemeralVolumeChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceGroupEphemeralVolumeChecker.class);

    public boolean instanceGroupContainsOnlyDatabaseAndEphemeralVolumes(InstanceGroupView ig) {
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

    public boolean instanceGroupContainsEphemeralVolumes(InstanceGroupView ig) {
        return ig.getTemplate().getVolumeTemplates().stream()
                .anyMatch(volumeTemplate -> volumeIsEphemeral(volumeTemplate));
    }

    private boolean instanceGroupContainsOnlyEphemeralVolumes(InstanceGroupView ig) {
        return ig.getTemplate().getVolumeTemplates().stream()
                .allMatch(volumeTemplate -> volumeIsEphemeral(volumeTemplate));
    }

    private boolean nonEphemeralVolumesAreDatabases(InstanceGroupView ig) {
        return ig.getTemplate().getVolumeTemplates().stream()
                .filter(volumeTemplate -> !volumeIsEphemeral(volumeTemplate))
                .allMatch(volumeTemplate -> VolumeUsageType.DATABASE.equals(volumeTemplate.getUsageType()));
    }
}
