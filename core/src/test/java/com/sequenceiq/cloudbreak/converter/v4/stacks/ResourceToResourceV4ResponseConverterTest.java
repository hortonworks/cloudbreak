package com.sequenceiq.cloudbreak.converter.v4.stacks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.resource.ResourceV4Response;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

public class ResourceToResourceV4ResponseConverterTest {

    private ResourceToResourceV4ResponseConverter underTest;

    @BeforeEach
    public void setUp() {
        underTest = new ResourceToResourceV4ResponseConverter();
    }

    @Test
    public void testConvertResourceToResourceV4Response() {
        Resource resource = new Resource();
        resource.setAttributes(new Json("{name: \"test\"}"));
        resource.setId(1L);
        resource.setInstanceGroup("TEST");
        resource.setResourceType(ResourceType.AWS_VOLUMESET);
        resource.setResourceStatus(CommonStatus.CREATED);
        Stack stack = mock(Stack.class);
        doReturn(1L).when(stack).getId();
        resource.setStack(stack);
        ResourceV4Response response = underTest.convertResourceToResourceV4Response(resource);
        assertEquals(1L, response.getId());
        assertEquals("TEST", response.getInstanceGroup());
        assertEquals(ResourceType.AWS_VOLUMESET, response.getResourceType());
        assertEquals(CommonStatus.CREATED, response.getResourceStatus());
    }
}
