package com.sequenceiq.cloudbreak.service.cluster.flow.filesystem;

import com.sequenceiq.cloudbreak.api.model.ExecutionType;
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterLifecycleEvent;

public class FileSystemScriptConfig {
    private String scriptLocation;
    private ClusterLifecycleEvent clusterLifecycleEvent;
    private ExecutionType executionType;

    public FileSystemScriptConfig(String scriptLocation, ClusterLifecycleEvent clusterLifecycleEvent, ExecutionType executionType) {
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

    public ExecutionType getExecutionType() {
        return executionType;
    }
}
