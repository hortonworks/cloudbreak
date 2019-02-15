package com.sequenceiq.cloudbreak.converter.stack.instance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.cloudbreak.converter.AbstractEntityConverterTest;
import com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.InstanceMetaDataToInstanceMetaDataV4ResponseConverter;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;

public class InstanceMetaDataToInstanceMetaDataJsonConverterTest extends AbstractEntityConverterTest<InstanceMetaData> {

    private InstanceMetaDataToInstanceMetaDataV4ResponseConverter underTest;

    @Before
    public void setUp() {
        underTest = new InstanceMetaDataToInstanceMetaDataV4ResponseConverter();
    }

    @Test
    public void testConvert() {
        // GIVEN
        // WHEN
        InstanceMetaDataV4Response result = underTest.convert(getSource());
        // THEN
        assertEquals("test-1-1", result.getDiscoveryFQDN());
        assertTrue(result.getAmbariServer());
        assertAllFieldsNotNull(result, List.of("state"));
    }

    @Override
    public InstanceMetaData createSource() {
        return TestUtil.instanceMetaData(1L, 1L, InstanceStatus.REGISTERED, true,
                TestUtil.instanceGroup(1L, InstanceGroupType.GATEWAY, TestUtil.gcpTemplate(1L)));
    }
}
