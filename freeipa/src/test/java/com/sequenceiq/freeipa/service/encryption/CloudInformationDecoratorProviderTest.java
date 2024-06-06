package com.sequenceiq.freeipa.service.encryption;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.freeipa.entity.Stack;

@ExtendWith(MockitoExtension.class)
class CloudInformationDecoratorProviderTest {

    private static final  AwsCloudInformationDecorator AWS_CLOUD_INFORMATION_DECORATOR = new AwsCloudInformationDecorator();

    @Spy
    private List<CloudInformationDecorator> cloudInformationDecorators = List.of(AWS_CLOUD_INFORMATION_DECORATOR);

    @InjectMocks
    private CloudInformationDecoratorProvider underTest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "cloudInformationDecorators", cloudInformationDecorators);
        ReflectionTestUtils.invokeMethod(underTest, "init");
    }

    @Test
    void testGet() {
        CloudPlatformVariant cloudPlatformVariant = new CloudPlatformVariant(AwsConstants.AWS_PLATFORM, AwsConstants.AWS_DEFAULT_VARIANT);

        assertEquals(AWS_CLOUD_INFORMATION_DECORATOR, underTest.get(cloudPlatformVariant));
    }

    @Test
    void testGetForStack() {
        Stack stack = new Stack();
        stack.setCloudPlatform(AwsConstants.AWS_PLATFORM.getValue());
        stack.setPlatformvariant(AwsConstants.AWS_DEFAULT_VARIANT.getValue());

        assertEquals(AWS_CLOUD_INFORMATION_DECORATOR, underTest.getForStack(stack));
    }
}
