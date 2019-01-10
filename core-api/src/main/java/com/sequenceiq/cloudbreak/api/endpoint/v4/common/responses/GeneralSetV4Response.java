package com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses;

import java.util.Set;

import com.google.common.collect.Sets;

public class GeneralSetV4Response<T> {

    private Set<T> responses = Sets.newHashSet();

    public Set<T> getResponses() {
        return responses;
    }

    public void setResponses(Set<T> responses) {
        this.responses = responses;
    }

    public static <T> GeneralSetV4Response<T> propagateResponses(Set<T> responses) {
        GeneralSetV4Response<T> v4ResponsesBase = new GeneralSetV4Response<>();
        v4ResponsesBase.setResponses(responses);
        return v4ResponsesBase;
    }
}
