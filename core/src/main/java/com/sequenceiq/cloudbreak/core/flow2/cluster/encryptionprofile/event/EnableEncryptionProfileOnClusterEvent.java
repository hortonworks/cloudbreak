package com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.event;

import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class EnableEncryptionProfileOnClusterEvent extends StackEvent {

    private final String encryptionProfileCrn;

    public EnableEncryptionProfileOnClusterEvent(String selector, Long resourceId, String encryptionProfileCrn) {
        super(selector, resourceId);
        this.encryptionProfileCrn = encryptionProfileCrn;
    }

    @JsonCreator
    public EnableEncryptionProfileOnClusterEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted,
            @JsonProperty("encryptionProfileCrn") String encryptionProfileCrn) {
        super(selector, resourceId, accepted);
        this.encryptionProfileCrn = encryptionProfileCrn;
    }

    public String getEncryptionProfileCrn() {
        return encryptionProfileCrn;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", EnableEncryptionProfileOnClusterEvent.class.getSimpleName() + "[", "]")
                .add("selector=" + getSelector())
                .add("stackId=" + getResourceId())
                .add("encryptionProfileCrn=" + encryptionProfileCrn)
                .toString();
    }
}
