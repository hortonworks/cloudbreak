package com.sequenceiq.cloudbreak.cloud.model.catalog;

import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.RELEASE_VERSION_TAG;
import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.STACK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

class ImageTest {

    @Test
    void getRuntimeVersionPrefersReleaseVersionTag() {
        Image image = imageWith(Map.of(RELEASE_VERSION_TAG.getKey(), "7.3.2.20000"), Map.of(STACK.getKey(), "7.3.2"));

        assertEquals("7.3.2.20000", image.getRuntimeVersion().orElseThrow());
    }

    @Test
    void getRuntimeVersionFallsBackToStackPackageWhenTagMissing() {
        Image image = imageWith(Map.of(), Map.of(STACK.getKey(), "7.3.2.20000"));

        assertEquals("7.3.2.20000", image.getRuntimeVersion().orElseThrow());
    }

    @Test
    void getRuntimeVersionFallsBackToStackPackageWhenTagEmpty() {
        Image image = imageWith(Map.of(RELEASE_VERSION_TAG.getKey(), ""), Map.of(STACK.getKey(), "7.3.2.20000"));

        assertEquals("7.3.2.20000", image.getRuntimeVersion().orElseThrow());
    }

    @Test
    void getRuntimeVersionEmptyWhenNeitherPresent() {
        Image image = imageWith(Map.of(), Map.of());

        assertTrue(image.getRuntimeVersion().isEmpty());
    }

    @Test
    void getRuntimeVersionEmptyWhenTagsAndPackageVersionsNull() {
        Image image = imageWith(null, null);

        assertTrue(image.getRuntimeVersion().isEmpty());
    }

    private Image imageWith(Map<String, String> tags, Map<String, String> packageVersions) {
        return new Image("date", null, null, "description", "os", "uuid", "version", null, Map.of(), null, "osType",
                packageVersions, null, null, "cmBuildNumber", false, null, null, "x86_64", tags);
    }
}
