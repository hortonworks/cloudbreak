package com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.DeleteVolumesEvent.DELETE_VOLUMES_CM_CONFIG_HANDLER_EVENT;

import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class DeleteVolumesCMConfigEvent extends StackEvent {

    private final String requestGroup;

    @JsonCreator
    public DeleteVolumesCMConfigEvent(@JsonProperty("resourceId") Long resourceId,
            @JsonProperty("requestGroup") String requestGroup) {
        super(DELETE_VOLUMES_CM_CONFIG_HANDLER_EVENT.event(), resourceId);
        this.requestGroup = requestGroup;
    }

    public String getRequestGroup() {
        return requestGroup;
    }

    @Override
    public String toString() {
        return new StringJoiner(",", DeleteVolumesCMConfigEvent.class.getSimpleName() + "[", "]")
                .add("RequestGroup=" + requestGroup).toString();
    }
}
