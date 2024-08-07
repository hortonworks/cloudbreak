package com.sequenceiq.environment.api.v1.environment.model.base;

import java.util.Collection;

public class GeneralCollectionV4Response<T> {

    private Collection<T> responses;

    public GeneralCollectionV4Response(Collection<T> responses) {
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
        return "GeneralCollectionV4Response{" +
                "responses=" + responses +
                '}';
    }
}
