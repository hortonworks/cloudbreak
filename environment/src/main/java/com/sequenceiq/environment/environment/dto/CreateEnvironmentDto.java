package com.sequenceiq.environment.environment.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@JsonDeserialize(builder = CreateEnvironmentDto.CreateEnvironmentDtoBuilder.class)
public class CreateEnvironmentDto {

    private EnvironmentDto environmentDto;

    private FlowIdentifier flowIdentifier;

    public static CreateEnvironmentDtoBuilder builder() {
        return new CreateEnvironmentDtoBuilder();
    }

    public FlowIdentifier getFlowIdentifier() {
        return flowIdentifier;
    }

    public void setFlowIdentifier(FlowIdentifier flowIdentifier) {
        this.flowIdentifier = flowIdentifier;
    }

    public EnvironmentDto getEnvironmentDto() {
        return environmentDto;
    }

    public void setEnvironmentDto(EnvironmentDto environmentDto) {
        this.environmentDto = environmentDto;
    }

    @Override
    public String toString() {
        return super.toString();
    }

    @JsonPOJOBuilder
    public static class CreateEnvironmentDtoBuilder {

        private FlowIdentifier flowIdentifier;

        private EnvironmentDto environmentDto;

        public CreateEnvironmentDtoBuilder withFlowIdentifier(FlowIdentifier flowIdentifier) {
            this.flowIdentifier = flowIdentifier;
            return this;
        }

        public CreateEnvironmentDtoBuilder withEnvironmentDto(EnvironmentDto environmentDto) {
            this.environmentDto = environmentDto;
            return this;
        }

        public CreateEnvironmentDto build() {
            CreateEnvironmentDto createEnvironmentDto = new CreateEnvironmentDto();
            createEnvironmentDto.setFlowIdentifier(flowIdentifier);
            createEnvironmentDto.setEnvironmentDto(environmentDto);
            return createEnvironmentDto;
        }
    }
}
