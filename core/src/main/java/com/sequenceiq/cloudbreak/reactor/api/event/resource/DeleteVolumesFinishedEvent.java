package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackDeleteVolumesRequest;
import com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.DeleteVolumesEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class DeleteVolumesFinishedEvent extends StackEvent {

    private final StackDeleteVolumesRequest stackDeleteVolumesRequest;

    @JsonCreator
    public DeleteVolumesFinishedEvent(
            @JsonProperty("stackDeleteVolumesRequest") StackDeleteVolumesRequest stackDeleteVolumesRequest) {
        super(DeleteVolumesEvent.DELETE_VOLUMES_FINISHED_EVENT.event(), stackDeleteVolumesRequest.getStackId());
        this.stackDeleteVolumesRequest = stackDeleteVolumesRequest;
    }

    public StackDeleteVolumesRequest getStackDeleteVolumesRequest() {
        return stackDeleteVolumesRequest;
    }
}
