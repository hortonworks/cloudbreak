package com.sequenceiq.datalake.flow.datalake.restartservices.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.datalake.restartservices.DatalakeRestartServicesFlowEvent;

public class DatalakeRestartServicesStartEvent extends SdxEvent {

    private final boolean rollingRestart;

    private final boolean staleServicesOnly;

    @JsonCreator
    public DatalakeRestartServicesStartEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("sdxName") String sdxName,
            @JsonProperty("userId") String userId,
            @JsonProperty("rollingRestart") boolean rollingRestart,
            @JsonProperty("staleServicesOnly") boolean staleServicesOnly) {
        super(DatalakeRestartServicesFlowEvent.DATALAKE_RESTART_SERVICES_START_EVENT.event(), sdxId, sdxName, userId);
        this.rollingRestart = rollingRestart;
        this.staleServicesOnly = staleServicesOnly;
    }

    public boolean isRollingRestart() {
        return rollingRestart;
    }

    public boolean isStaleServicesOnly() {
        return staleServicesOnly;
    }

    @Override
    public String toString() {
        return "DatalakeRestartServicesStartEvent{" +
                super.toString() +
                ", rollingRestart=" + rollingRestart +
                ", staleServicesOnly=" + staleServicesOnly +
                '}';
    }
}
