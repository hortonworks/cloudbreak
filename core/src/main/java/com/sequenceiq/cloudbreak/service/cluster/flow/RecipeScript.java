package com.sequenceiq.cloudbreak.service.cluster.flow;

import com.sequenceiq.cloudbreak.api.model.ExecutionType;

public class RecipeScript {
    private String script;
    private ClusterLifecycleEvent clusterLifecycleEvent;
    private ExecutionType executionType;

    public RecipeScript(String script, ClusterLifecycleEvent clusterLifecycleEvent, ExecutionType executionType) {
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

    public ExecutionType getExecutionType() {
        return executionType;
    }
}
