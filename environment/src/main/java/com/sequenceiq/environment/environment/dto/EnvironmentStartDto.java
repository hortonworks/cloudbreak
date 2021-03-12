package com.sequenceiq.environment.environment.dto;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.common.api.type.DataHubStartAction;

public class EnvironmentStartDto implements Payload {

    private Long id;

    private EnvironmentDto environmentDto;

    private DataHubStartAction dataHubStartAction;

    @Override
    public Long getResourceId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public EnvironmentDto getEnvironmentDto() {
        return environmentDto;
    }

    public void setEnvironmentDto(EnvironmentDto environmentDto) {
        this.environmentDto = environmentDto;
    }

    public DataHubStartAction getDataHubStart() {
        return dataHubStartAction;
    }

    public void setDataHubStart(DataHubStartAction dataHubStart) {
        this.dataHubStartAction = dataHubStart;
    }

    @Override
    public String toString() {
        return "EnvironmentDto{"
                + "environmentDto='" + environmentDto + '\''
                + ", dataHubStartAction='" + dataHubStartAction + '\''
                + '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private Long id;

        private EnvironmentDto environmentDto;

        private DataHubStartAction dataHubStartAction;

        private Builder() {
        }

        public Builder withId(Long id) {
            this.id = id;
            return this;
        }

        public Builder withEnvironmentDto(EnvironmentDto environmentDto) {
            this.environmentDto = environmentDto;
            return this;
        }

        public Builder withDataHubStart(DataHubStartAction tmpDataHubStartAction) {
            this.dataHubStartAction = tmpDataHubStartAction;
            return this;
        }

        public EnvironmentStartDto build() {
            //TODO: AF; a null check would be wise on these (environmentDto, id, dataHubStartAction)
            EnvironmentStartDto environmentDeletionDto = new EnvironmentStartDto();
            environmentDeletionDto.setEnvironmentDto(environmentDto);
            environmentDeletionDto.setId(id);
            environmentDeletionDto.setDataHubStart(dataHubStartAction);
            return environmentDeletionDto;
        }
    }
}
