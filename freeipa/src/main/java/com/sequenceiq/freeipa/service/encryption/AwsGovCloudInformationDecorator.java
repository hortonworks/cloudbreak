package com.sequenceiq.freeipa.service.encryption;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

@Component
public class AwsGovCloudInformationDecorator extends AwsCloudInformationDecorator {

    private static final String US_GOV_ARN_PARTITION = "aws-us-gov";

    @Override
    public Variant variant() {
        return AwsConstants.AwsVariant.AWS_NATIVE_GOV_VARIANT.variant();
    }

    @Override
    protected String getArnPartition() {
        return US_GOV_ARN_PARTITION;
    }

}
