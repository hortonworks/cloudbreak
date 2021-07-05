package com.sequenceiq.environment.api.v1.tags.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.environment.api.v1.tags.model.AccountTagBase;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountTagRequest extends AccountTagBase {

    @Override
    public String toString() {
        return super.toString() + ", " + "AccountTagRequest{}";
    }
}
