package com.sequenceiq.it.cloudbreak;

import org.testng.Assert;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

public class OpenStackNetworkCreationTest extends AbstractCloudbreakIntegrationTest {
    @Test
    @Parameters({ "networkName", "subnetCIDR", "publicNetId" })
    public void testGcpTemplateCreation(@Optional("it-openstack-network") String networkName, @Optional("10.0.36.0/24") String subnetCIDR, String publicNetId)
            throws Exception {
        // GIVEN
        // WHEN
        // TODO: publicInAccount
        String id = getClient().postOpenStackNetwork(networkName, "OpenStack network for integration testing", subnetCIDR, publicNetId, false);
        // THEN
        Assert.assertNotNull(id);
        getItContext().putContextParam(CloudbreakITContextConstants.NETWORK_ID, id, true);
    }
}
