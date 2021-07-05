package com.sequenceiq.environment.api.v1.credential.model.response;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EmptyResponse implements Serializable {
    @Override
    public String toString() {
        return "EmptyResponse{}";
    }
}
