package com.sequenceiq.cloudbreak.core.flow.handlers;

public class AmbariClusterResult<R extends AmbariClusterRequest> {

    private R request;

    public AmbariClusterResult(R request) {
        this.request = request;
    }

    public static String selector(Class clazz) {
        return clazz.getSimpleName().toUpperCase();
    }

    public String selector() {
        return selector(getClass());
    }

    public R getRequest() {
        return request;
    }

    @Override
    public String toString() {
        return "CloudPlatformResult{"
                + "request=" + request
                + '}';
    }
}
