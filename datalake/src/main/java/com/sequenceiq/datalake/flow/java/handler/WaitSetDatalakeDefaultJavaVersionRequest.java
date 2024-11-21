package com.sequenceiq.datalake.flow.java.handler;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;

public class WaitSetDatalakeDefaultJavaVersionRequest extends SdxEvent {

    private final String defaultJavaVersion;

    @JsonCreator
    public WaitSetDatalakeDefaultJavaVersionRequest(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("defaultJavaVersion") String defaultJavaVersion) {
        super(sdxId, userId);
        this.defaultJavaVersion = defaultJavaVersion;
    }

    public String getDefaultJavaVersion() {
        return defaultJavaVersion;
    }
}