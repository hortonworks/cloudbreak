package com.sequenceiq.environment.api;

import java.util.ArrayList;
import java.util.Collection;

import io.swagger.v3.oas.annotations.media.Schema;

public class GeneralCollectionV1Response<T> {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Collection<T> responses = new ArrayList<>();

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
