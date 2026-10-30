package com.sequenceiq.flow.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.sequenceiq.cloudbreak.eventbus.Event;

public final class FlowContextParamsUtil {

    private FlowContextParamsUtil() {
    }

    /**
     * Returns a mutable copy of the context params carried in the event headers, so the receiving flow cannot modify the sender's variables.
     */
    public static Map<Object, Object> getContextParams(Event<?> event) {
        Map<Object, Object> contextParams = event.getHeaders().get(FlowConstants.FLOW_CONTEXTPARAMS_ID);
        return contextParams == null ? new HashMap<>() : new HashMap<>(contextParams);
    }

    public static Map<Object, Object> filter(Map<Object, Object> contextParams, Set<String> keys) {
        return contextParams.entrySet().stream()
                .filter(entry -> entry.getValue() != null && keys.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (first, second) -> first, HashMap::new));
    }
}
