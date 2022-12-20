package com.sequenceiq.cloudbreak.service.eventbus;

import static com.sequenceiq.common.api.type.CommonStatus.REQUESTED;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_MANAGED_IMAGE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

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

    private static final List<String> RESOURCE_REFERENCES = List.of("resource-reference");

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

        when(resourceService.findByResourceReferencesAndStatusAndType(RESOURCE_REFERENCES, REQUESTED, AZURE_MANAGED_IMAGE)).thenReturn(List.of(resource));
        when(cloudResourceConverter.convert(resource)).thenReturn(cloudResource);

        List<CloudResource> actual = underTest.findByResourceReferencesAndStatusAndType(RESOURCE_REFERENCES, REQUESTED, AZURE_MANAGED_IMAGE);

        assertFalse(actual.isEmpty());
        assertEquals(cloudResource, actual.get(0));
        verify(resourceService).findByResourceReferencesAndStatusAndType(RESOURCE_REFERENCES, REQUESTED, AZURE_MANAGED_IMAGE);
        verify(cloudResourceConverter).convert(resource);
    }

    @Test
    public void testGetByResourceReferenceShouldReturnEmptyList() {
        when(resourceService.findByResourceReferencesAndStatusAndType(RESOURCE_REFERENCES, REQUESTED, AZURE_MANAGED_IMAGE)).thenReturn(List.of());

        List<CloudResource> actual = underTest.findByResourceReferencesAndStatusAndType(RESOURCE_REFERENCES, REQUESTED, AZURE_MANAGED_IMAGE);

        assertTrue(actual.isEmpty());
        verify(resourceService).findByResourceReferencesAndStatusAndType(RESOURCE_REFERENCES, REQUESTED, AZURE_MANAGED_IMAGE);
    }

    private Resource createResource() {
        Resource resource = new Resource();
        resource.setResourceName(RESOURCE);
        return resource;
    }

    private CloudResource createCloudResource() {
        return CloudResource.builder()
                .withName(RESOURCE)
                .withType(AZURE_MANAGED_IMAGE)
                .withStatus(REQUESTED)
                .withParameters(Collections.emptyMap())
                .build();
    }
}
