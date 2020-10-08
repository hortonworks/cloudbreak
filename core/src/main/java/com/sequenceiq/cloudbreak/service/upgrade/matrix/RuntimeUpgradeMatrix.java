package com.sequenceiq.cloudbreak.service.upgrade.matrix;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RuntimeUpgradeMatrix {

    private final Runtime targetRuntime;

    private final Set<Runtime> sourceRuntime;

    @JsonCreator
    public RuntimeUpgradeMatrix(
            @JsonProperty("target_runtime") Runtime targetRuntime,
            @JsonProperty("source_runtime") Set<Runtime> sourceRuntime) {
        this.targetRuntime = targetRuntime;
        this.sourceRuntime = sourceRuntime;
    }

    public Runtime getTargetRuntime() {
        return targetRuntime;
    }

    public Set<Runtime> getSourceRuntime() {
        return sourceRuntime;
    }
}
