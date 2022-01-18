package com.sequenceiq.cloudbreak.cloud.aws;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

@Service
public class AwsGovConnector extends AwsNativeConnector {

    @Override
    public Variant variant() {
        return AwsConstants.AwsVariant.AWS_GOV_VARIANT.variant();
    }
}
