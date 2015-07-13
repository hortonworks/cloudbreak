package com.sequenceiq.cloudbreak.converter;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.controller.json.NetworkJson;
import com.sequenceiq.cloudbreak.domain.AzureNetwork;

public class AzureNetworkToJsonEntityConverterTest extends AbstractEntityConverterTest<AzureNetwork> {

    private AzureNetworkToJsonConverter underTest;

    @Before
    public void setUp() {
        underTest = new AzureNetworkToJsonConverter();
    }

    @Test
    public void testConvert() {
        // GIVEN
        // WHEN
        NetworkJson result = underTest.convert(getSource());
        // THEN
        assertEquals("10.0.0.1/16", result.getSubnetCIDR());
        assertAllFieldsNotNull(result, Arrays.asList("description"));
    }

    @Override
    public AzureNetwork createSource() {
        return (AzureNetwork) TestUtil.network();
    }
}
