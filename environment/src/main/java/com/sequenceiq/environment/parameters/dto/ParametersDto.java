package com.sequenceiq.environment.parameters.dto;

public class ParametersDto {

    private Long id;

    private final String name;

    private final String accountId;

    private final AwsParametersDto awsParametersDto;

    private ParametersDto(Builder builder) {
        id = builder.id;
        name = builder.name;
        accountId = builder.accountId;
        awsParametersDto = builder.awsParametersDto;
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

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Long id;

        private String name;

        private String accountId;

        private AwsParametersDto awsParametersDto;

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

        public ParametersDto build() {
            return new ParametersDto(this);
        }
    }
}
