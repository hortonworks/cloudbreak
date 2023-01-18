package com.sequenceiq.environment.api.v1.environment.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PolicyValidationErrorResponse")
@JsonIgnoreProperties(ignoreUnknown = true)
public class PolicyValidationErrorResponse {

    @Schema(description = EnvironmentModelDescription.POLICY_VALIDATION_ERROR_SERVICE)
    private String service;

    @Schema(description = EnvironmentModelDescription.POLICY_VALIDATION_ERROR_MESSAGE)
    private String message;

    @Schema(description = EnvironmentModelDescription.POLICY_VALIDATION_ERROR_CODE)
    private Integer code;

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return "PolicyValidationErrorResponse{" +
                "service='" + service + '\'' +
                ", message='" + message + '\'' +
                ", code='" + code + '\'' +
                '}';
    }
}
