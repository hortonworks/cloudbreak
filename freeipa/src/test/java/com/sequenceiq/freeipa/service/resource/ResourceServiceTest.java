package com.sequenceiq.freeipa.service.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.freeipa.converter.cloud.ResourceToCloudResourceConverter;
import com.sequenceiq.freeipa.entity.Resource;
import com.sequenceiq.freeipa.repository.ResourceRepository;

@ExtendWith(MockitoExtension.class)
class ResourceServiceTest {

    @Mock
    private ResourceRepository repository;

    @Mock
    private ResourceToCloudResourceConverter resourceToCloudResourceConverter;

    @InjectMocks
    private ResourceService underTest;

    @Test
    void testFindAllByStackIdAndInstanceGroupAndResourceTypeIn() {
        Resource resource = mock(Resource.class);
        List<Resource> resourceList = List.of(resource);
        when(repository.findAllByStackIdAndInstanceGroupAndResourceTypeInAndInstanceIdIsNotNull(any(), any(), any())).thenReturn(resourceList);
        List<Resource> result = underTest.findAllByStackIdAndInstanceGroupAndResourceTypeIn(1L, "test", List.of(ResourceType.AZURE_INSTANCE));
        assertEquals(resourceList, result);
    }

    @Test
    void testFindByStackIdAndType() {
        Resource resource = mock(Resource.class);
        List<Resource> resourceList = List.of(resource);
        when(repository.findByStackIdAndType(1L, ResourceType.AWS_ROOT_DISK)).thenReturn(resourceList);
        List<Resource> resources = underTest.findByStackIdAndType(1L, ResourceType.AWS_ROOT_DISK);
        assertEquals(resourceList, resources);
    }

    @Test
    void testDeleteAll() {
        Resource resource = mock(Resource.class);
        List<Resource> resourceList = List.of(resource);
        underTest.deleteAll(resourceList);

        verify(repository).deleteAll(resourceList);
    }
}
