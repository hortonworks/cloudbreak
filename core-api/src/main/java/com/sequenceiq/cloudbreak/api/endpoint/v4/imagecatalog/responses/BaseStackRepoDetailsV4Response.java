package com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BaseStackRepoDetailsV4Response implements JsonEntity {

    @JsonProperty("stack")
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, String> stack = new HashMap<>();

    public Map<String, String> getStack() {
        return stack;
    }

    public void setStack(Map<String, String> stack) {
        this.stack = stack;
    }

}
