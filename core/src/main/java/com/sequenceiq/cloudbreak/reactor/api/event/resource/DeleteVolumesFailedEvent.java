package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.DeleteVolumesEvent.DELETE_VOLUMES_FAIL_HANDLED_EVENT;

import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class DeleteVolumesFailedEvent extends StackFailureEvent {

    private final ResourceStatus resourceStatus;

    private String statusReason;

    @JsonCreator
    public DeleteVolumesFailedEvent(@JsonProperty("statusReason") String statusReason,
            @JsonProperty("exception") Exception exception,
            @JsonProperty("resourceId") Long resourceId) {
        super(DELETE_VOLUMES_FAIL_HANDLED_EVENT.event(), resourceId, exception);
        this.statusReason = statusReason;
        this.resourceStatus = ResourceStatus.FAILED;
    }

    public ResourceStatus getResourceStatus() {
        return resourceStatus;
    }

    public boolean isFailed() {
        return resourceStatus == ResourceStatus.FAILED;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public String toString() {
        return new StringJoiner(", ", DeleteVolumesFailedEvent.class.getSimpleName() + "[", "]")
                .add("resourceStatus=" + resourceStatus)
                .add("statusReason=" + statusReason)
                .toString();
    }
}
