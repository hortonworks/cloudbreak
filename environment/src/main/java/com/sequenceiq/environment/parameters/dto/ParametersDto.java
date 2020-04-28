package com.sequenceiq.environment.parameters.dto;

public class ParametersDto {

    private Long id;

    private final String name;

    private final String accountId;

    private final AwsParametersDto awsParametersDto;

    private final AzureParametersDto azureParametersDto;

    private ParametersDto(Builder builder) {
        id = builder.id;
        name = builder.name;
        accountId = builder.accountId;
        awsParametersDto = builder.awsParametersDto;
        azureParametersDto = builder.azureParametersDto;
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

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Long id;

        private String name;

        private String accountId;

        private AwsParametersDto awsParametersDto;

        private AzureParametersDto azureParametersDto;

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

        public Builder withAwsParameters(AwsParametersDto awsParametersDto) {
            this.awsParametersDto = awsParametersDto;
            return this;
        }

        public Builder withAzureParameters(AzureParametersDto azureParametersDto) {
            this.azureParametersDto = azureParametersDto;
            return this;
        }

        public ParametersDto build() {
            return new ParametersDto(this);
        }
    }
}
