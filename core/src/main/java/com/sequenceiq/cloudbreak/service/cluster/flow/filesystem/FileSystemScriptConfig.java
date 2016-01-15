package com.sequenceiq.cloudbreak.service.cluster.flow.filesystem;

import com.sequenceiq.cloudbreak.api.model.PluginExecutionType;
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterLifecycleEvent;

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
