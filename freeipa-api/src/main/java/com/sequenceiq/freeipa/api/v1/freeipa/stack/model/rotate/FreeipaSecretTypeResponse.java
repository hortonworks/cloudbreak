package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.rotate;

import java.util.function.Function;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.rotation.response.BaseSecretTypeResponse;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "FreeipaSecretTypeResponse")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FreeipaSecretTypeResponse extends BaseSecretTypeResponse {

    public FreeipaSecretTypeResponse() {
    }

    public FreeipaSecretTypeResponse(String secretType, String displayName, String description, Long lastUpdated) {
        super(secretType, displayName, description, lastUpdated);
    }

    public static Function<BaseSecretTypeResponse, FreeipaSecretTypeResponse> converter() {
        return secretTypeResponse ->  new FreeipaSecretTypeResponse(secretTypeResponse.getSecretType(),
                secretTypeResponse.getDisplayName(), secretTypeResponse.getDescription(), secretTypeResponse.getLastUpdated());
    }

    @Override
    public String toString() {
        return "FreeipaSecretTypeResponse{" +
                "secretType='" + getSecretType() + '\'' +
                ", displayName='" + getDisplayName() + '\'' +
                ", description='" + getDescription() + '\'' +
                ", lastUpdated='" + getLastUpdated() + '\'' +
                '}';
    }
}
