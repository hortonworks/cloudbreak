package com.sequenceiq.it.cloudbreak.newway;

import java.util.Set;

abstract class AbstractCloudbreakEntity<R, S> extends Entity {
    private String name;

    private R request;

    private S response;

    private Set<S> responses;

    protected AbstractCloudbreakEntity(String newId) {
        super(newId);
    }

    public Set<S> getResponses() {
        return responses;
    }

    public void setResponses(Set<S> responses) {
        this.responses = responses;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public R getRequest() {
        return request;
    }

    public void setRequest(R request) {
        this.request = request;
    }

    public void setResponse(S response) {
        this.response = response;
    }

    public S getResponse() {
        return response;
    }
}
