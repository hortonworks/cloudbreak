package com.sequenceiq.cloudbreak.cloud.gcp.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Subnet;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.common.api.type.InstanceGroupType;

public class GcpStackUtilTest {

    private GcpStackUtil gcpStackUtil = new GcpStackUtil();

    @Test
    public void projectIdConverterWithNewNameRestrictions() {
        String projectId = gcpStackUtil.getProjectId(cloudCredential("siq-haas"));
        Assert.assertEquals("siq-haas", projectId);
        projectId = gcpStackUtil.getProjectId(cloudCredential("siq-haas123"));
        Assert.assertEquals("siq-haas123", projectId);
        projectId = gcpStackUtil.getProjectId(cloudCredential("Siq-haas123"));
        Assert.assertEquals("siq-haas123", projectId);
    }

    @Test
    public void projectIdConverterWithOldNameRestrictions() {
        String projectId = gcpStackUtil.getProjectId(cloudCredential("echo:siq-haas"));
        Assert.assertEquals("echo-siq-haas", projectId);
        projectId = gcpStackUtil.getProjectId(cloudCredential("echo:>siq>-haas"));
        Assert.assertEquals("echo--siq--haas", projectId);
        projectId = gcpStackUtil.getProjectId(cloudCredential("e?cho:siq-haas123"));
        Assert.assertEquals("e-cho-siq-haas123", projectId);
        projectId = gcpStackUtil.getProjectId(cloudCredential("echo:siq-hasfdsf12?as"));
        Assert.assertEquals("echo-siq-hasfdsf12-as", projectId);
    }

    private CloudCredential cloudCredential(String projectId) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("projectId", projectId);
        return new CloudCredential("crn", "test", parameters, false);
    }

    @Test
    public void testNewSubnetInExistingNetworkNoNetwork() {
        Network network = new Network(new Subnet(""));
        assertFalse(gcpStackUtil.isNewSubnetInExistingNetwork(network));
    }

    @Test
    public void testNewSubnetInExistingNetworkWithNetwork() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(GcpStackUtil.NETWORK_ID, "asdf");
        Network network = new Network(new Subnet(""), parameters);
        assertTrue(gcpStackUtil.isNewSubnetInExistingNetwork(network));
    }

    @Test
    public void testNewSubnetInExistingNetworkWithNetworkWithSubnet() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(GcpStackUtil.NETWORK_ID, "asdf");
        parameters.put(GcpStackUtil.SUBNET_ID, "asdf");
        Network network = new Network(new Subnet(""), parameters);
        assertFalse(gcpStackUtil.isNewSubnetInExistingNetwork(network));
    }

    @Test
    public void testGetGroupTypeTag() {
        assertEquals("gateway", gcpStackUtil.getGroupTypeTag(InstanceGroupType.GATEWAY));
        assertEquals("core", gcpStackUtil.getGroupTypeTag(InstanceGroupType.CORE));
        assertThrows(CloudbreakServiceException.class, () -> gcpStackUtil.getGroupTypeTag(null));
    }

}