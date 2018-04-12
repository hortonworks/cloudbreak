package com.sequenceiq.cloudbreak.cloud.model.catalog;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Mpack {
    private final String mpackUrl;

    @JsonCreator
    public Mpack(
            @JsonProperty(value = "mpack_url", required = true) String mpackUrl) {
        this.mpackUrl = mpackUrl;
    }

    public String getMpackUrl() {
        return mpackUrl;
    }
}
