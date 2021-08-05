package com.sequenceiq.environment.api;

import java.util.Collection;

public class GeneralCollectionV1Response<T> {

    private Collection<T> responses;

    public GeneralCollectionV1Response() {
    }

    public GeneralCollectionV1Response(Collection<T> responses) {
        this.responses = responses;
    }

    public Collection<T> getResponses() {
        return responses;
    }

    public void setResponses(Collection<T> responses) {
        this.responses = responses;
    }

    @Override
    public String toString() {
        return "GeneralCollectionV1Response{" +
                "responses=" + responses +
                '}';
    }
}
