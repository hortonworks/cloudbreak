package com.sequenceiq.cloudbreak.service.encryption;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;

public class AwsGovCloudInformationDecoratorTest {

    private final AwsGovCloudInformationDecorator underTest = new AwsGovCloudInformationDecorator();

    @Test
    void testGetArnPartition() {
        assertEquals("aws-us-gov", underTest.getArnPartition());
    }

    @Test
    void testVariant() {
        assertEquals(AwsConstants.AwsVariant.AWS_NATIVE_GOV_VARIANT.variant(), underTest.variant());
    }
}
