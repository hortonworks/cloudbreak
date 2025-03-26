package com.sequenceiq.distrox.api.v1.distrox.model;

import java.util.function.Function;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.rotation.response.BaseSecretTypeResponse;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DistroXSecretTypeResponse extends BaseSecretTypeResponse {

    public DistroXSecretTypeResponse() {
    }

    public DistroXSecretTypeResponse(String secretType, String displayName, String description, Long lastUpdated) {
        super(secretType, displayName, description, lastUpdated);
    }

    public static Function<BaseSecretTypeResponse, DistroXSecretTypeResponse> converter() {
        return secretTypeResponse ->  new DistroXSecretTypeResponse(secretTypeResponse.getSecretType(),
                secretTypeResponse.getDisplayName(), secretTypeResponse.getDescription(), secretTypeResponse.getLastUpdated());
    }

    @Override
    public String toString() {
        return "DistroXSecretTypeResponse{" +
                "secretType='" + getSecretType() + '\'' +
                ", displayName='" + getDisplayName() + '\'' +
                ", description='" + getDescription() + '\'' +
                ", lastUpdated='" + getLastUpdated() + '\'' +
                '}';
    }
}
