package com.sequenceiq.periscope.api.model;

import com.sequenceiq.periscope.doc.ApiDescription.StateJsonProperties;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("ClusterState")
public class StateJson implements Json {

    @ApiModelProperty(StateJsonProperties.STATE)
    private ClusterState state;

    public ClusterState getState() {
        return state;
    }

    public void setState(ClusterState state) {
        this.state = state;
    }
}
