package com.sequenceiq.environment.api.v1.credential.model.parameters.gcp;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "P12V1Parameters")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class P12Parameters implements Serializable {

    @NotNull
    @Schema(required = true)
    private String projectId;

    @NotNull
    @Schema(required = true, example = "serviceaccountemailaddress@example.com")
    private String serviceAccountId;

    @NotNull
    @Schema(required = true)
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
