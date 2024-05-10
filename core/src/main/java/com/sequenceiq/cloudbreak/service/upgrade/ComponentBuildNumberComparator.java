package com.sequenceiq.cloudbreak.service.upgrade;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

@Component
public class ComponentBuildNumberComparator {

    boolean compare(Map<String, String> currentImagePackages, Map<String, String> newImagePackages, String buildNumberKey) {
        Optional<String> currentVersion = getBuildVersion(currentImagePackages, buildNumberKey);
        Optional<String> newVersion = getBuildVersion(newImagePackages, buildNumberKey);
        return currentVersion.isPresent() && newVersion.isPresent() && compare(currentVersion, newVersion);
    }

    private Optional<String> getBuildVersion(Map<String, String> image, String key) {
        return Optional.ofNullable(image).map(map -> map.get(key));
    }

    private boolean compare(Optional<String> currentVersion, Optional<String> newVersion) {
        return currentVersion.map(Integer::parseInt).get() <= newVersion.map(Integer::parseInt).get();
    }
}
