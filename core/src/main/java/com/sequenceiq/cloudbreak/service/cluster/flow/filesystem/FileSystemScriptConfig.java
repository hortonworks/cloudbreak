package com.sequenceiq.cloudbreak.service.cluster.flow.filesystem;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.api.model.ExecutionType;
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterLifecycleEvent;

public class FileSystemScriptConfig {
    private String scriptLocation;

    private ClusterLifecycleEvent clusterLifecycleEvent;

    private ExecutionType executionType;

    private Map<String, String> properties = new HashMap<>();

    public FileSystemScriptConfig(String scriptLocation, ClusterLifecycleEvent clusterLifecycleEvent, ExecutionType executionType) {
        this.scriptLocation = scriptLocation;
        this.clusterLifecycleEvent = clusterLifecycleEvent;
        this.executionType = executionType;
    }

    public FileSystemScriptConfig(String scriptLocation, ClusterLifecycleEvent clusterLifecycleEvent,
            ExecutionType executionType, Map<String, String> properties) {
        this(scriptLocation, clusterLifecycleEvent, executionType);
        this.properties = properties;
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

    public Map<String, String> getProperties() {
        return properties;
    }
}
