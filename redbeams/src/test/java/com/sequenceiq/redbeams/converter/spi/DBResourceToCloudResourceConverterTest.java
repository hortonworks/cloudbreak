package com.sequenceiq.redbeams.converter.spi;


import static com.sequenceiq.common.api.type.ResourceType.AZURE_DATABASE;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.redbeams.domain.stack.DBResource;

@ExtendWith(MockitoExtension.class)
class DBResourceToCloudResourceConverterTest {

    private static final String RESOURCE_NAME = "aResource";

    private static final ResourceType RESOURCE_TYPE = AZURE_DATABASE;

    private static final String RESOURCE_REFERENCE = "aReference";

    @InjectMocks
    private DBResourceToCloudResourceConverter underTest;

    @Test
    void testConvert() {
        CloudResource cloudResource = underTest.convert(createResource());
        Assertions.assertEquals(RESOURCE_NAME, cloudResource.getName());
        Assertions.assertEquals(RESOURCE_TYPE, cloudResource.getType());
        Assertions.assertEquals(RESOURCE_REFERENCE, cloudResource.getReference());
        Assertions.assertEquals(CommonStatus.CREATED, cloudResource.getStatus());
    }

    private DBResource createResource() {
        DBResource resource = new DBResource();
        resource.setResourceName(RESOURCE_NAME);
        resource.setResourceType(RESOURCE_TYPE);
        resource.setResourceReference(RESOURCE_REFERENCE);
        resource.setResourceStatus(CommonStatus.CREATED);
        return resource;
    }
}