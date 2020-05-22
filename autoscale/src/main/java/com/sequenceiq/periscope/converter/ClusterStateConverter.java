package com.sequenceiq.periscope.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.periscope.api.model.ClusterState;

public class ClusterStateConverter extends DefaultEnumConverter<ClusterState> {

    @Override
    public ClusterState getDefault() {
        return ClusterState.RUNNING;
    }
}
