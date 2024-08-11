package com.sequenceiq.freeipa.converter.cloud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.freeipa.entity.Resource;
import com.sequenceiq.freeipa.service.resource.ResourceAttributeUtil;

@ExtendWith(MockitoExtension.class)
public class ResourceToCloudResourceConverterTest {

    private static final String AVAILABILITY_ZONE = "us-west2-c";

    @Mock
    private ResourceAttributeUtil resourceAttributeUtil;

    @Mock
    private Resource resource;

    @InjectMocks
    private ResourceToCloudResourceConverter underTest;

    @Test
    void testConvert() {
        when(resourceAttributeUtil.getTypedAttributes(resource)).thenReturn(Optional.of("test"));
        when(resource.getResourceType()).thenReturn(ResourceType.GCP_INSTANCE);
        when(resource.getResourceName()).thenReturn("test");
        when(resource.getResourceStatus()).thenReturn(CommonStatus.CREATED);
        when(resource.getAvailabilityZone()).thenReturn(AVAILABILITY_ZONE);
        CloudResource cloudResource = underTest.convert(resource);
        assertEquals(AVAILABILITY_ZONE, cloudResource.getAvailabilityZone());
    }
}
