package com.sequenceiq.cloudbreak.cloud.aws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

@Component
public class AwsGovAvailabilityZoneConnector extends AwsNativeAvailabilityZoneConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(AwsGovAvailabilityZoneConnector.class);

    @Override
    public Variant variant() {
        return AwsConstants.AwsVariant.AWS_NATIVE_GOV_VARIANT.variant();
    }
}