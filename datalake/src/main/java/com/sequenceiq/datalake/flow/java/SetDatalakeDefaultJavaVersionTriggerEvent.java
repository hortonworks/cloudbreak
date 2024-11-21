package com.sequenceiq.datalake.flow.java;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.datalake.flow.SdxEvent;

public class SetDatalakeDefaultJavaVersionTriggerEvent extends SdxEvent {

    private final String defaultJavaVersion;

    private final boolean restartServices;

    private final boolean restartCM;

    private final boolean rollingRestart;

    public SetDatalakeDefaultJavaVersionTriggerEvent(String selector, Long sdxId, String userId, String defaultJavaVersion, boolean restartServices,
            boolean restartCM, boolean rollingRestart) {
        super(selector, sdxId, userId);
        this.defaultJavaVersion = defaultJavaVersion;
        this.restartServices = restartServices;
        this.restartCM = restartCM;
        this.rollingRestart = rollingRestart;
    }

    @JsonCreator
    public SetDatalakeDefaultJavaVersionTriggerEvent(@JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("defaultJavaVersion") String defaultJavaVersion,
            @JsonProperty("restartServices") boolean restartServices,
            @JsonProperty("restartCM") boolean restartCM,
            @JsonProperty("rollingRestart") boolean rollingRestart,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted) {
        super(selector, sdxId, userId, accepted);
        this.defaultJavaVersion = defaultJavaVersion;
        this.restartServices = restartServices;
        this.restartCM = restartCM;
        this.rollingRestart = rollingRestart;
    }

    public String getDefaultJavaVersion() {
        return defaultJavaVersion;
    }

    public boolean isRestartServices() {
        return restartServices;
    }

    public boolean isRestartCM() {
        return restartCM;
    }

    public boolean isRollingRestart() {
        return rollingRestart;
    }

    @Override
    public String toString() {
        return "SetDefaultJavaVersionTriggerEvent{" +
                "defaultJavaVersion='" + defaultJavaVersion + '\'' +
                ", restartServices=" + restartServices +
                ", restartCM=" + restartCM +
                ", rollingRestart=" + rollingRestart +
                "} " + super.toString();
    }

}
