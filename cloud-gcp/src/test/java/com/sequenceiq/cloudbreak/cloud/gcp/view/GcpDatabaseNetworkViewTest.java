package com.sequenceiq.cloudbreak.cloud.gcp.view;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Subnet;

public class GcpDatabaseNetworkViewTest {

    @Test
    public void testGcpDatabaseNetworkView() {
        Map<String, Object> map = new HashMap<>();
        map.put("subnetId", "subnet-1");
        map.put("availabilityZone", "zone");
        map.put("sharedProjectId", "id");
        Network network = new Network(new Subnet("10.0.0.0"), map);

        GcpDatabaseNetworkView gcpDatabaseNetworkView = new GcpDatabaseNetworkView(network);

        Assert.assertEquals("zone", gcpDatabaseNetworkView.getAvailabilityZone());
        Assert.assertEquals("id", gcpDatabaseNetworkView.getSharedProjectId());
        Assert.assertEquals("subnet-1", gcpDatabaseNetworkView.getSubnetId());
    }
}