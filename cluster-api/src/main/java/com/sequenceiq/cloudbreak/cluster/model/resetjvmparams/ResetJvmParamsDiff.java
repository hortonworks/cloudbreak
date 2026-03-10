package com.sequenceiq.cloudbreak.cluster.model.resetjvmparams;

import java.util.ArrayList;
import java.util.List;

/**
 * Domain representation of the result of a JVM parameter recalculation diff.
 * Mirrors {@code com.cloudera.api.swagger.model.ApiHostReallocateMemoryResponse},
 * keeping CM-specific types confined to {@code cluster-cm}.
 *
 * <p>{@code configsBefore} is only populated for dry-run executions and contains
 * the original values of configs that would be affected. {@code configsAfter}
 * contains the new computed values for those same configs.
 */
public class ResetJvmParamsDiff {

    /**
     * Configs with their values before recalculation.
     * Only populated during dry runs. Matches the identity of entries in {@link #configsAfter}.
     */
    private List<JvmConfigRecord> configsBefore = new ArrayList<>();

    /** Configs whose value has changed (or would change) due to the recalculation. */
    private List<JvmConfigRecord> configsAfter = new ArrayList<>();

    public ResetJvmParamsDiff() {
    }

    public ResetJvmParamsDiff(List<JvmConfigRecord> configsBefore, List<JvmConfigRecord> configsAfter) {
        this.configsBefore = configsBefore;
        this.configsAfter = configsAfter;
    }

    public List<JvmConfigRecord> getConfigsBefore() {
        return configsBefore;
    }

    public void setConfigsBefore(List<JvmConfigRecord> configsBefore) {
        this.configsBefore = configsBefore;
    }

    public List<JvmConfigRecord> getConfigsAfter() {
        return configsAfter;
    }

    public void setConfigsAfter(List<JvmConfigRecord> configsAfter) {
        this.configsAfter = configsAfter;
    }

    public boolean hasChanges() {
        return configsAfter != null && !configsAfter.isEmpty();
    }

    @Override
    public String toString() {
        return "ResetJvmParamsDiff{" +
                "configsBefore=" + configsBefore +
                ", configsAfter=" + configsAfter +
                '}';
    }
}
