package com.sequenceiq.cloudbreak.common.event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sequenceiq.cloudbreak.eventbus.Promise;

public interface Acceptable extends Payload {
    @JsonIgnore
    Promise<AcceptResult> accepted();
}
