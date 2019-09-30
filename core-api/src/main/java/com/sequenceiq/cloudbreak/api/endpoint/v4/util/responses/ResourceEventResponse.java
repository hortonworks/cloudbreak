package com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.event.ResourceEvent;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResourceEventResponse {

    @ApiModelProperty(ModelDescriptions.StackModelDescription.EVENTS)
    private ResourceEvent event;

    public ResourceEvent getEvent() {
        return event;
    }

    public void setEvent(ResourceEvent event) {
        this.event = event;
    }

}
