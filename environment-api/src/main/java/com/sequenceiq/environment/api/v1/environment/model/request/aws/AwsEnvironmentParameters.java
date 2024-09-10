package com.sequenceiq.environment.api.v1.environment.model.request.aws;

import java.io.Serializable;

import jakarta.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AwsEnvironmentV1Parameters")
@JsonIgnoreProperties(ignoreUnknown = true)
public class AwsEnvironmentParameters implements Serializable {

    @Deprecated
    @Valid
    @Schema(description = EnvironmentModelDescription.S3_GUARD)
    private S3GuardRequestParameters s3guard;

    @Valid
    @Schema(description = EnvironmentModelDescription.AWS_DISK_ENCRYPTION_PARAMETERS)
    private AwsDiskEncryptionParameters awsDiskEncryptionParameters;

    public AwsEnvironmentParameters() {
    }

    private AwsEnvironmentParameters(Builder builder) {
        this.s3guard = builder.s3guard;
        this.awsDiskEncryptionParameters = builder.awsDiskEncryptionParameters;
    }

    @Deprecated
    public S3GuardRequestParameters getS3guard() {
        return s3guard;
    }

    public AwsDiskEncryptionParameters getAwsDiskEncryptionParameters() {
        return awsDiskEncryptionParameters;
    }

    @Deprecated
    public void setS3guard(S3GuardRequestParameters s3guard) {
        this.s3guard = s3guard;
    }

    public void setAwsDiskEncryptionParameters(AwsDiskEncryptionParameters awsDiskEncryptionParameters) {
        this.awsDiskEncryptionParameters = awsDiskEncryptionParameters;
    }

    @Override
    public String toString() {
        return "AwsEnvironmentParameters{" +
                "s3guard=" + s3guard +
                ", awsDiskEncryptionParameters=" + awsDiskEncryptionParameters +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private S3GuardRequestParameters s3guard;

        private AwsDiskEncryptionParameters awsDiskEncryptionParameters;

        private Builder() {
        }

        public Builder withS3guard(S3GuardRequestParameters s3guard) {
            this.s3guard = s3guard;
            return this;
        }

        public Builder withAwsDiskEncryptionParameters(AwsDiskEncryptionParameters awsDiskEncryptionParameters) {
            this.awsDiskEncryptionParameters = awsDiskEncryptionParameters;
            return this;
        }

        public AwsEnvironmentParameters build() {
            return new AwsEnvironmentParameters(this);
        }
    }
}
