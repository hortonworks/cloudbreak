package com.sequenceiq.periscope.api.model;

import com.sequenceiq.periscope.doc.ApiDescription.StateJsonProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public class StateJson implements Json {

    @Schema(description = StateJsonProperties.STATE)
    private ClusterState state;

    public ClusterState getState() {
        return state;
    }

    public void setState(ClusterState state) {
        this.state = state;
    }

    public static StateJson running() {
        StateJson stateJson = new StateJson();
        stateJson.state = ClusterState.RUNNING;
        return stateJson;
    }

    public static StateJson suspended() {
        StateJson stateJson = new StateJson();
        stateJson.state = ClusterState.SUSPENDED;
        return stateJson;
    }
}
