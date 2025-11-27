package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateEvent.SALT_UPDATE_EVENT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class SaltUpdateTriggerEvent extends StackEvent {

    private final boolean skipHighstate;

    public SaltUpdateTriggerEvent(Long stackId) {
        super(SALT_UPDATE_EVENT.event(), stackId);
        this.skipHighstate = false;
    }

    public SaltUpdateTriggerEvent(Long stackId, boolean skipHighstate) {
        super(SALT_UPDATE_EVENT.event(), stackId);
        this.skipHighstate = skipHighstate;
    }

    @JsonCreator
    public SaltUpdateTriggerEvent(
            @JsonProperty("resourceId") Long stackId,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted,
            @JsonProperty("skipHighstate") boolean skipHighstate) {
        super(SALT_UPDATE_EVENT.event(), stackId, accepted);
        this.skipHighstate = skipHighstate;
    }

    @Override
    public String getSelector() {
        return SALT_UPDATE_EVENT.event();
    }

    public boolean isSkipHighstate() {
        return skipHighstate;
    }

    @Override
    public String toString() {
        return "SaltUpdateTriggerEvent{" +
                "skipHighstate=" + skipHighstate +
                "} " + super.toString();
    }
}
