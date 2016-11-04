package com.sequenceiq.cloudbreak.cloud;

import com.sequenceiq.cloudbreak.cloud.event.Payload;

import reactor.rx.Promise;

public interface Acceptable extends Payload {
    Promise<Boolean> accepted();
}
