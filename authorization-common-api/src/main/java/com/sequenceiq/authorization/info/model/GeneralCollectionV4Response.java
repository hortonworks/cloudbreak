package com.sequenceiq.authorization.info.model;

import java.util.Collection;

import io.swagger.v3.oas.annotations.media.Schema;

public class GeneralCollectionV4Response<T> {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
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
}
