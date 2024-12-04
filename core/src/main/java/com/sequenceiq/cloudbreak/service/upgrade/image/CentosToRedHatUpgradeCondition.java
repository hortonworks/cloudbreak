package com.sequenceiq.cloudbreak.service.upgrade.image;

import static com.sequenceiq.common.model.OsType.CENTOS7;
import static com.sequenceiq.common.model.OsType.RHEL8;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.service.image.CurrentImageUsageCondition;

@Component
public class CentosToRedHatUpgradeCondition {

    @Inject
    private CurrentImageUsageCondition currentImageUsageCondition;

    public boolean isCentosToRedhatUpgrade(Long stackId, Image newImage) {
        return isRedHatImage(newImage) && isAllInstanceUsingCentOs(stackId);
    }

    private boolean isRedHatImage(Image newImage) {
        return RHEL8.getOs().equalsIgnoreCase(newImage.getOs()) && RHEL8.getOsType().equalsIgnoreCase(newImage.getOsType());
    }

    private boolean isAllInstanceUsingCentOs(Long stackId) {
        return currentImageUsageCondition.isCurrentOsUsedOnInstances(stackId, CENTOS7.getOs());
    }
}
