package com.sequenceiq.cloudbreak.cloud.event.resource;

import reactor.rx.Promise;

public class StatusCheckRequest<T> {

    private Promise<T> result;

    public StatusCheckRequest(Promise<T> result) {
        this.result = result;
    }

    public Promise<T> getResult() {
        return result;
    }
}
