package com.sequenceiq.cloudbreak.converter;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.controller.json.NetworkJson;
import com.sequenceiq.cloudbreak.domain.GcpNetwork;

public class GcpNetworkToJsonEntityConverterTest extends AbstractEntityConverterTest<GcpNetwork> {

    private GcpNetworkToJsonConverter underTest;

    @Before
    public void setUp() {
        underTest = new GcpNetworkToJsonConverter();
    }

    @Test
    public void testConvert() {
        // GIVEN
        // WHEN
        NetworkJson result = underTest.convert(getSource());
        // THEN
        assertEquals("10.0.0.1/16", result.getSubnetCIDR());
        assertAllFieldsNotNull(result);
    }

    @Override
    public GcpNetwork createSource() {
        return TestUtil.gcpNetwork("10.0.0.1/16");
    }
}
