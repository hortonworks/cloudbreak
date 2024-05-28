package com.sequenceiq.cloudbreak.cloud.aws.resource.kms;

import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants.AwsVariant.AWS_NATIVE_VARIANT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class AwsNativeKmsKeyResourceBuilderTest {

    private AwsNativeKmsKeyResourceBuilder underTest = new AwsNativeKmsKeyResourceBuilder();

    @Test
    void testVariant() {
        assertEquals(AWS_NATIVE_VARIANT.variant(), underTest.variant());
    }
}
