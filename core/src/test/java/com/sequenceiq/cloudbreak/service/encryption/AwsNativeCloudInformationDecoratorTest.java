package com.sequenceiq.cloudbreak.service.encryption;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;

public class AwsNativeCloudInformationDecoratorTest {

    private final AwsNativeCloudInformationDecorator underTest = new AwsNativeCloudInformationDecorator();

    @Test
    void testVariant() {
        assertEquals(AwsConstants.AwsVariant.AWS_NATIVE_VARIANT.variant(), underTest.variant());
    }
}
