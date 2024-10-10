package com.sequenceiq.cloudbreak.core.flow2.cluster.java;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class SetDefaultJavaVersionTriggerEvent extends StackEvent {

    private final String defaultJavaVersion;

    private final boolean restartServices;

    private final boolean restartCM;

    public SetDefaultJavaVersionTriggerEvent(String selector, Long stackId, String defaultJavaVersion, boolean restartServices, boolean restartCM) {
        super(selector, stackId);
        this.defaultJavaVersion = defaultJavaVersion;
        this.restartServices = restartServices;
        this.restartCM = restartCM;
    }

    @JsonCreator
    public SetDefaultJavaVersionTriggerEvent(@JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("defaultJavaVersion") String defaultJavaVersion,
            @JsonProperty("restartServices") boolean restartServices,
            @JsonProperty("restartCM") boolean restartCM,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted) {
        super(selector, stackId, accepted);
        this.defaultJavaVersion = defaultJavaVersion;
        this.restartServices = restartServices;
        this.restartCM = restartCM;
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

    @Override
    public String toString() {
        return "SetDefaultJavaVersionTriggerEvent{" +
                "defaultJavaVersion='" + defaultJavaVersion + '\'' +
                ", restartServices=" + restartServices +
                "} " + super.toString();
    }
}
