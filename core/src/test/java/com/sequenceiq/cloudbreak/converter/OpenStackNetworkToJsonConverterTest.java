package com.sequenceiq.cloudbreak.converter;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.controller.json.NetworkJson;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.OpenStackNetwork;

public class OpenStackNetworkToJsonConverterTest extends AbstractEntityConverterTest<OpenStackNetwork> {

    private OpenStackNetworkToJsonConverter underTest;

    @Before
    public void setUp() {
        underTest = new OpenStackNetworkToJsonConverter();
    }

    @Test
    public void testConvert() {
        // GIVEN
        // WHEN
        NetworkJson result = underTest.convert(getSource());
        // THEN
        assertEquals("10.0.0.1/16", result.getSubnetCIDR());
        assertEquals(CloudPlatform.OPENSTACK, result.getCloudPlatform());
        assertAllFieldsNotNull(result, Arrays.asList("description"));
    }

    @Override
    public OpenStackNetwork createSource() {
        return TestUtil.openStackNetwork("10.0.0.1/16");
    }
}
