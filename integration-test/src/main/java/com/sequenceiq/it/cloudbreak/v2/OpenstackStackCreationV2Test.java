package com.sequenceiq.it.cloudbreak.v2;

import java.util.Map;

import org.springframework.util.StringUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

import com.google.common.collect.Maps;
import com.sequenceiq.it.IntegrationTestContext;

public class OpenstackStackCreationV2Test extends AbstractStackCreationV2Test {
    @BeforeMethod(dependsOnGroups = "V2StackCreationInit")
    @Parameters({"subnetCidr", "floatingPool"})
    public void networkParams(String subnetCidr, @Optional("") String floatingPool) {
        IntegrationTestContext itContext = getItContext();
        var networkV2Request = createNetworkRequest(itContext, subnetCidr);
        floatingPool = StringUtils.hasText(floatingPool) ? floatingPool : itContext.getContextParam(CloudbreakV2Constants.OPENSTACK_FLOATING_POOL);
        Map<String, Object> networkParameters = Maps.newHashMap();
        // FIXME have to figure out how to decide which provider parameter is needed
        if (StringUtils.hasText(floatingPool)) {
            networkParameters.put("publicNetId", floatingPool);
        }
//        networkV2Request.setParameters(networkParameters);
    }
}