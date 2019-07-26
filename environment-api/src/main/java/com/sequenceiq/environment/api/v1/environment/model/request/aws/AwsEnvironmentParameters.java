package com.sequenceiq.environment.api.v1.environment.model.request.aws;

import io.swagger.annotations.ApiModel;

@ApiModel(value = "AwsEnvironmentV1Parameters")
public class AwsEnvironmentParameters {

    private S3GuardRequestParameters s3guard;

    public AwsEnvironmentParameters() {
    }

    private AwsEnvironmentParameters(Builder builder) {
        this.s3guard = builder.s3guard;
    }

    public S3GuardRequestParameters getS3guard() {
        return s3guard;
    }

    public void setS3guard(S3GuardRequestParameters s3guard) {
        this.s3guard = s3guard;
    }

    public static Builder awsEnvironmentParameters() {
        return new Builder();
    }

    public static final class Builder {
        private S3GuardRequestParameters s3guard;

        public Builder withS3guard(final S3GuardRequestParameters s3guard) {
            this.s3guard = s3guard;
            return this;
        }

        public AwsEnvironmentParameters build() {
            return new AwsEnvironmentParameters(this);
        }
    }
}
