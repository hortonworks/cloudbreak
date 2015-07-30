package com.sequenceiq.cloudbreak.converter;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.json.NetworkJson;
import com.sequenceiq.cloudbreak.domain.OpenStackNetwork;

public class JsonToOpenStackNetworkConverterTest extends AbstractJsonConverterTest<NetworkJson> {

    private JsonToOpenStackNetworkConverter underTest;

    @Before
    public void setUp() {
        underTest = new JsonToOpenStackNetworkConverter();
    }

    @Test
    public void testConvert() {
        // GIVEN
        // WHEN
        OpenStackNetwork network = underTest.convert(getRequest("network/openstack-network.json"));
        // THEN
        assertAllFieldsNotNull(network);
    }

    @Test(expected = BadRequestException.class)
    public void testConvertWhenMissingPublicNetId() {
        // GIVEN
        // WHEN
        underTest.convert(getRequest("network/openstack-network-invalid.json"));
    }

    @Override
    public Class<NetworkJson> getRequestClass() {
        return NetworkJson.class;
    }
}
