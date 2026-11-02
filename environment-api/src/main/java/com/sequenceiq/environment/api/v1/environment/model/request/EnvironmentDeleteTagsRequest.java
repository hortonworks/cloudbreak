package com.sequenceiq.environment.api.v1.environment.model.request;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "EnvironmentDeleteTagsV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnvironmentDeleteTagsRequest implements Serializable {

    @NotEmpty
    @Size(max = 64)
    @Schema(description = "User-defined tag keys to remove from the environment and cascade to child resources.")
    private Set<@NotBlank @Size(max = 512) String> tagKeys = new HashSet<>();

    public Set<String> getTagKeys() {
        return tagKeys;
    }

    public void setTagKeys(Set<String> tagKeys) {
        this.tagKeys = tagKeys;
    }

    @Override
    public String toString() {
        return "EnvironmentDeleteTagsRequest{"
                + "tagKeys=" + tagKeys
                + '}';
    }
}
