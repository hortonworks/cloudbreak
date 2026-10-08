package com.sequenceiq.cloudbreak.service.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.event.ClusterUpgradeParcelSettingsPreparationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.event.ClusterUpgradePreparationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.event.ClusterUpgradePreparationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeServiceValidationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationTriggerEvent;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;
import com.sequenceiq.common.model.OsType;

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

    @Test
    void testFlowTriggersPreservePropertiesAfterSerialization() throws Exception {
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withFlags(true, false, true);
        ClusterUpgradeValidationTriggerEvent validation = new ClusterUpgradeValidationTriggerEvent(1L, null, properties.targetImageId(),
                properties.lockComponents(), properties.rollingUpgradeEnabled(), properties.replaceVms(), properties, null);
        ClusterUpgradePreparationTriggerEvent preparation = new ClusterUpgradePreparationTriggerEvent(1L, null,
                new ImageChangeDto(1L, properties.targetImageId(), properties.imageCatalogName(), properties.imageCatalogUrl()),
                properties.runtimeVersion(), properties.currentOsType(), properties);

        ClusterUpgradeValidationTriggerEvent restoredValidation = JsonUtil.readValue(JsonUtil.writeValueAsString(validation),
                ClusterUpgradeValidationTriggerEvent.class);
        ClusterUpgradePreparationTriggerEvent restoredPreparation = JsonUtil.readValue(JsonUtil.writeValueAsString(preparation),
                ClusterUpgradePreparationTriggerEvent.class);

        assertEquals(properties, restoredValidation.getClusterUpgradeProperties());
        assertEquals(properties, restoredPreparation.getClusterUpgradeProperties());
        assertTrue(restoredValidation.isLockComponents());
        assertFalse(restoredValidation.isRollingUpgradeEnabled());
        assertEquals(properties.targetImage().catalogUrl(), restoredPreparation.getImageChangeDto().getImageCatalogUrl());
        assertEquals(properties.currentOsType().name(), JsonUtil.readTree(JsonUtil.writeValueAsString(restoredPreparation)).get("currentOsType").asText());
    }

    @Test
    void testPreparationEventsPreservePropertiesAfterSerialization() throws Exception {
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withRuntimeVersion("7.3.2");
        ClusterUpgradePreparationEvent download = new ClusterUpgradePreparationEvent("selector", 1L, Set.of(), "target", properties);
        ClusterUpgradeParcelSettingsPreparationEvent settings = new ClusterUpgradeParcelSettingsPreparationEvent(1L,
                new ImageChangeDto(1L, "target"), OsType.RHEL8, properties);

        assertEquals(properties, JsonUtil.readValue(JsonUtil.writeValueAsString(download), ClusterUpgradePreparationEvent.class).getClusterUpgradeProperties());
        ClusterUpgradeParcelSettingsPreparationEvent restoredSettings = JsonUtil.readValue(JsonUtil.writeValueAsString(settings),
                ClusterUpgradeParcelSettingsPreparationEvent.class);
        assertEquals(properties, restoredSettings.getClusterUpgradeProperties());
        assertEquals("RHEL8", JsonUtil.readTree(JsonUtil.writeValueAsString(restoredSettings)).get("currentOsType").asText());
    }

    @Test
    void testValidationRequestPreservesCatalogAndFlagsAfterSerialization() throws Exception {
        ImageChangeDto image = new ImageChangeDto(1L, "target", "custom", "custom-url");
        ClusterUpgradeValidationTriggerEvent request = new ClusterUpgradeValidationTriggerEvent(1L, null, image, true, false, true);

        ClusterUpgradeValidationTriggerEvent restored = JsonUtil.readValue(JsonUtil.writeValueAsString(request),
                ClusterUpgradeValidationTriggerEvent.class);

        assertNull(restored.getClusterUpgradeProperties());
        assertEquals("target", restored.getImageId());
        assertEquals("custom", restored.getImageChangeDto().getImageCatalogName());
        assertEquals("custom-url", restored.getImageChangeDto().getImageCatalogUrl());
        assertTrue(restored.isLockComponents());
        assertFalse(restored.isRollingUpgradeEnabled());
        assertTrue(restored.isReplaceVms());
    }

    @Test
    void testLegacyTriggersDeserializeWithoutProperties() throws Exception {
        String validationJson = """
                {
                    "@type":"com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationTriggerEvent",
                    "resourceId":1,"imageId":"target","lockComponents":true,"rollingUpgradeEnabled":false,"replaceVms":true
                }
                """;
        String preparationJson = """
                {
                    "@type":"com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.event.ClusterUpgradePreparationTriggerEvent",
                    "resourceId":1,"imageChangeDto":{"stackId":1,"imageId":"target","imageCatalogName":"custom","imageCatalogUrl":"url"},
                    "runtimeVersion":"7.3.2","currentOsType":"RHEL8"}
                """;
        ClusterUpgradeValidationTriggerEvent validation = JsonUtil.readValue(validationJson, ClusterUpgradeValidationTriggerEvent.class);
        ClusterUpgradePreparationTriggerEvent preparation = JsonUtil.readValue(preparationJson, ClusterUpgradePreparationTriggerEvent.class);

        assertNull(validation.getClusterUpgradeProperties());
        assertTrue(validation.isLockComponents());
        assertTrue(validation.isReplaceVms());
        assertNull(preparation.getClusterUpgradeProperties());
        assertEquals("custom", preparation.getImageChangeDto().getImageCatalogName());
        assertEquals("RHEL8", JsonUtil.readTree(JsonUtil.writeValueAsString(preparation)).get("currentOsType").asText());
    }
}
