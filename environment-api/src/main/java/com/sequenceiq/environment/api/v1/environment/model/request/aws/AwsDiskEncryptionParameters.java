package com.sequenceiq.environment.api.v1.environment.model.request.aws;

import java.io.Serializable;

import javax.validation.constraints.Pattern;
import com.google.common.annotations.VisibleForTesting;

import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "AwsDiskEncryptionV1Parameters")
public class AwsDiskEncryptionParameters implements Serializable {

    @VisibleForTesting
    static final String AWS_ENCRYPTION_KEY_INVALID_MESSAGE = "The identifier of the AWS Key Management Service (AWS KMS) " +
            "customer master key (CMK) to use for Amazon EBS encryption.\n" +
            "You can specify the key ARN in the below format:\n" +
            "Key ARN: arn:partition:service:region:account-id:resource-type/resource-id. " +
            "For example, arn:[aws|aws-us-gov]:kms:us-east-1:012345678910:key/1234abcd-12ab-34cd-56ef-1234567890ab.\n";

    @VisibleForTesting
    static final String REG_EXP_FOR_ENCRYPTION_KEY_ARN =
            "^arn:(aws|aws-cn|aws-us-gov):kms:[a-zA-Z0-9-]+:[0-9]+:key/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";

    @Pattern(regexp = REG_EXP_FOR_ENCRYPTION_KEY_ARN,
            message = AWS_ENCRYPTION_KEY_INVALID_MESSAGE)
    @ApiModelProperty(EnvironmentModelDescription.AWS_DISK_ENCRYPTION_PARAMETERS)
    private String encryptionKeyArn;

    public AwsDiskEncryptionParameters() {
    }

    private AwsDiskEncryptionParameters(Builder builder) {
        this.encryptionKeyArn = builder.encryptionKeyArn;
    }

    public String getEncryptionKeyArn() {
        return encryptionKeyArn;
    }

    public void setEncryptionKeyArn(String encryptionKeyArn) {
        this.encryptionKeyArn = encryptionKeyArn;
    }

    @Override
    public String toString() {
        return "AwsDiskEncryptionParameters{" +
                "encryptionKeyArn=" + encryptionKeyArn + '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String encryptionKeyArn;

        private Builder() {
        }

        public Builder withEncryptionKeyArn(String encryptionKeyArn) {
            this.encryptionKeyArn = encryptionKeyArn;
            return this;
        }

        public AwsDiskEncryptionParameters build() {
            return new AwsDiskEncryptionParameters(this);
        }
    }
}
