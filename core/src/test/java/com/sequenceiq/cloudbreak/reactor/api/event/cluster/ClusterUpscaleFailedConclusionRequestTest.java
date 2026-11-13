package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.json.JsonUtil;

class ClusterUpscaleFailedConclusionRequestTest {

    @Test
    void repairDefaultsToFalseWhenMissingFromJson() throws Exception {
        String legacyJson = "{\"@type\":\"" + ClusterUpscaleFailedConclusionRequest.class.getName() + "\",\"resourceId\":1}";

        ClusterUpscaleFailedConclusionRequest restored = JsonUtil.readValue(legacyJson, ClusterUpscaleFailedConclusionRequest.class);

        assertFalse(restored.isRepair());
    }

    @Test
    void repairIsPreservedThroughSerialization() throws Exception {
        ClusterUpscaleFailedConclusionRequest request = new ClusterUpscaleFailedConclusionRequest(1L, true);

        ClusterUpscaleFailedConclusionRequest restored =
                JsonUtil.readValue(JsonUtil.writeValueAsString(request), ClusterUpscaleFailedConclusionRequest.class);

        assertTrue(restored.isRepair());
    }
}
