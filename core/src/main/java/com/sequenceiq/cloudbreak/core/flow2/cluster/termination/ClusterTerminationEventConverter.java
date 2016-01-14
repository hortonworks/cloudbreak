package com.sequenceiq.cloudbreak.core.flow2.cluster.termination;


import com.sequenceiq.cloudbreak.core.flow2.EventConverter;

public class ClusterTerminationEventConverter implements EventConverter<ClusterTerminationEvent> {

    @Override
    public ClusterTerminationEvent convert(String key) {
        return ClusterTerminationEvent.fromString(key);
    }
}
