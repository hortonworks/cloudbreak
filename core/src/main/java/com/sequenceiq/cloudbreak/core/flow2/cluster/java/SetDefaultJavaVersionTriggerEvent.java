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

    public SetDefaultJavaVersionTriggerEvent(String selector, Long stackId, String defaultJavaVersion, boolean restartServices) {
        super(selector, stackId);
        this.defaultJavaVersion = defaultJavaVersion;
        this.restartServices = restartServices;
    }

    @JsonCreator
    public SetDefaultJavaVersionTriggerEvent(@JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("defaultJavaVersion") String defaultJavaVersion,
            @JsonProperty("restartServices") boolean restartServices,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted) {
        super(selector, stackId, accepted);
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
