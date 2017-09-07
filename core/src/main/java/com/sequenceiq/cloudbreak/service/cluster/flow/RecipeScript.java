package com.sequenceiq.cloudbreak.service.cluster.flow;

import com.sequenceiq.cloudbreak.api.model.ExecutionType;

public class RecipeScript {

    private final String script;

    private ExecutionType executionType;

    private final ClusterLifecycleEvent clusterLifecycleEvent;

    public RecipeScript(String script, ClusterLifecycleEvent clusterLifecycleEvent) {
        this.script = script;
        this.clusterLifecycleEvent = clusterLifecycleEvent;
    }

    public RecipeScript(String script, ExecutionType executionType, ClusterLifecycleEvent clusterLifecycleEvent) {
        this.script = script;
        this.executionType = executionType;
        this.clusterLifecycleEvent = clusterLifecycleEvent;
    }

    public String getScript() {
        return script;
    }

    public ExecutionType getExecutionType() {
        return executionType;
    }

    public ClusterLifecycleEvent getClusterLifecycleEvent() {
        return clusterLifecycleEvent;
    }
}
