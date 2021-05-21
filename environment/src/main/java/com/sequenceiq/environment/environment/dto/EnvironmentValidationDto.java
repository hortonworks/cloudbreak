package com.sequenceiq.environment.environment.dto;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.environment.environment.validation.ValidationType;

public class EnvironmentValidationDto implements Payload {

    private EnvironmentDto environmentDto;

    private ValidationType validationType;

    public EnvironmentDto getEnvironmentDto() {
        return environmentDto;
    }

    public void setEnvironmentDto(EnvironmentDto environmentDto) {
        this.environmentDto = environmentDto;
    }

    public ValidationType getValidationType() {
        return validationType;
    }

    public void setValidationType(ValidationType validationType) {
        this.validationType = validationType;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Long getResourceId() {
        return getEnvironmentDto().getResourceId();
    }

    public static final class Builder {

        private EnvironmentDto environmentDto;

        private ValidationType validationType;

        private Builder() {
        }

        public Builder withEnvironmentDto(EnvironmentDto environmentDto) {
            this.environmentDto = environmentDto;
            return this;
        }

        public Builder withValidationType(ValidationType validationType) {
            this.validationType = validationType;
            return this;
        }

        public EnvironmentValidationDto build() {
            EnvironmentValidationDto environmentDeletionDto = new EnvironmentValidationDto();
            environmentDeletionDto.setEnvironmentDto(environmentDto);
            environmentDeletionDto.setValidationType(validationType);
            return environmentDeletionDto;
        }
    }
}
