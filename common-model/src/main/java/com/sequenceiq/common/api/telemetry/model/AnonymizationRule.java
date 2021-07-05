package com.sequenceiq.common.api.telemetry.model;

import java.io.Serializable;

import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnonymizationRule implements Serializable {

    @NotBlank
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
