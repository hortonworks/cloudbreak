package com.sequenceiq.externalizedcompute.api.model;

import java.util.Map;

import jakarta.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ExternalizedComputeClusterBase {

    @NotEmpty
    @Schema
    private String name;

    @NotEmpty
    @Schema
    private String environmentCrn;

    @Schema
    private Map<String, String> tags;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }

    @Override
    public String toString() {
        return "ExternalizedComputeClusterBase{" +
                "name='" + name + '\'' +
                ", envCrn='" + environmentCrn + '\'' +
                ", tags=" + tags +
                '}';
    }
}
