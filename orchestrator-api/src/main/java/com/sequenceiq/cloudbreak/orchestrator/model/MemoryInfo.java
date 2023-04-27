package com.sequenceiq.cloudbreak.orchestrator.model;

import java.util.Map;

public class MemoryInfo {

    private final Map<String, Map<String, String>> values;

    public MemoryInfo(Map<String, Map<String, String>> values) {
        this.values = values;
    }

    public Memory getTotalMemory() {
        Map<String, String> memTotal = values.get("MemTotal");
        int value = Integer.parseInt(memTotal.get("value"));
        String unit = memTotal.get("unit").toLowerCase();
        return Memory.of(value, unit);
    }
}
