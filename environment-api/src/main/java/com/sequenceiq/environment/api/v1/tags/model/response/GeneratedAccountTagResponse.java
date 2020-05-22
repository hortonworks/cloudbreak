package com.sequenceiq.environment.api.v1.tags.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.environment.api.v1.tags.model.AccountTagBase;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GeneratedAccountTagResponse extends AccountTagBase {

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
