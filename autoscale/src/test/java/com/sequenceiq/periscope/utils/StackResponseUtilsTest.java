package com.sequenceiq.periscope.utils;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;

public class StackResponseUtilsTest {

    private StackResponseUtils underTest = new StackResponseUtils();

    @Test
    public void getGetCloudInstanceIdsForHostGroup() {
        String hostGroup = "compute";
        String instanceType = "m5.10large";

        Map<String, String> instanceIdsForHostGroups = underTest
                .getCloudInstanceIdsForHostGroup(getMockStackV4Response(hostGroup, instanceType), hostGroup);

        assertEquals("Retrieved Instance Ids size should match", instanceIdsForHostGroups.size(), 3);
        assertEquals("Retrieved Instance Id should match",
                instanceIdsForHostGroups.get("test_fqdn1"), "test_instanceid1");
        assertEquals("Retrieved Instance Id should match",
                instanceIdsForHostGroups.get("test_fqdn2"), "test_instanceid2");
        assertEquals("Retrieved Instance Id should match",
                instanceIdsForHostGroups.get("test_fqdn3"), "test_instanceid3");
    }

    @Test
    public void testGetNodeCountForHostGroup() {
        String hostGroup = "compute";
        String instanceType = "m5.10large";

        Integer nodeCountForHostGroup = underTest
                .getNodeCountForHostGroup(getMockStackV4Response(hostGroup, instanceType), hostGroup);
        assertEquals("Retrieved HostGroup Instance Count should match.", Integer.valueOf(3), nodeCountForHostGroup);
    }

    private StackV4Response getMockStackV4Response(String hostGroup, String instanceType) {
        Map hostGroupInstanceType = new HashMap();
        hostGroupInstanceType.put("compute1", "m5.xlarge");
        hostGroupInstanceType.put("master1", "m5.xlarge");
        hostGroupInstanceType.put("worker1", "m5.xlarge");
        hostGroupInstanceType.put(hostGroup, instanceType);

        InstanceMetaDataV4Response metadata1 = new InstanceMetaDataV4Response();
        metadata1.setDiscoveryFQDN("test_fqdn1");
        metadata1.setInstanceId("test_instanceid1");

        InstanceMetaDataV4Response metadata2 = new InstanceMetaDataV4Response();
        metadata2.setDiscoveryFQDN("test_fqdn2");
        metadata2.setInstanceId("test_instanceid2");

        InstanceMetaDataV4Response metadata3 = new InstanceMetaDataV4Response();
        metadata3.setDiscoveryFQDN("test_fqdn3");
        metadata3.setInstanceId("test_instanceid3");

        return MockStackResponseGenerator.getMockStackV4Response("test-crn",
                hostGroupInstanceType,
                Set.of(metadata1, metadata2, metadata3));
    }
}
