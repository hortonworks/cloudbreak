package com.sequenceiq.environment.api.v1.environment.model.request.aws;

import java.io.Serializable;

import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "UpdateAwsDiskEncryptionParametersV1Request")
public class UpdateAwsDiskEncryptionParametersRequest implements Serializable {

    @ApiModelProperty(EnvironmentModelDescription.AWS_DISK_ENCRYPTION_PARAMETERS)
    private AwsDiskEncryptionParameters awsDiskEncryptionParameters;

    public AwsDiskEncryptionParameters getAwsDiskEncryptionParameters() {
        return awsDiskEncryptionParameters;
    }

    public void setAwsDiskEncryptionParameters(AwsDiskEncryptionParameters awsDiskEncryptionParameters) {
        this.awsDiskEncryptionParameters = awsDiskEncryptionParameters;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "UpdateAwsDiskEncryptionParametersRequest{" +
                "AwsDiskEncryptionParameters='" + awsDiskEncryptionParameters + '\'' +
                '}';
    }

    public static class Builder {
        private AwsDiskEncryptionParameters awsDiskEncryptionParameters;

        private Builder() {
        }

        public Builder withAwsDiskEncryptionParameters(AwsDiskEncryptionParameters awsDiskEncryptionParameters) {
            this.awsDiskEncryptionParameters = awsDiskEncryptionParameters;
            return this;
        }

        public UpdateAwsDiskEncryptionParametersRequest build() {
            UpdateAwsDiskEncryptionParametersRequest updateAwsDiskEncryptionParametersRequest = new UpdateAwsDiskEncryptionParametersRequest();
            updateAwsDiskEncryptionParametersRequest.setAwsDiskEncryptionParameters(awsDiskEncryptionParameters);
            return updateAwsDiskEncryptionParametersRequest;
        }
    }
}
