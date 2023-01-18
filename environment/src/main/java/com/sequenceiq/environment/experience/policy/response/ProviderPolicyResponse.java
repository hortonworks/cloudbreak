package com.sequenceiq.environment.experience.policy.response;

import java.io.Serializable;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProviderPolicyResponse implements Serializable {

    @Schema(description = "The credential policy encoded in base64")
    private String policy;

    public ProviderPolicyResponse(String policy) {
        this.policy = policy;
    }

    public ProviderPolicyResponse() {
    }

    public String getPolicy() {
        return policy;
    }

    public void setPolicy(String policy) {
        this.policy = policy;
    }

    @Override
    public String toString() {
        return "ProviderPolicyResponse{" +
                "policy='" + policy + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProviderPolicyResponse)) {
            return false;
        }
        ProviderPolicyResponse that = (ProviderPolicyResponse) o;
        return getPolicy().equals(that.getPolicy());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPolicy());
    }

}
