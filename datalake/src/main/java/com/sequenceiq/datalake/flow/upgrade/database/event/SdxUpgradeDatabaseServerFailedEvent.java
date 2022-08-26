package com.sequenceiq.datalake.flow.upgrade.database.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.SdxFailedEvent;

public class SdxUpgradeDatabaseServerFailedEvent extends SdxFailedEvent {

    private final String message;

    @JsonCreator
    public SdxUpgradeDatabaseServerFailedEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("exception") Exception exception,
            @JsonProperty("message") String message) {
        super(sdxId, userId, exception);
        this.message = message;
    }

    public static SdxUpgradeDatabaseServerFailedEvent from(SdxEvent event, Exception exception, String message) {
        return new SdxUpgradeDatabaseServerFailedEvent(event.getResourceId(), event.getUserId(), exception, message);
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "SdxUpgradeDatabaseServerFailedEvent{" +
                "message='" + message + '\'' +
                "} " + super.toString();
    }
}
