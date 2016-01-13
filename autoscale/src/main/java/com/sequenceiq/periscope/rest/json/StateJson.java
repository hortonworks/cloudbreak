package com.sequenceiq.periscope.rest.json;

import com.sequenceiq.periscope.domain.ClusterState;

public class StateJson implements Json {

    private ClusterState state;

    public ClusterState getState() {
        return state;
    }

    public void setState(ClusterState state) {
        this.state = state;
    }
}
