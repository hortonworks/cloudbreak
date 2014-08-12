package com.sequenceiq.periscope.rest.json;

import com.sequenceiq.periscope.registry.ClusterState;

public class StateJson implements Json {

    private ClusterState state;

    public ClusterState getState() {
        return state;
    }

    public void setState(ClusterState state) {
        this.state = state;
    }
}
