package com.sequenceiq.common.api.telemetry.model;

import java.io.Serializable;

import jakarta.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnonymizationRule implements Serializable {

    @NotBlank
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String value;

    private String replacement = "[REDACTED]";

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getReplacement() {
        return replacement;
    }

    public void setReplacement(String replacement) {
        this.replacement = replacement;
    }

    @Override
    public String toString() {
        return "AnonymizationRule{" +
                "value='" + value + '\'' +
                ", replacement='" + replacement + '\'' +
                '}';
    }
}
