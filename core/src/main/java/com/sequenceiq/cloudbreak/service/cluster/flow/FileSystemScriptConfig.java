package com.sequenceiq.cloudbreak.service.cluster.flow;

import com.sequenceiq.cloudbreak.domain.PluginExecutionType;

public class FileSystemScriptConfig {
    private String scriptLocation;
    private ClusterLifecycleEvent clusterLifecycleEvent;
    private PluginExecutionType executionType;

    public FileSystemScriptConfig(String scriptLocation, ClusterLifecycleEvent clusterLifecycleEvent, PluginExecutionType executionType) {
        this.scriptLocation = scriptLocation;
        this.clusterLifecycleEvent = clusterLifecycleEvent;
        this.executionType = executionType;
    }

    public String getScriptLocation() {
        return scriptLocation;
    }

    public ClusterLifecycleEvent getClusterLifecycleEvent() {
        return clusterLifecycleEvent;
    }

    public PluginExecutionType getExecutionType() {
        return executionType;
    }
}
