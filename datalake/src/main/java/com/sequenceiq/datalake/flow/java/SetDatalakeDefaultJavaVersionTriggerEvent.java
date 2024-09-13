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

    public SetDatalakeDefaultJavaVersionTriggerEvent(String selector, Long sdxId, String userId, String defaultJavaVersion, boolean restartServices) {
        super(selector, sdxId, userId);
        this.defaultJavaVersion = defaultJavaVersion;
        this.restartServices = restartServices;
    }

    @JsonCreator
    public SetDatalakeDefaultJavaVersionTriggerEvent(@JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("defaultJavaVersion") String defaultJavaVersion,
            @JsonProperty("restartServices") boolean restartServices,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted) {
        super(selector, sdxId, userId, accepted);
        this.defaultJavaVersion = defaultJavaVersion;
        this.restartServices = restartServices;
    }

    public String getDefaultJavaVersion() {
        return defaultJavaVersion;
    }

    public boolean isRestartServices() {
        return restartServices;
    }

    @Override
    public String toString() {
        return "SetDefaultJavaVersionTriggerEvent{" +
                "defaultJavaVersion='" + defaultJavaVersion + '\'' +
                ", restartServices=" + restartServices +
                "} " + super.toString();
    }

}
