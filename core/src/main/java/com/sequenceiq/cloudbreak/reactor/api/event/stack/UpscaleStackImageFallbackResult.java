package com.sequenceiq.cloudbreak.reactor.api.event.stack;

import java.util.List;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;

public class UpscaleStackImageFallbackResult extends UpscaleStackResult {

    private final String notificationMessage;

    @JsonCreator
    public UpscaleStackImageFallbackResult(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("resourceStatus") ResourceStatus resourceStatus,
            @JsonProperty("results") List<CloudResourceStatus> results,
            @JsonProperty("notificationMessage") String notificationMessage) {
        super(resourceId, resourceStatus, results);
        this.notificationMessage = notificationMessage;
    }

    public String getNotificationMessage() {
        return notificationMessage;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", UpscaleStackImageFallbackResult.class.getSimpleName() + "[", "]")
                .add("resourceStatus=" + getResourceStatus())
                .add("results=" + getResults())
                .add("notificationMessage=" + getNotificationMessage())
                .toString();
    }
}