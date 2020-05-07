package com.sequenceiq.cloudbreak.domain.converter;

import org.junit.Assert;
import org.junit.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;

public class ResourceStatusConverterTest {

    private final ResourceStatusConverter resourceStatusConverter = new ResourceStatusConverter();

    @Test
    public void testConvertToDatabaseColumnWhenDefaultIsReturnAsDefault() {
        String defaultString = resourceStatusConverter.convertToDatabaseColumn(ResourceStatus.DEFAULT);
        Assert.assertEquals("DEFAULT", defaultString);
    }

    @Test
    public void testConvertToEntityAttributeWhenDefaultIsReturnAsDefault() {
        ResourceStatus resourceStatus = resourceStatusConverter.convertToEntityAttribute("DEFAULT");
        Assert.assertEquals(ResourceStatus.DEFAULT, resourceStatus);
    }

    @Test
    public void testConvertToEntityAttributeWhenNotKnownIsReturnAsUserManaged() {
        ResourceStatus resourceStatus = resourceStatusConverter.convertToEntityAttribute("not-known");
        Assert.assertEquals(ResourceStatus.USER_MANAGED, resourceStatus);
    }

}