package com.sequenceiq.cloudbreak.core.flow2.cluster.java.handler;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformRequest;

public class SetDefaultJavaVersionRequest extends ClusterPlatformRequest {

    private final String defaultJavaVersion;

    @JsonCreator
    public SetDefaultJavaVersionRequest(@JsonProperty("stackId") Long stackId, @JsonProperty("defaultJavaVersion") String defaultJavaVersion) {
        super(stackId);
        this.defaultJavaVersion = defaultJavaVersion;
    }

    public String getDefaultJavaVersion() {
        return defaultJavaVersion;
    }

}
