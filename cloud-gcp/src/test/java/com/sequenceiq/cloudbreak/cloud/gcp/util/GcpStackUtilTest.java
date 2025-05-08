package com.sequenceiq.cloudbreak.cloud.gcp.util;

import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.SUBNET_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Subnet;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.common.api.type.InstanceGroupType;

class GcpStackUtilTest {

    private GcpStackUtil gcpStackUtil = new GcpStackUtil();

    @Test
    void projectIdConverterWithNewNameRestrictions() {
        String projectId = gcpStackUtil.getProjectId(cloudCredential("siq-haas"));
        assertEquals("siq-haas", projectId);
        projectId = gcpStackUtil.getProjectId(cloudCredential("siq-haas123"));
        assertEquals("siq-haas123", projectId);
        projectId = gcpStackUtil.getProjectId(cloudCredential("Siq-haas123"));
        assertEquals("siq-haas123", projectId);
    }

    @Test
    void projectIdConverterWithOldNameRestrictions() {
        String projectId = gcpStackUtil.getProjectId(cloudCredential("echo:siq-haas"));
        assertEquals("echo-siq-haas", projectId);
        projectId = gcpStackUtil.getProjectId(cloudCredential("echo:>siq>-haas"));
        assertEquals("echo--siq--haas", projectId);
        projectId = gcpStackUtil.getProjectId(cloudCredential("e?cho:siq-haas123"));
        assertEquals("e-cho-siq-haas123", projectId);
        projectId = gcpStackUtil.getProjectId(cloudCredential("echo:siq-hasfdsf12?as"));
        assertEquals("echo-siq-hasfdsf12-as", projectId);
    }

    private CloudCredential cloudCredential(String projectId) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("projectId", projectId);
        return new CloudCredential("crn", "test", parameters, "acc");
    }

    @Test
    void testNewSubnetInExistingNetworkNoNetwork() {
        Network network = new Network(new Subnet(""));
        assertFalse(gcpStackUtil.isNewSubnetInExistingNetwork(network));
    }

    @Test
    void testNewSubnetInExistingNetworkWithNetwork() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(GcpStackUtil.NETWORK_ID, "asdf");
        Network network = new Network(new Subnet(""), parameters);
        assertTrue(gcpStackUtil.isNewSubnetInExistingNetwork(network));
    }

    @Test
    void testNewSubnetInExistingNetworkWithNetworkWithSubnet() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(GcpStackUtil.NETWORK_ID, "asdf");
        parameters.put(SUBNET_ID, "asdf");
        Network network = new Network(new Subnet(""), parameters);
        assertFalse(gcpStackUtil.isNewSubnetInExistingNetwork(network));
    }

    @Test
    void testGetGroupTypeTag() {
        assertEquals("gateway", gcpStackUtil.getGroupTypeTag(InstanceGroupType.GATEWAY));
        assertEquals("core", gcpStackUtil.getGroupTypeTag(InstanceGroupType.CORE));
        assertThrows(CloudbreakServiceException.class, () -> gcpStackUtil.getGroupTypeTag(null));
    }

    @Test
    void testGetBucketNameOnlyOneSegment() {
        String result = gcpStackUtil.getBucketName("bucketname");

        assertEquals("bucketname", result);
    }

    @Test
    void testGetBucketNameOnlyOneSegmentWithProtocol() {
        String result = gcpStackUtil.getBucketName("gs://bucketname");

        assertEquals("bucketname", result);
    }

    @Test
    void testGetBucketNameMoreSegmentWithProtocol() {
        String result = gcpStackUtil.getBucketName("gs://bucketname/bucket/bucket");

        assertEquals("bucketname", result);
    }

    @Test
    void testGetGroupClusterTag() {
        CloudContext cloudContext = mock(CloudContext.class);
        when(cloudContext.getId()).thenReturn(12345L);

        Group group = mock(Group.class);
        when(group.getName()).thenReturn("TestGroup");

        String result = gcpStackUtil.getGroupClusterTag(cloudContext, group);

        assertEquals("testgroup12345", result);
    }
}
