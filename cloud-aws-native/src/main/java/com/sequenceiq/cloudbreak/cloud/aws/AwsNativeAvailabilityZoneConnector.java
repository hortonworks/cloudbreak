package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.AvailabilityZoneConnector;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

@Component
public class AwsNativeAvailabilityZoneConnector implements AvailabilityZoneConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(AwsNativeAvailabilityZoneConnector.class);

    @Override
    public Set<String> getAvailabilityZones(ExtendedCloudCredential cloudCredential, Set<String> environmentZones, String instanceType, Region region) {
        return environmentZones;
    }

    @Override
    public Platform platform() {
        return AwsConstants.AWS_PLATFORM;
    }

    @Override
    public Variant variant() {
        return AwsConstants.AwsVariant.AWS_NATIVE_VARIANT.variant();
    }
}