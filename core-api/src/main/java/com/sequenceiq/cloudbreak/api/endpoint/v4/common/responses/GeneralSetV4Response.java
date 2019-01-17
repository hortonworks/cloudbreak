package com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses;

import java.util.Set;

public class GeneralSetV4Response<T> {

    private Set<T> responses;

    public GeneralSetV4Response(Set<T> responses) {
        this.responses = responses;
    }

    public Set<T> getResponses() {
        return responses;
    }

    public void setResponses(Set<T> responses) {
        this.responses = responses;
    }
}
