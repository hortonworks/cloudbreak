package com.sequenceiq.cloudbreak.cloud.template.compute;

public class ResourceRequestResult<T> {

    private final FutureResult status;
    private final T result;

    public ResourceRequestResult(FutureResult status, T result) {
        this.status = status;
        this.result = result;
    }

    public FutureResult getStatus() {
        return status;
    }

    public T getResult() {
        return result;
    }
}
