package com.sequenceiq.redbeams.service.cloud;

import static com.sequenceiq.common.api.type.CommonStatus.CREATED;
import static com.sequenceiq.common.api.type.CommonStatus.REQUESTED;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_DATABASE;
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
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.redbeams.converter.spi.DBResourceToCloudResourceConverter;
import com.sequenceiq.redbeams.domain.stack.DBResource;
import com.sequenceiq.redbeams.service.stack.DBResourceService;

@ExtendWith(MockitoExtension.class)
class CloudResourceRetrieverServiceTest {

    private static final String RESOURCE = "resource1";

    private static final ResourceType RESOURCE_TYPE = AZURE_DATABASE;

    private static final Long STACK_ID = 1L;

    @InjectMocks
    private CloudResourceRetrieverService underTest;

    @Mock
    private DBResourceToCloudResourceConverter cloudResourceConverter;

    @Mock
    private DBResourceService resourceService;

    @Test
    void testFindFirstByStatusAndTypeAndStackShouldReturnCloudResource() {
        DBResource resource = createResource();
        CloudResource cloudResource = createCloudResource();

        when(resourceService.findByStatusAndTypeAndStack(CREATED, RESOURCE_TYPE, STACK_ID)).thenReturn(Optional.of(resource));
        when(cloudResourceConverter.convert(resource)).thenReturn(cloudResource);

        Optional<CloudResource> actual = underTest.findByStatusAndTypeAndStack(CREATED, RESOURCE_TYPE, STACK_ID);

        assertTrue(actual.isPresent());
        assertEquals(cloudResource, actual.get());
        verify(resourceService).findByStatusAndTypeAndStack(CREATED, RESOURCE_TYPE, STACK_ID);
        verify(cloudResourceConverter).convert(resource);
    }

    @Test
    void testFindFirstByStatusAndTypeAndStackShouldReturnEmptyList() {
        when(resourceService.findByStatusAndTypeAndStack(CREATED, RESOURCE_TYPE, STACK_ID)).thenReturn(Optional.empty());

        Optional<CloudResource> actual = underTest.findByStatusAndTypeAndStack(CREATED, RESOURCE_TYPE, STACK_ID);

        assertTrue(actual.isEmpty());
        verify(resourceService).findByStatusAndTypeAndStack(CREATED, RESOURCE_TYPE, STACK_ID);
    }

    private DBResource createResource() {
        DBResource resource = new DBResource();
        resource.setResourceName(RESOURCE);
        return resource;
    }

    private CloudResource createCloudResource() {
        return CloudResource.builder()
                .withName(RESOURCE)
                .withType(RESOURCE_TYPE)
                .withStatus(REQUESTED)
                .withParameters(Collections.emptyMap())
                .build();
    }
}
