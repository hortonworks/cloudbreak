package com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.event;

import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class UpdateSslConfigEvent extends StackEvent {

    private final String encryptionProfileCrn;

    @JsonCreator
    public UpdateSslConfigEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("encryptionProfileCrn") String encryptionProfileCrn) {
        super(selector, resourceId);
        this.encryptionProfileCrn = encryptionProfileCrn;
    }

    public String getEncryptionProfileCrn() {
        return encryptionProfileCrn;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", UpdateSslConfigEvent.class.getSimpleName() + "[", "]")
                .add("selector=" + getSelector())
                .add("stackId=" + getResourceId())
                .add("encryptionProfileCrn=" + encryptionProfileCrn)
                .toString();
    }
}
