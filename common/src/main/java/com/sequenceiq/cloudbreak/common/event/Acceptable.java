package com.sequenceiq.cloudbreak.common.event;

import com.fasterxml.jackson.annotation.JsonIgnore;

import reactor.rx.Promise;

public interface Acceptable extends Payload {
    @JsonIgnore
    Promise<AcceptResult> accepted();
}
