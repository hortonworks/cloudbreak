package com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.EnableEncryptionProfileOnClusterStateSelectors;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class EnableEncryptionProfileFailedEvent extends StackFailureEvent {

    public EnableEncryptionProfileFailedEvent(Long stackId, Exception exception) {
        this(EnableEncryptionProfileOnClusterStateSelectors.FAILED_ENABLE_ENCRYPTION_PROFILE_ON_CLUSTER_EVENT.name(), stackId, exception);
    }

    @JsonCreator
    public EnableEncryptionProfileFailedEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("exception") Exception exception) {
        super(selector, stackId, exception);
    }
}
