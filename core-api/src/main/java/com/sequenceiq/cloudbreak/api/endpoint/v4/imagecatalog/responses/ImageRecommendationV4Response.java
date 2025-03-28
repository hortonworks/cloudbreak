package com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImageRecommendationV4Response implements JsonEntity {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean hasValidationError;

    private String validationMessage;

    public boolean hasValidationError() {
        return hasValidationError;
    }

    public void setHasValidationError(boolean hasValidationError) {
        this.hasValidationError = hasValidationError;
    }

    public String getValidationMessage() {
        return validationMessage;
    }

    public void setValidationMessage(String validationMessage) {
        this.validationMessage = validationMessage;
    }

    @Override
    public String toString() {
        return "ImageRecommendationV4Response{" +
                "validationResult=" + hasValidationError +
                ", validationMessage='" + validationMessage + '\'' +
                '}';
    }
}
