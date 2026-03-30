package com.sequenceiq.cloudbreak.cloud.aws;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.CommonSecretEncryptionValidator;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.cloud.aws.validator.AwsGovSecretEncryptionValidator;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

@Service
public class AwsGovConnector extends AwsNativeConnector {

    @Inject
    private AwsGovSecretEncryptionValidator awsGovSecretEncryptionValidator;

    @Override
    protected CommonSecretEncryptionValidator secretEncryptionValidator() {
        return awsGovSecretEncryptionValidator;
    }

    @Override
    public Variant variant() {
        return AwsConstants.AwsVariant.AWS_NATIVE_GOV_VARIANT.variant();
    }
}
