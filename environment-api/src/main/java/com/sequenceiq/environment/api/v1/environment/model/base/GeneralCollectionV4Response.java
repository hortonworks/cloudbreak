package com.sequenceiq.environment.api.v1.environment.model.base;

import java.util.ArrayList;
import java.util.Collection;

import io.swagger.v3.oas.annotations.media.Schema;

public class GeneralCollectionV4Response<T> {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Collection<T> responses = new ArrayList<>();

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
