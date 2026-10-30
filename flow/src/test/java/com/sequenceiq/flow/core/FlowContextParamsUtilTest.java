package com.sequenceiq.flow.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.eventbus.Event;

class FlowContextParamsUtilTest {

    @Test
    void getContextParamsReturnsMutableEmptyMapWithoutHeader() {
        Map<Object, Object> contextParams = FlowContextParamsUtil.getContextParams(new Event<>("payload"));

        assertThat(contextParams).isEmpty();
        contextParams.put("key", "value");
        assertThat(contextParams).containsEntry("key", "value");
    }

    @Test
    void getContextParamsReturnsIndependentCopyOfHeader() {
        Map<Object, Object> header = new HashMap<>(Map.of("key", "value"));
        Event<String> event = new Event<>(new Event.Headers(Map.of(FlowConstants.FLOW_CONTEXTPARAMS_ID, header)), "payload");

        Map<Object, Object> contextParams = FlowContextParamsUtil.getContextParams(event);
        contextParams.put("other", "value");

        assertThat(contextParams).containsEntry("key", "value").isNotSameAs(header);
        assertThat(header).doesNotContainKey("other");
    }
}
