package com.sequenceiq.cloudbreak.service.eventbus;

import static com.sequenceiq.common.api.type.CommonStatus.REQUESTED;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_MANAGED_IMAGE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;

@RunWith(MockitoJUnitRunner.class)
public class CloudResourceRetrieverServiceTest {

    private static final String RESOURCE_REFERENCE = "resource-reference";

    private static final String RESOURCE = "resource1";

    @InjectMocks
    private CloudResourceRetrieverService underTest;

    @Mock
    private ResourceToCloudResourceConverter cloudResourceConverter;

    @Mock
    private ResourceService resourceService;

    @Test
    public void testGetByResourceReferenceShouldReturnCloudResource() {
        Resource resource = createResource();
        CloudResource cloudResource = createCloudResource();

        when(resourceService.findByResourceReferenceAndStatusAndType(RESOURCE_REFERENCE, REQUESTED, AZURE_MANAGED_IMAGE)).thenReturn(Optional.of(resource));
        when(cloudResourceConverter.convert(resource)).thenReturn(cloudResource);

        Optional<CloudResource> actual = underTest.findByResourceReferenceAndStatusAndType(RESOURCE_REFERENCE, REQUESTED, AZURE_MANAGED_IMAGE);

        assertTrue(actual.isPresent());
        assertEquals(cloudResource, actual.get());
        verify(resourceService).findByResourceReferenceAndStatusAndType(RESOURCE_REFERENCE, REQUESTED, AZURE_MANAGED_IMAGE);
        verify(cloudResourceConverter).convert(resource);
    }

    @Test
    public void testGetByResourceReferenceShouldReturnEmptyList() {
        when(resourceService.findByResourceReferenceAndStatusAndType(RESOURCE_REFERENCE, REQUESTED, AZURE_MANAGED_IMAGE)).thenReturn(Optional.empty());

        Optional<CloudResource> actual = underTest.findByResourceReferenceAndStatusAndType(RESOURCE_REFERENCE, REQUESTED, AZURE_MANAGED_IMAGE);

        assertTrue(actual.isEmpty());
        verify(resourceService).findByResourceReferenceAndStatusAndType(RESOURCE_REFERENCE, REQUESTED, AZURE_MANAGED_IMAGE);
    }

    private Resource createResource() {
        Resource resource = new Resource();
        resource.setResourceName(RESOURCE);
        return resource;
    }

    private CloudResource createCloudResource() {
        return CloudResource.builder()
                .name(RESOURCE)
                .type(AZURE_MANAGED_IMAGE)
                .status(REQUESTED)
                .params(Collections.emptyMap())
                .build();
    }
}