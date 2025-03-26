package com.sequenceiq.sdx.api.model;

import java.util.function.Function;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.rotation.response.BaseSecretTypeResponse;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxSecretTypeResponse extends BaseSecretTypeResponse {

    public SdxSecretTypeResponse() {
    }

    public SdxSecretTypeResponse(String secretType, String displayName, String description, Long lastUpdated) {
        super(secretType, displayName, description, lastUpdated);
    }

    public static Function<BaseSecretTypeResponse, SdxSecretTypeResponse> converter() {
        return secretTypeResponse ->  new SdxSecretTypeResponse(secretTypeResponse.getSecretType(),
                secretTypeResponse.getDisplayName(), secretTypeResponse.getDescription(), secretTypeResponse.getLastUpdated());
    }

    @Override
    public String toString() {
        return "SdxSecretTypeResponse{" +
                "secretType='" + getSecretType() + '\'' +
                ", displayName='" + getDisplayName() + '\'' +
                ", description='" + getDescription() + '\'' +
                ", lastUpdated='" + getLastUpdated() + '\'' +
                '}';
    }
}
