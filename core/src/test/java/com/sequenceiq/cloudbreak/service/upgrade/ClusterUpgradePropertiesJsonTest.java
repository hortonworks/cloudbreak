package com.sequenceiq.cloudbreak.service.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeServiceValidationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationEvent;

class ClusterUpgradePropertiesJsonTest {

    @Test
    void testRoundTripSerialization() throws Exception {
        ClusterUpgradeProperties original = ClusterUpgradePropertiesTestUtils.withRuntimeVersionAndFlags("7.2.18", true, false, true);

        String json = JsonUtil.writeValueAsString(original);
        ClusterUpgradeProperties restored = JsonUtil.readValue(json, ClusterUpgradeProperties.class);

        assertEquals(original.targetImageId(), restored.targetImageId());
        assertEquals(original.runtimeVersion(), restored.runtimeVersion());
        assertEquals(original.isLockComponents(), restored.isLockComponents());
        assertEquals(original.isRollingUpgradeEnabled(), restored.isRollingUpgradeEnabled());
        assertEquals(original.isReplaceVms(), restored.isReplaceVms());
        assertEquals(original.currentImageId(), restored.currentImageId());
        assertEquals(original.toCurrentCloudImage().getImageName(), restored.toCurrentCloudImage().getImageName());
        assertEquals(original.toCurrentCloudImage().getArchitecture(), restored.toCurrentCloudImage().getArchitecture());
        assertEquals(original.toCurrentCloudImage().getImageCatalogUrl(), restored.toCurrentCloudImage().getImageCatalogUrl());

        JsonNode root = JsonUtil.readTree(json);
        assertFalse(root.has("targetImageId"));
        assertFalse(root.has("cloudImage"));
        assertFalse(root.has("runtimeVersion"));
        assertTrue(root.has("options"));
        assertTrue(root.has("currentImage"));
        assertTrue(root.has("targetImage"));
    }

    @Test
    void testLegacyValidationEventJsonWithoutClusterUpgradeProperties() throws Exception {
        String legacyJson = """
                {
                    "@type": "com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeServiceValidationEvent",
                    "selector": "VALIDATE_SERVICES_EVENT",
                    "resourceId": 1,
                    "imageId": "legacyTargetImageId",
                    "lockComponents": true,
                    "rollingUpgradeEnabled": false,
                    "targetRuntime": "7.2.18",
                    "replaceVms": true
                }
                """;

        ClusterUpgradeServiceValidationEvent event = JsonUtil.readValue(legacyJson, ClusterUpgradeServiceValidationEvent.class);

        assertNull(event.getClusterUpgradeProperties());
        assertEquals("legacyTargetImageId", event.getImageId());
        assertTrue(event.isLockComponents());
        assertFalse(event.isRollingUpgradeEnabled());
        assertTrue(event.isReplaceVms());
        assertEquals("7.2.18", event.getTargetRuntime());
        assertNotNull(event.toString());
    }

    @Test
    void testLegacyValidationEventJsonWithUnknownProperties() throws Exception {
        String legacyJson = """
                {
                    "@type": "com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationEvent",
                    "selector": "START_CLUSTER_UPGRADE_S3GUARD_VALIDATION_EVENT",
                    "resourceId": 2,
                    "imageId": "legacyImageId",
                    "targetImageId": "duplicateFieldFromOldSerialization"
                }
                """;

        ClusterUpgradeValidationEvent event = JsonUtil.readValue(legacyJson, ClusterUpgradeValidationEvent.class);

        assertNull(event.getClusterUpgradeProperties());
        assertEquals("legacyImageId", event.getImageId());
    }
}
