package com.sequenceiq.cloudbreak.cloud.aws.resource.kms;

import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants.AwsVariant.AWS_NATIVE_GOV_VARIANT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class AwsGovKmsKeyResourceBuilderTest {
    private AwsGovKmsKeyResourceBuilder underTest = new AwsGovKmsKeyResourceBuilder();

    @Test
    void testVariant() {
        assertEquals(AWS_NATIVE_GOV_VARIANT.variant(), underTest.variant());
    }
}
