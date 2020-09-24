package com.sequenceiq.cloudbreak.service.upgrade;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;

@Component
class ComponentBuildNumberComparator {

    boolean compare(Image currentImage, Image newImage, String buildNumberKey) {
        Optional<String> currentVersion = getBuildVersion(currentImage, buildNumberKey);
        Optional<String> newVersion = getBuildVersion(newImage, buildNumberKey);
        return currentVersion.isPresent() && newVersion.isPresent() && compare(currentVersion, newVersion);
    }

    private Optional<String> getBuildVersion(Image image, String key) {
        return Optional.ofNullable(image.getPackageVersions()).map(map -> map.get(key));
    }

    private boolean compare(Optional<String> currentVersion, Optional<String> newVersion) {
        return currentVersion.map(Integer::parseInt).get() <= newVersion.map(Integer::parseInt).get();
    }
}
