package com.sequenceiq.cloudbreak.domain.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;

class ResourceStatusConverterTest {

    private final ResourceStatusConverter resourceStatusConverter = new ResourceStatusConverter();

    @Test
    void testConvertToDatabaseColumnWhenDefaultIsReturnAsDefault() {
        String defaultString = resourceStatusConverter.convertToDatabaseColumn(ResourceStatus.DEFAULT);
        assertEquals("DEFAULT", defaultString);
    }

    @Test
    void testConvertToEntityAttributeWhenDefaultIsReturnAsDefault() {
        ResourceStatus resourceStatus = resourceStatusConverter.convertToEntityAttribute("DEFAULT");
        assertEquals(ResourceStatus.DEFAULT, resourceStatus);
    }

    @Test
    void testConvertToEntityAttributeWhenNotKnownIsReturnAsUserManaged() {
        ResourceStatus resourceStatus = resourceStatusConverter.convertToEntityAttribute("not-known");
        assertEquals(ResourceStatus.USER_MANAGED, resourceStatus);
    }

}