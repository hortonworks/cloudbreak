package com.sequenceiq.it.cloudbreak;

import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.model.NetworkJson;

public class OpenStackNetworkCreationTest extends AbstractCloudbreakIntegrationTest {
    @Test
    @Parameters({ "networkName", "subnetCIDR", "publicNetId" })
    public void testGcpTemplateCreation(@Optional("it-openstack-network") String networkName, @Optional("10.0.36.0/24") String subnetCIDR, String publicNetId)
            throws Exception {
        // GIVEN
        // WHEN
        // TODO: publicInAccount
        NetworkJson networkJson = new NetworkJson();
        networkJson.setDescription("OpenStack network for integration testing");
        networkJson.setName(networkName);
        networkJson.setSubnetCIDR(subnetCIDR);
        Map<String, Object> map = new HashMap<>();
        map.put("publicNetId", publicNetId);
        networkJson.setParameters(map);
        networkJson.setCloudPlatform("OPENSTACK");

        String id = getNetworkEndpoint().postPrivate(networkJson).getId().toString();
        // THEN
        Assert.assertNotNull(id);
        getItContext().putContextParam(CloudbreakITContextConstants.NETWORK_ID, id, true);
    }
}
