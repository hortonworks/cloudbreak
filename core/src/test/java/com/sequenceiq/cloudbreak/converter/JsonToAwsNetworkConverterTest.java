package com.sequenceiq.cloudbreak.converter;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.controller.json.NetworkJson;
import com.sequenceiq.cloudbreak.domain.AwsNetwork;

public class JsonToAwsNetworkConverterTest extends AbstractJsonConverterTest<NetworkJson> {

    private JsonToAwsNetworkConverter underTest;

    @Before
    public void setUp() {
        underTest = new JsonToAwsNetworkConverter();
    }

    @Test
    public void testConvert() {
        // GIVEN
        // WHEN
        AwsNetwork result = underTest.convert(getRequest("network/aws-network.json"));
        // THEN
        assertAllFieldsNotNull(result);
    }

    @Override
    public Class<NetworkJson> getRequestClass() {
        return NetworkJson.class;
    }
}
