package com.sequenceiq.cloudbreak.service.stack.resource;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.AzureResourceNameService;


public class AzureResourceNameServiceTest {
    private ResourceNameService resourceNameService;

    @Before
    public void before() {
        resourceNameService = new AzureResourceNameService();
    }

    @Test
    public void testTrimHash() throws Exception {
        // given
        String name = "testing-resource-1234550";

        // when
        String resourceName = ((CloudbreakResourceNameService) resourceNameService).trimHash(name);

        // then
        Assert.assertEquals("Invalid resource name", "testing-resource", resourceName);
    }

    @Test
    public void keepLettersAndDigitsOnly() throws Exception {
        // given
        String invalidResourceName = "name With_ whitespaces and $ invalid characters";

        // when
        String resourceName = ((CloudbreakResourceNameService) resourceNameService).normalize(invalidResourceName);

        // then
        Assert.assertEquals("the normalized name is not as expected", "namewithwhitespacesandinvalidcharacters", resourceName);
    }

    @Test
    public void shouldAdjustLength() throws Exception {
        // given
        String invalidResourceName = "verylongresourcenamethatshouldbetrimmed";

        // when
        String resourceName = ((CloudbreakResourceNameService) resourceNameService).adjustPartLength(invalidResourceName);

        // then
        Assert.assertEquals("the shortened name is not as expected", "verylongresourcename", resourceName);

    }

    @Test
    public void shoudAssembleReservedIpResourceName() throws Exception {
        // given
        Long stackId = 1L;

        // when
        String resourceName = ((CloudbreakResourceNameService) resourceNameService).resourceName(ResourceType.AZURE_RESERVED_IP, stackId);

        // then
        Assert.assertEquals("the shortened name is not as expected", "reserved-ip-1", resourceName);


    }
}