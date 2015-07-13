package com.sequenceiq.cloudbreak.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.controller.json.InstanceMetaDataJson;
import com.sequenceiq.cloudbreak.domain.InstanceGroupType;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.InstanceStatus;

public class MetaDataToJsonConverterTest extends AbstractEntityConverterTest<InstanceMetaData> {

    private MetaDataToJsonConverter underTest;

    @Before
    public void setUp() {
        underTest = new MetaDataToJsonConverter();
    }

    @Test
    public void testConvert() {
        // GIVEN
        // WHEN
        InstanceMetaDataJson result = underTest.convert(getSource());
        // THEN
        assertEquals("test", result.getDiscoveryFQDN());
        assertTrue(result.getAmbariServer());
        assertAllFieldsNotNull(result);
    }

    @Override
    public InstanceMetaData createSource() {
        return TestUtil.instanceMetaData(1L, InstanceStatus.REGISTERED, true,
                TestUtil.instanceGroup(1L, InstanceGroupType.GATEWAY, TestUtil.azureTemplate(1L)));
    }
}
