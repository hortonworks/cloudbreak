package com.sequenceiq.cloudbreak.cloud.model;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sequenceiq.common.model.Architecture;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ArchitectureVmTypes {

    private final EnumMap<Architecture, List<String>> defaultVmtypes;

    public ArchitectureVmTypes(List<String> x86, List<String> arm) {
        this.defaultVmtypes = new EnumMap<>(Architecture.class);
        if (x86 != null) {
            defaultVmtypes.put(Architecture.X86_64, x86);
        }
        if (arm != null) {
            defaultVmtypes.put(Architecture.ARM64, arm);
        }
    }

    @JsonCreator
    public ArchitectureVmTypes(Map<Architecture, List<String>> defaultVmtypes) {
        this.defaultVmtypes = defaultVmtypes != null ? new EnumMap<>(defaultVmtypes) : new EnumMap<>(Architecture.class);
    }

    public List<String> get(Architecture architecture) {
        return defaultVmtypes.get(architecture);
    }

    @JsonValue
    public Map<Architecture, List<String>> getDefaultVmtypes() {
        return defaultVmtypes;
    }
}
