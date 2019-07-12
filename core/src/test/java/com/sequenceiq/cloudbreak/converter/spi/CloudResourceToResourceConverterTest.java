package com.sequenceiq.cloudbreak.converter.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.common.json.Json;

class CloudResourceToResourceConverterTest {

    private CloudResourceToResourceConverter underTest = new CloudResourceToResourceConverter();

    @Test
    void convertPartial() {
        Resource convertedResource = underTest.convert(CloudResource.builder()
                .type(ResourceType.AWS_ENCRYPTED_AMI)
                .status(CommonStatus.REQUESTED)
                .name("ami-01231h231")
                .params(Map.of())
                .build());

        assertEquals(ResourceType.AWS_ENCRYPTED_AMI, convertedResource.getResourceType());
        assertEquals(CommonStatus.REQUESTED, convertedResource.getResourceStatus());
        assertEquals("ami-01231h231", convertedResource.getResourceName());
        assertNull(convertedResource.getResourceReference());
        assertNull(convertedResource.getInstanceGroup());
        assertNull(convertedResource.getInstanceId());
        assertNull(convertedResource.getAttributes());
    }

    @Test
    void convert() throws JsonProcessingException {
        Map<String, Object> parameters = Map.of("key", "test");
        Json attributes = new Json(parameters);
        Resource convertedResource = underTest.convert(CloudResource.builder()
                .type(ResourceType.AWS_ENCRYPTED_AMI)
                .status(CommonStatus.REQUESTED)
                .name("ami-01231h231")
                .group("group")
                .instanceId("id")
                .params(Map.of(CloudResource.ATTRIBUTES, Map.of("key", "test")))
                .reference("ref")
                .build());

        assertEquals(ResourceType.AWS_ENCRYPTED_AMI, convertedResource.getResourceType());
        assertEquals(CommonStatus.REQUESTED, convertedResource.getResourceStatus());
        assertEquals("ami-01231h231", convertedResource.getResourceName());
        assertEquals("ref", convertedResource.getResourceReference());
        assertEquals("group", convertedResource.getInstanceGroup());
        assertEquals("id", convertedResource.getInstanceId());
        assertEquals(attributes.getValue(), convertedResource.getAttributes().getValue());
    }
}