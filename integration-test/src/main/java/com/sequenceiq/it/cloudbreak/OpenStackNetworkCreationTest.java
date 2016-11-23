package com.sequenceiq.it.cloudbreak;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.testng.Assert;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.model.NetworkRequest;

public class OpenStackNetworkCreationTest extends AbstractCloudbreakIntegrationTest {
    @Value("${integrationtest.openstack.publicNetId}")
    private String defaultPublicNetId;

    @Test
    @Parameters({ "networkName", "subnetCIDR", "publicNetId" })
    public void testOpenstackNetworkCreation(@Optional("it-openstack-network") String networkName, @Optional("10.0.36.0/24") String subnetCIDR,
            @Optional("") String publicNetId)
            throws Exception {
        // GIVEN
        publicNetId = getPublicNetId(publicNetId, defaultPublicNetId);
        // WHEN
        NetworkRequest networkRequest = new NetworkRequest();
        networkRequest.setDescription("OpenStack network for integration testing");
        networkRequest.setName(networkName);
        networkRequest.setSubnetCIDR(subnetCIDR);
        Map<String, Object> map = new HashMap<>();
        map.put("publicNetId", publicNetId);
        networkRequest.setParameters(map);
        networkRequest.setCloudPlatform("OPENSTACK");

        String id = getCloudbreakClient().networkEndpoint().postPrivate(networkRequest).getId().toString();
        // THEN
        Assert.assertNotNull(id);
        getItContext().putContextParam(CloudbreakITContextConstants.NETWORK_ID, id, true);
    }

    private String getPublicNetId(String publicNetId, String defaultPublicNetId) {
        if ("__empty__".equals(publicNetId)) {
            publicNetId = "";
        } else if (StringUtils.isEmpty(publicNetId)) {
            publicNetId = defaultPublicNetId;
        }
        return publicNetId;
    }
}
