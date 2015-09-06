package com.sequenceiq.cloudbreak.service.cluster.flow;

import com.sequenceiq.cloudbreak.domain.PluginExecutionType;

public class FileSystemScript {
    private String script;
    private ClusterLifecycleEvent clusterLifecycleEvent;
    private PluginExecutionType executionType;

    public FileSystemScript(String script, ClusterLifecycleEvent clusterLifecycleEvent, PluginExecutionType executionType) {
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
