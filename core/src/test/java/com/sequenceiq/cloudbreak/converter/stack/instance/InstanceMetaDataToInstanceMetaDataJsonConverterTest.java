package com.sequenceiq.cloudbreak.converter.stack.instance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceMetaDataJson;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceStatus;
import com.sequenceiq.cloudbreak.converter.AbstractEntityConverterTest;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;

public class InstanceMetaDataToInstanceMetaDataJsonConverterTest extends AbstractEntityConverterTest<InstanceMetaData> {

    private InstanceMetaDataToInstanceMetaDataJsonConverter underTest;

    @Before
    public void setUp() {
        underTest = new InstanceMetaDataToInstanceMetaDataJsonConverter();
    }

    @Test
    public void testConvert() {
        // GIVEN
        // WHEN
        InstanceMetaDataJson result = underTest.convert(getSource());
        // THEN
        assertEquals("test-1-1", result.getDiscoveryFQDN());
        assertTrue(result.getAmbariServer());
        assertAllFieldsNotNull(result);
    }

    @Override
    public InstanceMetaData createSource() {
        return TestUtil.instanceMetaData(1L, 1L, InstanceStatus.SERVICES_RUNNING, true,
                TestUtil.instanceGroup(1L, InstanceGroupType.GATEWAY, TestUtil.gcpTemplate(1L)));
    }
}
