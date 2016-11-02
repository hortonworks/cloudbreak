package com.sequenceiq.cloudbreak.orchestrator.model;

public class LogVolumePath {
    private final String hostPath;

    private final String containerPath;

    public LogVolumePath(String hostPath, String containerPath) {
        this.hostPath = hostPath;
        this.containerPath = containerPath;
    }

    public String getHostPath() {
        return hostPath;
    }

    public String getContainerPath() {
        return containerPath;
    }
}
