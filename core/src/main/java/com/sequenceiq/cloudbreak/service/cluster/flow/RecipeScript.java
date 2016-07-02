package com.sequenceiq.cloudbreak.service.cluster.flow;

public class RecipeScript {
    private String script;
    private ClusterLifecycleEvent clusterLifecycleEvent;

    public RecipeScript(String script, ClusterLifecycleEvent clusterLifecycleEvent) {
        this.script = script;
        this.clusterLifecycleEvent = clusterLifecycleEvent;
    }

    public String getScript() {
        return script;
    }

    public ClusterLifecycleEvent getClusterLifecycleEvent() {
        return clusterLifecycleEvent;
    }
}
