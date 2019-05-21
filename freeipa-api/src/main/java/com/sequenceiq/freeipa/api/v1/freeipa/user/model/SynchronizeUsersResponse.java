package com.sequenceiq.freeipa.api.v1.freeipa.user.model;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.annotations.ApiModel;

@ApiModel("SynchronizeUsersV1Response")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SynchronizeUsersResponse {
    private final String value;

    public SynchronizeUsersResponse(String value) {
        this.value = requireNonNull(value);
    }

    public String getValue() {
        return value;
    }
}
