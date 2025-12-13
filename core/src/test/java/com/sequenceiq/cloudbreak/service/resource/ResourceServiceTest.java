package com.sequenceiq.cloudbreak.service.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.repository.ResourceRepository;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class ResourceServiceTest {

    @InjectMocks
    private ResourceService underTest;

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private ResourceToCloudResourceConverter cloudResourceToResourceConverter;

    @Test
    public void testGetAllAsCloudResourceStatusWhenNoResource() {
        List<Resource> resources = new ArrayList<>();

        when(resourceRepository.findAllByStackId(1L)).thenReturn(resources);
        List<CloudResourceStatus> actual = underTest.getAllAsCloudResourceStatus(1L);

        assertEquals(0L, actual.size());
    }

    @Test
    public void testGetAllAsCloudResourceStatusWhenHasResource() {
        Resource resource = new Resource();
        List<Resource> resources = Collections.singletonList(resource);

        CloudResource cloudResource = CloudResource.builder()
                .withType(ResourceType.CLOUDFORMATION_STACK)
                .withStatus(CommonStatus.CREATED)
                .withName("name")
                .withParameters(Collections.emptyMap())
                .build();

        when(resourceRepository.findAllByStackId(1L)).thenReturn(resources);
        when(cloudResourceToResourceConverter.convert(resource)).thenReturn(cloudResource);

        List<CloudResourceStatus> actual = underTest.getAllAsCloudResourceStatus(1L);

        assertEquals(1L, actual.size());
        assertEquals(cloudResource, actual.getFirst().getCloudResource());
        assertEquals(ResourceStatus.CREATED, actual.getFirst().getStatus());
    }
}
