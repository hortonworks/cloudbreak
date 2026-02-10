package com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.UpdateSslConfigsOnClusterStateSelectors;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class UpdateSslConfigFailedEvent extends StackFailureEvent {

    public UpdateSslConfigFailedEvent(Long stackId, Exception exception) {
        this(UpdateSslConfigsOnClusterStateSelectors.FAILED_UPDATE_SSL_CONFIGS_ON_CLUSTER_EVENT.name(), stackId, exception);
    }

    @JsonCreator
    public UpdateSslConfigFailedEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("exception") Exception exception) {
        super(selector, stackId, exception);
    }
}