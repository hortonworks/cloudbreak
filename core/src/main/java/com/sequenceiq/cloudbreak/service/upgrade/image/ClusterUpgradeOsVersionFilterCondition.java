package com.sequenceiq.cloudbreak.service.upgrade.image;

import static com.sequenceiq.common.model.OsType.RHEL9;

import java.util.Set;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.common.model.OsType;

@Component
public class ClusterUpgradeOsVersionFilterCondition {

    @Inject
    private OsChangeService osChangeService;

    public boolean isImageAllowed(OsType currentOsType, String currentArchitecture, Image image, boolean rhel9Enabled, Set<OsType> osUsedByInstances) {
        return isOsEntitled(image, rhel9Enabled) && (isOsMatches(currentOsType, image) || isOsChangePermitted(currentOsType, currentArchitecture, image,
                osUsedByInstances));
    }

    private boolean isOsChangePermitted(OsType currentOsType, String currentArchitecture, Image image, Set<OsType> osUsedByInstances) {
        return osChangeService.isOsChangePermitted(image, currentOsType, osUsedByInstances, currentArchitecture);
    }

    private boolean isOsEntitled(Image image, boolean rhel9Enabled) {
        return !RHEL9.matches(image.getOs(), image.getOsType()) || rhel9Enabled;
    }

    private boolean isOsMatches(OsType currentOsType, Image newImage) {
        return newImage.getOs().equalsIgnoreCase(currentOsType.getOs()) && newImage.getOsType().equalsIgnoreCase(currentOsType.getOsType());
    }
}
