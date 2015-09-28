package com.sequenceiq.cloudbreak.service.cluster.flow;

import com.sequenceiq.cloudbreak.common.type.PluginExecutionType;

public class RecipeScript {
    private String script;
    private ClusterLifecycleEvent clusterLifecycleEvent;
    private PluginExecutionType executionType;

    public RecipeScript(String script, ClusterLifecycleEvent clusterLifecycleEvent, PluginExecutionType executionType) {
        this.script = script;
        this.clusterLifecycleEvent = clusterLifecycleEvent;
        this.executionType = executionType;
    }

    public String getScript() {
        return script;
    }

    public ClusterLifecycleEvent getClusterLifecycleEvent() {
        return clusterLifecycleEvent;
    }

    public PluginExecutionType getExecutionType() {
        return executionType;
    }
}
