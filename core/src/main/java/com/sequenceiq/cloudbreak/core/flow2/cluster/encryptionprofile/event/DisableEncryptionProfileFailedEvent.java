package com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.DisableEncryptionProfileOnClusterStateSelectors;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class DisableEncryptionProfileFailedEvent extends StackFailureEvent {

    public DisableEncryptionProfileFailedEvent(Long stackId, Exception exception) {
        this(DisableEncryptionProfileOnClusterStateSelectors.FAILED_DISABLE_ENCRYPTION_PROFILE_ON_CLUSTER_EVENT.name(), stackId, exception);
    }

    @JsonCreator
    public DisableEncryptionProfileFailedEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("exception") Exception exception) {
        super(selector, stackId, exception);
    }
}
