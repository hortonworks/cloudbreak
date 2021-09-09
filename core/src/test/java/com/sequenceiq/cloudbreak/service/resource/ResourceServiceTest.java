package com.sequenceiq.cloudbreak.service.resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.repository.ResourceRepository;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@RunWith(MockitoJUnitRunner.class)
public class ResourceServiceTest {

    @InjectMocks
    private final ResourceService underTest = new ResourceService();

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private ResourceToCloudResourceConverter cloudResourceToResourceConverter;

    @Test
    public void testGetAllAsCloudResourceStatusWhenNoResource() {
        List<Resource> resources = new ArrayList<>();

        Mockito.when(resourceRepository.findAllByStackId(1L)).thenReturn(resources);
        List<CloudResourceStatus> actual = underTest.getAllAsCloudResourceStatus(1L);

        Assert.assertEquals(0L, actual.size());
    }

    @Test
    public void testGetAllAsCloudResourceStatusWhenHasResource() {
        Resource resource = new Resource();
        List<Resource> resources = Collections.singletonList(resource);

        CloudResource cloudResource = CloudResource.builder()
                .type(ResourceType.HEAT_STACK)
                .status(CommonStatus.CREATED)
                .name("name")
                .params(Collections.emptyMap())
                .build();

        Mockito.when(resourceRepository.findAllByStackId(1L)).thenReturn(resources);
        Mockito.when(cloudResourceToResourceConverter.convert(resource)).thenReturn(cloudResource);

        List<CloudResourceStatus> actual = underTest.getAllAsCloudResourceStatus(1L);

        Assert.assertEquals(1L, actual.size());
        Assert.assertEquals(cloudResource, actual.get(0).getCloudResource());
        Assert.assertEquals(ResourceStatus.CREATED, actual.get(0).getStatus());
    }
}
