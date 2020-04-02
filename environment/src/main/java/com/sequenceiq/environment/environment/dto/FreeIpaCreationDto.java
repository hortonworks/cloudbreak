package com.sequenceiq.environment.environment.dto;

public class FreeIpaCreationDto {

    private boolean create = true;

    private int instanceCountByGroup = 1;

    private FreeIpaCreationAwsParametersDto aws;

    private FreeIpaCreationDto() {
    }

    public void setCreate(boolean create) {
        this.create = create;
    }

    public boolean getCreate() {
        return create;
    }

    public void setInstanceCountByGroup(int instanceCountByGroup) {
        this.instanceCountByGroup = instanceCountByGroup;
    }

    public int getInstanceCountByGroup() {
        return instanceCountByGroup;
    }

    public FreeIpaCreationAwsParametersDto getAws() {
        return aws;
    }

    @Override
    public String toString() {
        return "FreeIpaCreationDto{" +
            "create='" + create + '\'' +
            "instanceCountByGroup='" + instanceCountByGroup + '\'' +
            "aws='" + aws + '\'' +
            '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private boolean create = true;

        private int instanceCountByGroup = 1;

        private FreeIpaCreationAwsParametersDto aws;

        private Builder() {
        }

        public Builder withCreate(boolean create) {
            this.create = create;
            return this;
        }

        public Builder withInstanceCountByGroup(int instanceCountByGroup) {
            this.instanceCountByGroup = instanceCountByGroup;
            return this;
        }

        public Builder withAws(FreeIpaCreationAwsParametersDto aws) {
            this.aws = aws;
            return this;
        }

        public FreeIpaCreationDto build() {
            FreeIpaCreationDto response = new FreeIpaCreationDto();
            response.create = create;
            response.instanceCountByGroup = instanceCountByGroup;
            response.aws = aws;
            return response;
        }
    }
}
