package com.sequenceiq.datalake.flow.java.handler;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;

public class WaitSetDatalakeDefaultJavaVersionRequest extends SdxEvent {

    private final String defaultJavaVersion;

    private final boolean restartServices;

    @JsonCreator
    public WaitSetDatalakeDefaultJavaVersionRequest(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("defaultJavaVersion") String defaultJavaVersion,
            @JsonProperty("restartServices")boolean restartServices) {
        super(sdxId, userId);
        this.defaultJavaVersion = defaultJavaVersion;
        this.restartServices = restartServices;
    }

    public String getDefaultJavaVersion() {
        return defaultJavaVersion;
    }

    public boolean isRestartServices() {
        return restartServices;
    }
}