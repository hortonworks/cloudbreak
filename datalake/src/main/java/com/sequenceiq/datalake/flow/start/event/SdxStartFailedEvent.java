package com.sequenceiq.datalake.flow.start.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.SdxFailedEvent;

public class SdxStartFailedEvent extends SdxFailedEvent {

    private final boolean includeExceptionDetailsInNotification;

    public SdxStartFailedEvent(Long sdxId, String userId, Exception exception) {
        this(sdxId, userId, exception, false);
    }

    @JsonCreator
    public SdxStartFailedEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("exception") Exception exception,
            @JsonProperty("includeExceptionDetailsInNotification") boolean includeExceptionDetailsInNotification) {
        super(sdxId, userId, exception);
        this.includeExceptionDetailsInNotification = includeExceptionDetailsInNotification;
    }

    public static SdxStartFailedEvent from(SdxEvent event, Exception exception) {
        return new SdxStartFailedEvent(event.getResourceId(), event.getUserId(), exception, false);
    }

    @Override
    public String selector() {
        return "SdxStartFailedEvent";
    }

    public boolean isIncludeExceptionDetailsInNotification() {
        return includeExceptionDetailsInNotification;
    }
}
