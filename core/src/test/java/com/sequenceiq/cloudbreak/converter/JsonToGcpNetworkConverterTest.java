package com.sequenceiq.cloudbreak.converter;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.controller.json.NetworkJson;
import com.sequenceiq.cloudbreak.domain.GcpNetwork;

public class JsonToGcpNetworkConverterTest extends AbstractJsonConverterTest<NetworkJson> {

    private JsonToGcpNetworkConverter underTest;

    @Before
    public void setUp() {
        underTest = new JsonToGcpNetworkConverter();
    }

    @Test
    public void testConvert() {
        // GIVEN
        // WHEN
        GcpNetwork result = underTest.convert(getRequest("network/gcp-network.json"));
        // THEN
        assertAllFieldsNotNull(result);
    }

    @Override
    public Class<NetworkJson> getRequestClass() {
        return NetworkJson.class;
    }
}
