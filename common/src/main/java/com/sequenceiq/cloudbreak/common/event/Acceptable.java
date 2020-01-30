package com.sequenceiq.cloudbreak.common.event;

import reactor.rx.Promise;

public interface Acceptable extends Payload {
    Promise<AcceptResult> accepted();
}
