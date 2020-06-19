package com.sequenceiq.environment.environment.dto;

import com.sequenceiq.cloudbreak.common.event.Payload;

public class EnvironmentDeletionDto implements Payload {

    private Long id;

    private EnvironmentDto environmentDto;

    private boolean forceDelete;

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

    public boolean isForceDelete() {
        return forceDelete;
    }

    public void setEnvironmentDto(EnvironmentDto environmentDto) {
        this.environmentDto = environmentDto;
    }

    public void setForceDelete(boolean forceDelete) {
        this.forceDelete = forceDelete;
    }

    @Override
    public String toString() {
        return "EnvironmentDto{"
                + "environmentDto='" + environmentDto + '\''
                + ", forceDelete='" + forceDelete + '\''
                + '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private Long id;

        private EnvironmentDto environmentDto;

        private boolean forceDelete;

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

        public Builder withForceDelete(boolean forceDelete) {
            this.forceDelete = forceDelete;
            return this;
        }

        public EnvironmentDeletionDto build() {
            EnvironmentDeletionDto environmentDeletionDto = new EnvironmentDeletionDto();
            environmentDeletionDto.setEnvironmentDto(environmentDto);
            environmentDeletionDto.setId(id);
            environmentDeletionDto.setForceDelete(forceDelete);
            return environmentDeletionDto;
        }
    }
}
