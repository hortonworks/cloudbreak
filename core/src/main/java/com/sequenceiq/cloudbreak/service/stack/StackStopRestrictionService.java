package com.sequenceiq.cloudbreak.service.stack;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.VersionComparator;
import com.sequenceiq.cloudbreak.cloud.model.CloudbreakDetails;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.cloudbreak.domain.StopRestrictionReason;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.common.model.AwsDiskType;

@Service
public class StackStopRestrictionService {

    public static final String MIN_VERSION = "2.48.0";

    private static final Logger LOGGER = LoggerFactory.getLogger(StackStopRestrictionService.class);

    private static final String RESTRICTED_CLOUD_PLATFORM = "AWS";

    @Inject
    private ComponentConfigProviderService componentConfigProviderService;

    public StopRestrictionReason isInfrastructureStoppable(Stack stack) {
        StopRestrictionReason reason = StopRestrictionReason.NONE;
        if (RESTRICTED_CLOUD_PLATFORM.equals(stack.getCloudPlatform())) {
            for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
                if (onlyEphemeralStorageInInstanceGroup(instanceGroup)) {
                    reason = StopRestrictionReason.EPHEMERAL_VOLUMES;
                    LOGGER.info("Infrastructure cannot be stopped. Instances in group [{}] have ephemeral storage only.", instanceGroup.getGroupName());
                    return reason;
                } else if (isCbVersionBeforeStopSupport(stack)) {
                    TemporaryStorage temporaryStorage = instanceGroup.getTemplate().getTemporaryStorage();
                    if (TemporaryStorage.EPHEMERAL_VOLUMES.equals(temporaryStorage)) {
                        reason = StopRestrictionReason.EPHEMERAL_VOLUME_CACHING;
                        LOGGER.info("Infrastructure cannot be stopped. Group [{}] has ephemeral volume caching enabled. " +
                                "Stopping clusters with ephemeral volume caching enabled are only available in clusters " +
                                "created at least with Cloudbreak version [{}]", instanceGroup.getGroupName(), MIN_VERSION);
                        return reason;
                    }
                    if (hasInstanceGroupAwsEphemeralStorage(instanceGroup)) {
                        reason = StopRestrictionReason.EPHEMERAL_VOLUMES;
                        LOGGER.info("Infrastructure cannot be stopped. Instances in group [{}] have ephemeral storage." +
                                "Stopping clusters with ephemeral storage instances are only available in clusters " +
                                "created at least with Cloudbreak version [{}]", instanceGroup.getGroupName(), MIN_VERSION);
                        return reason;
                    }
                }
            }
        }
        return reason;
    }

    private boolean hasInstanceGroupAwsEphemeralStorage(InstanceGroup instanceGroup) {
        return instanceGroup.getTemplate().getVolumeTemplates().stream().anyMatch(volume -> AwsDiskType.Ephemeral.value().equals(volume.getVolumeType()));
    }

    private boolean onlyEphemeralStorageInInstanceGroup(InstanceGroup instanceGroup) {
        return instanceGroup.getTemplate().getVolumeTemplates().stream().allMatch(volume -> AwsDiskType.Ephemeral.value().equals(volume.getVolumeType()));
    }

    private boolean isCbVersionBeforeStopSupport(Stack stack) {
        CloudbreakDetails cloudbreakDetails = componentConfigProviderService.getCloudbreakDetails(stack.getId());
        VersionComparator versionComparator = new VersionComparator();
        String version = StringUtils.substringBefore(cloudbreakDetails.getVersion(), "-");
        int compare = versionComparator.compare(() -> version, () -> MIN_VERSION);
        return compare < 0;
    }
}
