package com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses;


import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SupportedVersionsV4Response implements JsonEntity {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Set<SupportedVersionV4Response> supportedVersions = new HashSet<>();

    public Set<SupportedVersionV4Response> getSupportedVersions() {
        return supportedVersions;
    }

    public void setSupportedVersions(Set<SupportedVersionV4Response> supportedVersions) {
        this.supportedVersions = supportedVersions;
    }
}
