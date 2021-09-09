package com.sequenceiq.freeipa.service.cloud;

import static com.sequenceiq.common.api.type.CommonStatus.REQUESTED;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_MANAGED_IMAGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;

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

    private static final String RESOURCE_REFERENCE = "resource-reference";

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
        when(resourceService.findByResourceReferenceAndStatusAndTypeAndStack(RESOURCE_REFERENCE, REQUESTED, AZURE_MANAGED_IMAGE, STACK_ID))
                .thenReturn(Optional.empty());

        Optional<CloudResource> actual = underTest.findByResourceReferenceAndStatusAndTypeAndStack(RESOURCE_REFERENCE, REQUESTED, AZURE_MANAGED_IMAGE, STACK_ID);

        assertTrue(actual.isEmpty());
        verify(resourceService).findByResourceReferenceAndStatusAndTypeAndStack(RESOURCE_REFERENCE, REQUESTED, AZURE_MANAGED_IMAGE, STACK_ID);
    }

    @Test
    public void testGetByResourceReferenceShouldReturnNOTEmptyList() {
        Resource resource = createResource();
        CloudResource cloudResource = createCloudResource();

        when(resourceService.findByResourceReferenceAndStatusAndTypeAndStack(RESOURCE_REFERENCE, REQUESTED, AZURE_MANAGED_IMAGE, STACK_ID))
                .thenReturn(Optional.of(resource));
        when(cloudResourceConverter.convert(resource)).thenReturn(cloudResource);

        Optional<CloudResource> actual = underTest.findByResourceReferenceAndStatusAndTypeAndStack(RESOURCE_REFERENCE, REQUESTED, AZURE_MANAGED_IMAGE, STACK_ID);

        assertTrue(actual.isPresent());
        assertEquals(cloudResource, actual.get());
        verify(resourceService).findByResourceReferenceAndStatusAndTypeAndStack(RESOURCE_REFERENCE, REQUESTED, AZURE_MANAGED_IMAGE, STACK_ID);
        verify(cloudResourceConverter).convert(resource);
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