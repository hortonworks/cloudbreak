package com.sequenceiq.environment.api.v1.platformresource.model;

import java.util.HashMap;
import java.util.Map;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.environment.api.v1.platformresource.PlatformResourceModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@NotNull
@JsonIgnoreProperties(ignoreUnknown = true)
public class TagSpecificationsResponse {

    @Schema(description = PlatformResourceModelDescription.TAG_SPECIFICATIONS, requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, Map<String, Object>> specifications = new HashMap<>();

    public Map<String, Map<String, Object>> getSpecifications() {
        return specifications;
    }

    public void setSpecifications(Map<String, Map<String, Object>> specifications) {
        this.specifications = specifications;
    }

    @Override
    public String toString() {
        return "TagSpecificationsResponse{" +
                "specifications=" + specifications +
                '}';
    }
}
