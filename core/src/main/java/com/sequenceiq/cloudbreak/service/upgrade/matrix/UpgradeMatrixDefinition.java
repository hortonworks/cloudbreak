package com.sequenceiq.cloudbreak.service.upgrade.matrix;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class UpgradeMatrixDefinition {

    private final Set<RuntimeUpgradeMatrix> runtimeUpgradeMatrix;

    @JsonCreator
    public UpgradeMatrixDefinition(@JsonProperty("runtime_upgrade_matrix") Set<RuntimeUpgradeMatrix> runtimeUpgradeMatrix) {
        this.runtimeUpgradeMatrix = runtimeUpgradeMatrix;
    }

    public Set<RuntimeUpgradeMatrix> getRuntimeUpgradeMatrix() {
        return runtimeUpgradeMatrix;
    }
}
