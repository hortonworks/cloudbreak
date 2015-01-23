package com.sequenceiq.cloudbreak.converter;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.controller.json.InstanceMetaDataJson;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;

public class MetaDataConverterTest {

    private MetaDataConverter underTest;

    private InstanceMetaData metaData;

    @Before
    public void setUp() {
        underTest = new MetaDataConverter();
        metaData = createMetaData();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testConvertMetaDataJsonToEntity() {
        // GIVEN
        // WHEN
        underTest.convert(new InstanceMetaDataJson());

    }

    @Test
    public void testConvertMetaDataEntityToJson() {
        // GIVEN
        // WHEN
        InstanceMetaDataJson result = underTest.convert(metaData);
        // THEN
        assertEquals(result.getAmbariServer(), metaData.getAmbariServer());
        assertEquals(result.getDockerSubnet(), metaData.getDockerSubnet());
        assertEquals(result.getInstanceId(), metaData.getInstanceId());
        assertEquals(result.getPrivateIp(), metaData.getPrivateIp());
        assertEquals(result.getPublicIp(), metaData.getPublicIp());

    }

    private InstanceMetaData createMetaData() {
        InstanceMetaData metaData = new InstanceMetaData();
        metaData.setAmbariServer(true);
        metaData.setDockerSubnet("dummyDockerSubnet");
        metaData.setId(1L);
        metaData.setInstanceId("dummyInstanceId");
        metaData.setPrivateIp("dummyPrivateIp");
        metaData.setPublicIp("dummyPublicIp");
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName("master");
        metaData.setInstanceGroup(instanceGroup);
        return metaData;
    }
}
