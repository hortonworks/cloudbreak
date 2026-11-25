package com.sequenceiq.freeipa.flow.freeipa.rollingvscale.event;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.freeipa.flow.freeipa.verticalscale.model.FreeIpaVerticalScaleParameters;

class FreeIpaRollingVerticalScaleTriggerEventTest {

    private static final FreeIpaVerticalScaleParameters SCALE_CONFIG = new FreeIpaVerticalScaleParameters("master", "m5.xlarge", "m5.large", null);

    @Test
    void testTriggerEventRoundTrip() throws Exception {
        FreeIpaRollingVerticalScaleTriggerEvent event =
                new FreeIpaRollingVerticalScaleTriggerEvent(1L, "i-1", SCALE_CONFIG, true, "op-id");

        FreeIpaRollingVerticalScaleTriggerEvent result =
                JsonUtil.readValue(JsonUtil.writeValueAsString(event), FreeIpaRollingVerticalScaleTriggerEvent.class);

        assertThat(result.getInstanceId()).isEqualTo("i-1");
        assertThat(result.isFinalChain()).isTrue();
        assertThat(result.getOperationId()).isEqualTo("op-id");
        assertThat(result.getScaleConfig()).isNotNull();
    }

    @Test
    void testChainTriggerEventRoundTrip() throws Exception {
        FreeIpaRollingVerticalScaleChainTriggerEvent event = new FreeIpaRollingVerticalScaleChainTriggerEvent(1L, SCALE_CONFIG, "op-id");

        FreeIpaRollingVerticalScaleChainTriggerEvent result =
                JsonUtil.readValue(JsonUtil.writeValueAsString(event), FreeIpaRollingVerticalScaleChainTriggerEvent.class);

        assertThat(result.getOperationId()).isEqualTo("op-id");
        assertThat(result.getScaleConfig()).isNotNull();
    }
}
