package com.sequenceiq.it.cloudbreak;

import java.io.IOException;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.log.Log;

public class SecurityRulesAction {

    private SecurityRulesAction() {
    }

    public static void getDefaultSecurityRules(IntegrationTestContext integrationTestContext, Entity entity) throws IOException {
        SecurityRulesEntity securityRulesEntity = (SecurityRulesEntity) entity;
        CloudbreakClient client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT, CloudbreakClient.class);

        Log.log(" get Security Rules to");
        securityRulesEntity.setResponse(client.getCloudbreakClient().utilV4Endpoint().getDefaultSecurityRules());
        Log.logJSON(" get Security Rules response: ", securityRulesEntity.getResponse());
    }
}
