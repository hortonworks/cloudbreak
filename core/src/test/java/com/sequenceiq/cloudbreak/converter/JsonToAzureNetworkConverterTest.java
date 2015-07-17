package com.sequenceiq.cloudbreak.converter;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.json.NetworkJson;
import com.sequenceiq.cloudbreak.domain.AzureNetwork;

public class JsonToAzureNetworkConverterTest extends AbstractJsonConverterTest<NetworkJson> {

    private JsonToAzureNetworkConverter underTest;

    @Before
    public void setUp() {
        underTest = new JsonToAzureNetworkConverter();
    }

    @Test
    public void testConvert() {
        // GIVEN
        // WHEN
        AzureNetwork result = underTest.convert(getRequest("network/azure-network.json"));
        // THEN
        assertAllFieldsNotNull(result);
    }

    @Test(expected = BadRequestException.class)
    public void testConvertInvalid() {
        // GIVEN
        // WHEN
        underTest.convert(getRequest("network/azure-network-invalid.json"));
    }

    @Override
    public Class<NetworkJson> getRequestClass() {
        return NetworkJson.class;
    }
}
