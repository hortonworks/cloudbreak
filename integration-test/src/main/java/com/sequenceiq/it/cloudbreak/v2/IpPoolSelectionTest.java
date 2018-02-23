package com.sequenceiq.it.cloudbreak.v2;

import java.util.Set;

import org.springframework.util.StringUtils;
import org.testng.Assert;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.model.IpPoolJson;
import com.sequenceiq.cloudbreak.api.model.PlatformIpPoolsResponse;
import com.sequenceiq.cloudbreak.api.model.PlatformResourceRequestJson;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.AbstractCloudbreakIntegrationTest;

public class IpPoolSelectionTest extends AbstractCloudbreakIntegrationTest {
    @Test
    @Parameters({"credentialName", "region", "availabilityZone", "poolName"})
    public void testIpPoolSelection(@Optional("") String credentialName, @Optional("") String region, @Optional("") String availabilityZone,
            String poolName) {
        // GIVEN
        IntegrationTestContext itContext = getItContext();
        credentialName = StringUtils.hasText(credentialName) ? credentialName : itContext.getContextParam(CloudbreakV2Constants.CREDENTIAL_NAME);
        region = StringUtils.hasText(region) ? region : itContext.getContextParam(CloudbreakV2Constants.REGION);
        availabilityZone = StringUtils.hasText(availabilityZone) ? availabilityZone : itContext.getContextParam(CloudbreakV2Constants.AVAILABILTYZONE);
        PlatformResourceRequestJson resourceRequestJson = new PlatformResourceRequestJson();
        resourceRequestJson.setCredentialName(credentialName);
        resourceRequestJson.setRegion(region);
        resourceRequestJson.setAvailabilityZone(availabilityZone);
        // WHEN
        PlatformIpPoolsResponse response = getCloudbreakClient().connectorV1Endpoint().getIpPoolsCredentialId(resourceRequestJson);
        // THEN
        Set<IpPoolJson> ipPools = response.getIppools().get(availabilityZone);
        Assert.assertNotNull(ipPools, "ippools cannot be null for " + region);
        java.util.Optional<IpPoolJson> selected = ipPools.stream().filter(rk -> rk.getName().equals(poolName)).findFirst();
        Assert.assertTrue(selected.isPresent(), "the ippool list doesn't contain [" + poolName + ']');
        getItContext().putContextParam(CloudbreakV2Constants.OPENSTACK_FLOATING_POOL, selected.get().getId());
    }
}
