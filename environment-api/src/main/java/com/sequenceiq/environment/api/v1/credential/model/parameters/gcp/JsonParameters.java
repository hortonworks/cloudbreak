package com.sequenceiq.environment.api.v1.credential.model.parameters.gcp;

import java.io.Serializable;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "JsonV1Parameters")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class JsonParameters implements Serializable {

    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String credentialJson;

    private String projectId;

    public String getCredentialJson() {
        return credentialJson;
    }

    public void setCredentialJson(String credentialJson) {
        this.credentialJson = credentialJson;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    @Override
    public String toString() {
        return "JsonParameters{" +
                "projectId='" + projectId + '\'' +
                '}';
    }
}
