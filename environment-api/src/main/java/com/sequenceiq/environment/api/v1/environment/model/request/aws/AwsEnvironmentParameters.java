package com.sequenceiq.environment.api.v1.environment.model.request.aws;

import java.io.Serializable;

import javax.validation.Valid;

import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "AwsEnvironmentV1Parameters")
public class AwsEnvironmentParameters implements Serializable {

    @Valid
    @ApiModelProperty(EnvironmentModelDescription.S3_GUARD)
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

    @Override
    public String toString() {
        return "AwsEnvironmentParameters{" +
                "s3guard=" + s3guard +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private S3GuardRequestParameters s3guard;

        private Builder() {
        }

        public Builder withS3guard(S3GuardRequestParameters s3guard) {
            this.s3guard = s3guard;
            return this;
        }

        public AwsEnvironmentParameters build() {
            return new AwsEnvironmentParameters(this);
        }
    }
}
