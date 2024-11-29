package com.sequenceiq.environment.api.v1.credential.model.parameters.gcp;

import java.io.Serializable;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "P12V1Parameters")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class P12Parameters implements Serializable {

    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String projectId;

    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "serviceaccountemailaddress@example.com")
    private String serviceAccountId;

    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String serviceAccountPrivateKey;

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getServiceAccountId() {
        return serviceAccountId;
    }

    public void setServiceAccountId(String serviceAccountId) {
        this.serviceAccountId = serviceAccountId;
    }

    public String getServiceAccountPrivateKey() {
        return serviceAccountPrivateKey;
    }

    public void setServiceAccountPrivateKey(String serviceAccountPrivateKey) {
        this.serviceAccountPrivateKey = serviceAccountPrivateKey;
    }

    @Override
    public String toString() {
        return "P12Parameters{" +
                "projectId='" + projectId + '\'' +
                ", serviceAccountId='" + serviceAccountId + '\'' +
                '}';
    }
}
