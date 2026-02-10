package com.sequenceiq.cloudbreak.core.flow2.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class UpdateSslConfigTriggerEvent extends StackEvent {

    private final String encryptionProfileCrn;

    public UpdateSslConfigTriggerEvent(String selector, Long stackId) {
        super(selector, stackId);
        this.encryptionProfileCrn = null;
    }

    @JsonCreator
    public UpdateSslConfigTriggerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted,
            @JsonProperty("encryptionProfileCrn") String encryptionProfileCrn) {
        super(selector, stackId, accepted);
        this.encryptionProfileCrn = encryptionProfileCrn;
    }

    public String getEncryptionProfileCrn() {
        return encryptionProfileCrn;
    }

    @Override
    public boolean equalsEvent(StackEvent other) {
        return isClassAndEqualsEvent(UpdateSslConfigTriggerEvent.class, other);
    }
}
