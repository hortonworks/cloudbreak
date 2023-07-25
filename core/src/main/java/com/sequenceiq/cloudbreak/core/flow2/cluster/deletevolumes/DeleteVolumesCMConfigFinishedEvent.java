package com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.DeleteVolumesEvent.DELETE_VOLUMES_CM_CONFIG_FINISHED_EVENT;

import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class DeleteVolumesCMConfigFinishedEvent extends StackEvent {

    private final String requestGroup;

    @JsonCreator
    public DeleteVolumesCMConfigFinishedEvent(@JsonProperty("resourceId") Long resourceId,
            @JsonProperty("requestGroup") String requestGroup) {
        super(DELETE_VOLUMES_CM_CONFIG_FINISHED_EVENT.event(), resourceId);
        this.requestGroup = requestGroup;
    }

    public String getRequestGroup() {
        return requestGroup;
    }

    @Override
    public String toString() {
        return new StringJoiner(",", DeleteVolumesCMConfigFinishedEvent.class.getSimpleName() + "[", "]")
                .add("RequestGroup=" + requestGroup).toString();
    }
}
