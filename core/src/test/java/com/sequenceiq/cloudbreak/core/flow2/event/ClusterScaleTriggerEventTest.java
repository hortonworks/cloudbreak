package com.sequenceiq.cloudbreak.core.flow2.event;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.json.JsonUtil;

class ClusterScaleTriggerEventTest {

    @Test
    void repairDefaultsToFalseWhenMissingFromJson() throws Exception {
        String legacyJson = "{\"@type\":\"" + ClusterScaleTriggerEvent.class.getName()
                + "\",\"selector\":\"CLUSTER_UPSCALE_TRIGGER_EVENT\",\"resourceId\":1}";

        ClusterScaleTriggerEvent restored = JsonUtil.readValue(legacyJson, ClusterScaleTriggerEvent.class);

        assertFalse(restored.isRepair());
    }

    @Test
    void repairIsPreservedThroughSerialization() throws Exception {
        ClusterScaleTriggerEvent event = new ClusterScaleTriggerEvent("CLUSTER_UPSCALE_TRIGGER_EVENT", 1L, null, null, null);
        event.setRepair(true);

        ClusterScaleTriggerEvent restored = JsonUtil.readValue(JsonUtil.writeValueAsString(event), ClusterScaleTriggerEvent.class);

        assertTrue(restored.isRepair());
    }
}
