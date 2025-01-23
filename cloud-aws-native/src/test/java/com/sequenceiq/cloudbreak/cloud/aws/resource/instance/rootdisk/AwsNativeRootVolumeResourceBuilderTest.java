package com.sequenceiq.cloudbreak.cloud.aws.resource.instance.rootdisk;

import static com.sequenceiq.cloudbreak.cloud.aws.resource.AwsNativeResourceBuilderOrderConstants.NATIVE_ROOT_VOLUME_RESOURCE_BUILDER_ORDER;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith({ MockitoExtension.class })
public class AwsNativeRootVolumeResourceBuilderTest {

    @InjectMocks
    private AwsNativeRootVolumeResourceBuilder underTest;

    @Test
    void testResourceType() {
        assertEquals(ResourceType.AWS_ROOT_DISK, underTest.resourceType());
    }

    @Test
    void testOrder() {
        assertEquals(NATIVE_ROOT_VOLUME_RESOURCE_BUILDER_ORDER, underTest.order());
    }

    @Test
    void testVariant() {
        assertEquals(AwsConstants.AwsVariant.AWS_NATIVE_VARIANT.variant(), underTest.variant());
    }
}
