package com.sequenceiq.environment.environment.dto.aws;

public class AwsEnvironmentParamsDto {

    private S3GuardParamsDto s3guard;

    public AwsEnvironmentParamsDto() {
    }

    private AwsEnvironmentParamsDto(Builder builder) {
        this.s3guard = builder.s3guard;
    }

    public static Builder aAwsEnvironmentParamsBuilder() {
        return new Builder();
    }

    public S3GuardParamsDto getS3guard() {
        return s3guard;
    }

    public static final class Builder {
        private S3GuardParamsDto s3guard;

        public Builder withS3guard(final S3GuardParamsDto s3guard) {
            this.s3guard = s3guard;
            return this;
        }

        public AwsEnvironmentParamsDto build() {
            return new AwsEnvironmentParamsDto(this);
        }
    }
}
