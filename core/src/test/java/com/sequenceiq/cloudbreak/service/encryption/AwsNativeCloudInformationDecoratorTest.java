package com.sequenceiq.cloudbreak.service.encryption;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

public class AwsNativeCloudInformationDecoratorTest {
    private AwsNativeCloudInformationDecorator underTest = new AwsNativeCloudInformationDecorator();

    @Test
    void testVariant() {
        Variant variant = underTest.variant();
        assertEquals(AwsConstants.AwsVariant.AWS_NATIVE_VARIANT.variant(), variant);
    }
}
