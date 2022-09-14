package com.sequenceiq.freeipa.service.cloud;

import static com.sequenceiq.common.api.type.CommonStatus.REQUESTED;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_MANAGED_IMAGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.freeipa.converter.cloud.ResourceToCloudResourceConverter;
import com.sequenceiq.freeipa.entity.Resource;
import com.sequenceiq.freeipa.service.resource.ResourceService;

@ExtendWith(MockitoExtension.class)
public class CloudResourceRetrieverServiceTest {

    private static final List<String> RESOURCE_REFERENCE = List.of("resource-reference");

    private static final Long STACK_ID = 1L;

    private static final String RESOURCE = "resource1";

    @InjectMocks
    private CloudResourceRetrieverService underTest;

    @Mock
    private ResourceToCloudResourceConverter cloudResourceConverter;

    @Mock
    private ResourceService resourceService;

    @Test
    public void testGetByResourceReferenceShouldReturnEmptyList() {
        when(resourceService.findByResourceReferencesAndStatusAndTypeAndStack(RESOURCE_REFERENCE, REQUESTED, AZURE_MANAGED_IMAGE, STACK_ID))
                .thenReturn(List.of());

        List<CloudResource> actual = underTest.findByResourceReferencesAndStatusAndTypeAndStack(RESOURCE_REFERENCE, REQUESTED, AZURE_MANAGED_IMAGE, STACK_ID);

        assertTrue(actual.isEmpty());
        verify(resourceService).findByResourceReferencesAndStatusAndTypeAndStack(RESOURCE_REFERENCE, REQUESTED, AZURE_MANAGED_IMAGE, STACK_ID);
    }

    @Test
    public void testGetByResourceReferenceShouldReturnNOTEmptyList() {
        Resource resource = createResource();
        CloudResource cloudResource = createCloudResource();

        when(resourceService.findByResourceReferencesAndStatusAndTypeAndStack(RESOURCE_REFERENCE, REQUESTED, AZURE_MANAGED_IMAGE, STACK_ID))
                .thenReturn(List.of(resource));
        when(cloudResourceConverter.convert(resource)).thenReturn(cloudResource);

        List<CloudResource> actual = underTest.findByResourceReferencesAndStatusAndTypeAndStack(RESOURCE_REFERENCE, REQUESTED, AZURE_MANAGED_IMAGE, STACK_ID);

        assertFalse(actual.isEmpty());
        assertEquals(cloudResource, actual.get(0));
        verify(resourceService).findByResourceReferencesAndStatusAndTypeAndStack(RESOURCE_REFERENCE, REQUESTED, AZURE_MANAGED_IMAGE, STACK_ID);
        verify(cloudResourceConverter).convert(resource);
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
                .withParams(Collections.emptyMap())
                .build();
    }
}
