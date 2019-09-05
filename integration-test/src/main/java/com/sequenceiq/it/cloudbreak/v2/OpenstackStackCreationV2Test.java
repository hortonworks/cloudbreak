package com.sequenceiq.it.cloudbreak.v2;

import java.util.Map;

import org.springframework.util.StringUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.api.model.v2.NetworkV2Request;
import com.sequenceiq.it.IntegrationTestContext;

public class OpenstackStackCreationV2Test extends AbstractStackCreationV3Test {
    @BeforeMethod(dependsOnGroups = "V2StackCreationInit")
    @Parameters({"subnetCidr", "floatingPool"})
    public void networkParams(String subnetCidr, @Optional("") String floatingPool) {
        IntegrationTestContext itContext = getItContext();
        NetworkV2Request networkV2Request = createNetworkRequest(itContext, subnetCidr);
        floatingPool = StringUtils.hasText(floatingPool) ? floatingPool : itContext.getContextParam(CloudbreakV2Constants.OPENSTACK_FLOATING_POOL);
        Map<String, Object> networkParameters = Maps.newHashMap();
        if (StringUtils.hasText(floatingPool)) {
            networkParameters.put("publicNetId", floatingPool);
        }
        networkV2Request.setParameters(networkParameters);
    }
}
