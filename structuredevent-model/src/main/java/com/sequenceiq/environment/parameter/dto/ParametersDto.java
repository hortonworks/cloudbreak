package com.sequenceiq.environment.parameter.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = ParametersDto.Builder.class)
public class ParametersDto {

    private Long id;

    private final String name;

    private final String accountId;

    private final AwsParametersDto awsParametersDto;

    private final AzureParametersDto azureParametersDto;

    private final GcpParametersDto gcpParametersDto;

    private final YarnParametersDto yarnParametersDto;

    private ParametersDto(Builder builder) {
        id = builder.id;
        name = builder.name;
        accountId = builder.accountId;
        awsParametersDto = builder.awsParametersDto;
        azureParametersDto = builder.azureParametersDto;
        yarnParametersDto = builder.yarnParametersDto;
        gcpParametersDto = builder.gcpParametersDto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getAccountId() {
        return accountId;
    }

    public AwsParametersDto getAwsParametersDto() {
        return awsParametersDto;
    }

    public AzureParametersDto getAzureParametersDto() {
        return azureParametersDto;
    }

    public AzureParametersDto azureParametersDto() {
        return azureParametersDto;
    }

    public GcpParametersDto getGcpParametersDto() {
        return gcpParametersDto;
    }

    public YarnParametersDto getYarnParametersDto() {
        return yarnParametersDto;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "ParametersDto{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", accountId='" + accountId + '\'' +
                ", awsParametersDto=" + awsParametersDto +
                ", azureParametersDto=" + azureParametersDto +
                '}';
    }

    @JsonPOJOBuilder
    public static final class Builder {
        private Long id;

        private String name;

        private String accountId;

        private AwsParametersDto awsParametersDto;

        private AzureParametersDto azureParametersDto;

        private GcpParametersDto gcpParametersDto;

        private YarnParametersDto yarnParametersDto;

        public Builder withId(Long id) {
            this.id = id;
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withAccountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder withAwsParametersDto(AwsParametersDto awsParametersDto) {
            this.awsParametersDto = awsParametersDto;
            return this;
        }

        public Builder withAzureParametersDto(AzureParametersDto azureParametersDto) {
            this.azureParametersDto = azureParametersDto;
            return this;
        }

        public Builder withGcpParametersDto(GcpParametersDto gcpParametersDto) {
            this.gcpParametersDto = gcpParametersDto;
            return this;
        }

        public Builder withYarnParametersDto(YarnParametersDto yarnParametersDto) {
            this.yarnParametersDto = yarnParametersDto;
            return this;
        }

        public ParametersDto build() {
            return new ParametersDto(this);
        }
    }
}
